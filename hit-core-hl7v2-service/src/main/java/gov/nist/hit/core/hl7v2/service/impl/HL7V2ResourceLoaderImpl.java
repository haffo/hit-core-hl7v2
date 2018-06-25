package gov.nist.hit.core.hl7v2.service.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;

import gov.nist.hit.core.domain.ConformanceProfile;
import gov.nist.hit.core.domain.Constraints;
import gov.nist.hit.core.domain.IntegrationProfile;
import gov.nist.hit.core.domain.ProfileModel;
import gov.nist.hit.core.domain.ResourceType;
import gov.nist.hit.core.domain.ResourceUploadAction;
import gov.nist.hit.core.domain.ResourceUploadResult;
import gov.nist.hit.core.domain.ResourceUploadStatus;
import gov.nist.hit.core.domain.TestCaseDocument;
import gov.nist.hit.core.domain.TestContext;
import gov.nist.hit.core.domain.TestScope;
import gov.nist.hit.core.domain.TestingStage;
import gov.nist.hit.core.domain.UploadedProfileModel;
import gov.nist.hit.core.domain.VocabularyLibrary;
import gov.nist.hit.core.hl7v2.domain.HL7V2TestContext;
import gov.nist.hit.core.hl7v2.domain.HLV2TestCaseDocument;
import gov.nist.hit.core.hl7v2.repo.HL7V2TestContextRepository;
import gov.nist.hit.core.hl7v2.service.HL7V2ProfileParser;
import gov.nist.hit.core.hl7v2.service.HL7V2ResourceLoader;
import gov.nist.hit.core.hl7v2.service.PackagingHandler;
import gov.nist.hit.core.service.ValueSetLibrarySerializer;
import gov.nist.hit.core.service.exception.ProfileParserException;
import gov.nist.hit.core.service.impl.ValueSetLibrarySerializerImpl;
import gov.nist.hit.core.service.util.FileUtil;

public class HL7V2ResourceLoaderImpl extends HL7V2ResourceLoader {

	static final Logger logger = LoggerFactory.getLogger(HL7V2ResourceLoaderImpl.class);
	static final String FORMAT = "hl7v2";

	public static final String TMP_DIR = new File(System.getProperty("java.io.tmpdir")).getAbsolutePath() + "/cf";

	@Autowired
	HL7V2TestContextRepository testContextRepository;

	@Autowired
	private PackagingHandler packagingHandler;

	HL7V2ProfileParser profileParser = new HL7V2ProfileParserImpl();
	ValueSetLibrarySerializer valueSetLibrarySerializer = new ValueSetLibrarySerializerImpl();

	@Autowired
	@PersistenceContext(unitName = "base-tool")
	protected EntityManager entityManager;

	@Override
	protected VocabularyLibrary getVocabularyLibrary(String id) throws IOException {
		return this.vocabularyLibraryRepository.findOneBySourceId(id);
	}

	@Override
	protected Constraints getConstraints(String id) throws IOException {
		return this.constraintsRepository.findOneBySourceId(id);
	}

	@Override
	protected IntegrationProfile getIntegrationProfile(String messageId) throws IOException {
		String sourceId = this.getProfilesMap().get(messageId);
		if (sourceId != null) {
			return this.integrationProfileRepository.findBySourceId(sourceId);
		}
		return null;
	}

	// ----- Global -> ValueSet, Constraints, IntegrationProfile

	@Override
	public List<ResourceUploadStatus> addOrReplaceValueSet(String rootPath, String domain, TestScope scope,
			String authorUsername, boolean preloaded) {
		System.out.println("AddOrReplace VS");

		List<Resource> resources;
		try {
			resources = this.getApiResources("*.xml", rootPath);
			if (resources == null || resources.isEmpty()) {
				ResourceUploadStatus result = new ResourceUploadStatus();
				result.setType(ResourceType.VALUESETLIBRARY);
				result.setStatus(ResourceUploadResult.FAILURE);
				result.setMessage("No resource found");
				return Arrays.asList(result);
			}
		} catch (IOException e1) {
			ResourceUploadStatus result = new ResourceUploadStatus();
			result.setType(ResourceType.VALUESETLIBRARY);
			result.setStatus(ResourceUploadResult.FAILURE);
			result.setMessage("Error while parsing resources");
			return Arrays.asList(result);
		}

		List<ResourceUploadStatus> results = new ArrayList<ResourceUploadStatus>();

		for (Resource resource : resources) {
			ResourceUploadStatus result = new ResourceUploadStatus();
			result.setType(ResourceType.VALUESETLIBRARY);
			String content = FileUtil.getContent(resource);
			try {
				VocabularyLibrary vocabLibrary = vocabLibrary(content, domain, scope, authorUsername, preloaded);
				result.setId(vocabLibrary.getSourceId());
				vocabLibrary.setDomain(domain);
				VocabularyLibrary exist = this.getVocabularyLibrary(vocabLibrary.getSourceId());
				if (exist != null) {
					System.out.println("Replace");
					result.setAction(ResourceUploadAction.UPDATE);
					vocabLibrary.setId(exist.getId());
					vocabLibrary.setSourceId(exist.getSourceId());
				} else {
					result.setAction(ResourceUploadAction.ADD);
				}

				this.vocabularyLibraryRepository.save(vocabLibrary);
				result.setStatus(ResourceUploadResult.SUCCESS);

			} catch (Exception e) {
				result.setStatus(ResourceUploadResult.FAILURE);
				result.setMessage(e.getMessage());
			}
			results.add(result);
		}
		return results;
	}

	@Override
	public List<ResourceUploadStatus> addOrReplaceConstraints(String rootPath, String domain, TestScope scope,
			String authorUsername, boolean preloaded) {
		System.out.println("AddOrReplace Constraints");

		List<Resource> resources;
		try {
			resources = this.getApiResources("*.xml", rootPath);
			if (resources == null || resources.isEmpty()) {
				ResourceUploadStatus result = new ResourceUploadStatus();
				result.setType(ResourceType.CONSTRAINTS);
				result.setStatus(ResourceUploadResult.FAILURE);
				result.setMessage("No resource found");
				return Arrays.asList(result);
			}
		} catch (IOException e1) {
			ResourceUploadStatus result = new ResourceUploadStatus();
			result.setType(ResourceType.CONSTRAINTS);
			result.setStatus(ResourceUploadResult.FAILURE);
			result.setMessage("Error while parsing resources");
			return Arrays.asList(result);
		}

		List<ResourceUploadStatus> results = new ArrayList<ResourceUploadStatus>();

		for (Resource resource : resources) {
			ResourceUploadStatus result = new ResourceUploadStatus();
			result.setType(ResourceType.CONSTRAINTS);
			String content = FileUtil.getContent(resource);
			try {
				Constraints constraint = constraint(content, domain, scope, authorUsername, preloaded);
				result.setId(constraint.getSourceId());
				Constraints exist = this.getConstraints(constraint.getSourceId());
				if (exist != null) {
					System.out.println("Replace");
					result.setAction(ResourceUploadAction.UPDATE);
					constraint.setId(exist.getId());
					constraint.setSourceId(exist.getSourceId());
				} else {
					result.setAction(ResourceUploadAction.ADD);
					System.out.println("Add");
				}

				this.constraintsRepository.save(constraint);
				result.setStatus(ResourceUploadResult.SUCCESS);

			} catch (Exception e) {
				result.setStatus(ResourceUploadResult.FAILURE);
				result.setMessage(e.getMessage());
			}
			results.add(result);
		}
		return results;
	}

	@Override
	public List<ResourceUploadStatus> addOrReplaceIntegrationProfile(String rootPath, String domain, TestScope scope,
			String authorUsername, boolean preloaded) {
		System.out.println("AddOrReplace integration profile");

		List<Resource> resources;
		try {
			resources = this.getApiResources("*.xml", rootPath);
			if (resources == null || resources.isEmpty()) {
				ResourceUploadStatus result = new ResourceUploadStatus();
				result.setType(ResourceType.PROFILE);
				result.setStatus(ResourceUploadResult.FAILURE);
				result.setMessage("No resource found");
				return Arrays.asList(result);
			}
		} catch (IOException e1) {
			ResourceUploadStatus result = new ResourceUploadStatus();
			result.setType(ResourceType.PROFILE);
			result.setStatus(ResourceUploadResult.FAILURE);
			result.setMessage("Error while parsing resources");
			return Arrays.asList(result);
		}

		List<ResourceUploadStatus> results = new ArrayList<ResourceUploadStatus>();
		for (Resource resource : resources) {
			ResourceUploadStatus result = new ResourceUploadStatus();
			result.setType(ResourceType.PROFILE);
			String content = FileUtil.getContent(resource);
			try {
				IntegrationProfile integrationP = integrationProfile(content, domain, scope, authorUsername, preloaded);
				result.setId(integrationP.getSourceId());
				IntegrationProfile exist = this.integrationProfileRepository.findBySourceId(integrationP.getSourceId());
				if (exist != null) {
					System.out.println("Replace");
					result.setAction(ResourceUploadAction.UPDATE);
					integrationP.setId(exist.getId());
					integrationP.setSourceId(exist.getSourceId());
				} else {
					result.setAction(ResourceUploadAction.ADD);
					System.out.println("Add");
				}
				this.integrationProfileRepository.save(integrationP);
				result.setStatus(ResourceUploadResult.SUCCESS);
			} catch (Exception e) {
				result.setStatus(ResourceUploadResult.FAILURE);
				result.setMessage(e.getMessage());
			}
			results.add(result);
		}
		return results;

	}

	@Override
	public TestCaseDocument generateTestCaseDocument(TestContext c) throws IOException {
		HLV2TestCaseDocument doc = new HLV2TestCaseDocument();
		if (c != null) {
			HL7V2TestContext context = testContextRepository.findOne(c.getId());
			doc.setExMsgPresent(context.getMessage() != null && context.getMessage().getContent() != null);
			doc.setXmlConfProfilePresent(
					context.getConformanceProfile() != null && context.getConformanceProfile().getJson() != null);
			doc.setXmlValueSetLibraryPresent(
					context.getVocabularyLibrary() != null && context.getVocabularyLibrary().getJson() != null);
			doc.setXmlConstraintsPresent((context.getAddditionalConstraints() != null
					&& context.getAddditionalConstraints().getXml() != null)
					|| (context.getConstraints() != null && context.getConstraints().getXml() != null));
		}
		return doc;
	}

	private Constraints createAdditionalConstraint(String content, String domain, TestScope scope,
			String authorUsername, boolean preloaded) throws IOException {
		Constraints constraint = additionalConstraints(content, domain, scope, authorUsername, preloaded);
		if (constraint != null)
			constraint.setSourceId(UUID.randomUUID().toString());
		return constraint;
	}

	@Override
	public TestContext testContext(String path, JsonNode formatObj, TestingStage stage, String rootPath, String domain,
			TestScope scope, String authorUsername, boolean preloaded) throws Exception {
		// for backward compatibility
		formatObj = formatObj.findValue(FORMAT) != null ? formatObj.findValue(FORMAT) : formatObj;

		JsonNode messageId = formatObj.findValue("messageId");
		JsonNode constraintId = formatObj.findValue("constraintId");
		JsonNode valueSetLibraryId = formatObj.findValue("valueSetLibraryId");
		JsonNode dqa = formatObj.findValue("dqa");
		HL7V2TestContext testContext = new HL7V2TestContext();
		testContext.setFormat(FORMAT);
		testContext.setStage(stage);
		testContext.setDomain(domain);
		testContext.setScope(scope);
		testContext.setAuthorUsername(authorUsername);
		testContext.setPreloaded(preloaded);

		if (valueSetLibraryId != null && !"".equals(valueSetLibraryId.textValue())) {
			testContext.setVocabularyLibrary((getVocabularyLibrary(valueSetLibraryId.textValue())));
		} else {
			try {
				Resource resource = this.getResource(path + VALUESETS_FILE_PATTERN, rootPath);
				if (resource != null) {
					String content = IOUtils.toString(resource.getInputStream());
					content = packagingHandler.changeVsId(content);
					VocabularyLibrary vocabLibrary = vocabLibrary(content, domain, scope, authorUsername, preloaded);
					this.vocabularyLibraryRepository.save(vocabLibrary);
					testContext.setVocabularyLibrary(vocabLibrary);
				}

			} catch (Exception e) {
				throw new RuntimeException("Failed to parse the value sets at " + path);
			}
		}

		if (constraintId != null && !"".equals(constraintId.textValue())) {
			Constraints co = getConstraints(constraintId.textValue());
			co.setDomain(domain);
			co.setScope(scope);
			co.setAuthorUsername(authorUsername);
			co.setPreloaded(preloaded);
			testContext.setConstraints(co);
		}

		try {
			Resource resource = this.getResource(path + CONSTRAINTS_FILE_PATTERN, rootPath);
			if (resource != null) {
				String content = IOUtils.toString(resource.getInputStream());
				content = packagingHandler.changeConstraintId(content);
				Constraints co = createAdditionalConstraint(content, domain, scope, authorUsername, preloaded);
				testContext.setAddditionalConstraints(co);
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to parse the constraints at " + path);
		}

		testContext.setMessage(message(FileUtil.getContent(getResource(path + "Message.txt", rootPath)), domain, scope,
				authorUsername, preloaded));
		if (testContext.getMessage() == null) {
			testContext.setMessage(message(FileUtil.getContent(getResource(path + "Message.text", rootPath)), domain,
					scope, authorUsername, preloaded));
		}

		if (dqa != null && !"".equals(dqa.textValue())) {
			testContext.setDqa(dqa.booleanValue());
		}

		if (messageId != null) {
			try {
				ConformanceProfile conformanceProfile = new ConformanceProfile();
				IntegrationProfile integrationProfile = getIntegrationProfile(messageId.textValue());
				conformanceProfile.setJson(jsonConformanceProfile(integrationProfile.getXml(), messageId.textValue(),
						testContext.getConstraints() != null ? testContext.getConstraints().getXml() : null,
						testContext.getAddditionalConstraints() != null
								? testContext.getAddditionalConstraints().getXml() : null));
				conformanceProfile
						.setXml(getConformanceProfileContent(integrationProfile.getXml(), messageId.textValue()));
				conformanceProfile.setSourceId(messageId.textValue());
				// conformanceProfile.setIntegrationProfileId(integrationProfile.getId());
				// conformanceProfile.setSourceId(messageId.textValue());
				conformanceProfile.setDomain(domain);
				conformanceProfile.setScope(scope);
				conformanceProfile.setAuthorUsername(authorUsername);
				conformanceProfile.setPreloaded(preloaded);
				testContext.setConformanceProfile(conformanceProfile);
			} catch (ProfileParserException e) {
				throw new RuntimeException("Failed to parse integrationProfile at " + path);
			}
		} else {
			try {
				Resource resource = this.getResource(path + PROFILE_FILE_PATTERN, rootPath);
				String content = IOUtils.toString(resource.getInputStream());
				List<UploadedProfileModel> list = packagingHandler.getUploadedProfiles(content);
				content = packagingHandler.removeUnusedAndDuplicateMessages(content,
						new HashSet<UploadedProfileModel>(Arrays.asList(list.get(0))));
				content = packagingHandler.changeProfileId(content);
				String messageID = getMessageId(content);
				ConformanceProfile conformanceProfile = new ConformanceProfile();
				conformanceProfile.setJson(
						jsonConformanceProfile(content, messageID, null, testContext.getAddditionalConstraints() != null
								? testContext.getAddditionalConstraints().getXml() : null));
				conformanceProfile.setXml(getConformanceProfileContent(content, messageID));
				conformanceProfile.setDomain(domain);
				conformanceProfile.setScope(scope);
				conformanceProfile.setAuthorUsername(authorUsername);
				conformanceProfile.setPreloaded(preloaded);
				conformanceProfile.setSourceId(messageID);
				testContext.setConformanceProfile(conformanceProfile);

			} catch (Exception e) {
				throw new RuntimeException("Failed to parse integrationProfile at " + path);
			}
		}
		return testContext;
	}

	// private IntegrationProfile createIntegrationProfile(String content,
	// String domain, TestScope scope,
	// String authorUsername, boolean preloaded) {
	// Document doc = this.stringToDom(content);
	// IntegrationProfile integrationProfile = new IntegrationProfile();
	// Element profileElement = (Element)
	// doc.getElementsByTagName("ConformanceProfile").item(0);
	// integrationProfile.setSourceId(profileElement.getAttribute("ID"));
	// Element metaDataElement = (Element)
	// profileElement.getElementsByTagName("MetaData").item(0);
	// integrationProfile.setName(metaDataElement.getAttribute("Name"));
	// integrationProfile.setXml(content);
	// Element conformanceProfilElementRoot = (Element)
	// profileElement.getElementsByTagName("Messages").item(0);
	// NodeList messages =
	// conformanceProfilElementRoot.getElementsByTagName("Message");
	//
	// // Message IDs
	// List<String> ids = new ArrayList<String>();
	//
	// for (int j = 0; j < messages.getLength(); j++) {
	// Element elmCode = (Element) messages.item(j);
	// String id = elmCode.getAttribute("ID");
	// ids.add(id);
	// }
	// integrationProfile.setDomain(domain);
	// integrationProfile.setScope(scope);
	// integrationProfile.setAuthorUsername(authorUsername);
	// integrationProfile.setPreloaded(preloaded);
	// return integrationProfile;
	// }

	private String getMessageId(String content) {
		Document doc = this.stringToDom(content);
		IntegrationProfile integrationProfile = new IntegrationProfile();
		Element profileElement = (Element) doc.getElementsByTagName("ConformanceProfile").item(0);
		integrationProfile.setSourceId(profileElement.getAttribute("ID"));
		Element metaDataElement = (Element) profileElement.getElementsByTagName("MetaData").item(0);
		integrationProfile.setName(metaDataElement.getAttribute("Name"));
		integrationProfile.setXml(content);
		Element conformanceProfilElementRoot = (Element) profileElement.getElementsByTagName("Messages").item(0);
		NodeList messages = conformanceProfilElementRoot.getElementsByTagName("Message");
		// Message IDs
		Element elmCode = (Element) messages.item(0);
		return elmCode.getAttribute("ID");
	}

	@Override
	public ProfileModel parseProfile(String integrationProfileXml, String conformanceProfileId, String constraintsXml,
			String additionalConstraintsXml) throws ProfileParserException {
		return profileParser.parse(integrationProfileXml, conformanceProfileId, constraintsXml,
				additionalConstraintsXml);
	}

	@Override
	public VocabularyLibrary vocabLibrary(String content, String domain, TestScope scope, String authorUsername,
			boolean preloaded) throws JsonGenerationException, JsonMappingException, IOException {
		Document doc = this.stringToDom(content);
		VocabularyLibrary vocabLibrary = new VocabularyLibrary();
		Element valueSetLibraryeElement = (Element) doc.getElementsByTagName("ValueSetLibrary").item(0);
		vocabLibrary.setSourceId(valueSetLibraryeElement.getAttribute("ValueSetLibraryIdentifier"));
		vocabLibrary.setName(valueSetLibraryeElement.getAttribute("Name"));
		vocabLibrary.setDescription(valueSetLibraryeElement.getAttribute("Description"));
		vocabLibrary.setXml(content);
		vocabLibrary.setDomain(domain);
		vocabLibrary.setScope(scope);
		vocabLibrary.setAuthorUsername(authorUsername);
		vocabLibrary.setPreloaded(preloaded);
		vocabLibrary.setJson(obm.writeValueAsString(valueSetLibrarySerializer.toObject(content)));
		return vocabLibrary;
	}

}
