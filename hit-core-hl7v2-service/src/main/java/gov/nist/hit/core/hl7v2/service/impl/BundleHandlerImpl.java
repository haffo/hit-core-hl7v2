package gov.nist.hit.core.hl7v2.service.impl;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nist.hit.core.domain.CFTestPlan;
import gov.nist.hit.core.domain.CFTestStep;
import gov.nist.hit.core.domain.ConformanceProfile;
import gov.nist.hit.core.domain.Constraints;
import gov.nist.hit.core.domain.GVTSaveInstance;
import gov.nist.hit.core.domain.IntegrationProfile;
import gov.nist.hit.core.domain.Message;
import gov.nist.hit.core.domain.TestScope;
import gov.nist.hit.core.domain.TestingStage;
import gov.nist.hit.core.domain.VocabularyLibrary;
import gov.nist.hit.core.hl7v2.domain.HL7V2TestContext;
import gov.nist.hit.core.service.BundleHandler;
import gov.nist.hit.core.service.ResourceLoader;
import gov.nist.hit.core.service.exception.ProfileParserException;

@Service
public class BundleHandlerImpl implements BundleHandler {

	@Autowired
	private ResourceLoader resourceLoader;

	@Override
	public String unzip(byte[] bytes, String path) throws Exception {
		File tmpDir = new File(path);
		tmpDir.mkdirs();
		if (tmpDir.isDirectory()) {
			// Extract ZIP
			ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes));
			ZipEntry ze;
			while ((ze = zip.getNextEntry()) != null) {
				String filePath = tmpDir.getAbsolutePath() + File.separator + ze.getName();
				if (!ze.isDirectory()) {
					BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
					byte[] bytesIn = new byte[1024];
					int read = 0;
					while ((read = zip.read(bytesIn)) != -1) {
						bos.write(bytesIn, 0, read);
					}
					bos.close();
				} else {
					File dir = new File(filePath);
					dir.mkdir();
				}
				zip.closeEntry();
			}
			zip.close();
			return tmpDir.getAbsolutePath();

		} else {
			throw new Exception("Could not create TMP directory at " + tmpDir.getAbsolutePath());
		}
	}

	@Override
	public GVTSaveInstance createGVTSaveInstance(String dir, String domain) throws IOException, ProfileParserException {
		GVTSaveInstance save = new GVTSaveInstance();
		File testCasesFile = new File(dir + "/TestCases.json");
		if (!testCasesFile.exists()) {
			throw new IllegalArgumentException("No TestCases.json found");
		}

		CFTestPlan gtcg = new CFTestPlan();
		gtcg.setPersistentId(new Random().nextLong());
		String descriptorContent = FileUtils.readFileToString(testCasesFile);
		ObjectMapper mapper = new ObjectMapper();
		JsonNode testCasesObj = mapper.readTree(descriptorContent);
		gtcg.setDomain(domain);
		gtcg.setName(testCasesObj.get("name").asText());
		gtcg.setDescription(testCasesObj.get("description").asText());
		gtcg.setPreloaded(false);
		gtcg.setScope(testCasesObj.get("scope") != null && testCasesObj.get("scope").asText() != null
				? TestScope.valueOf(testCasesObj.get("scope").asText()) : TestScope.USER);
		save.tcg = gtcg;

		// Profile
		String profileName = testCasesObj.findValue("profile").asText();
		File profileFile = new File(dir + "/" + profileName);
		if (!profileFile.exists()) {
			throw new IllegalArgumentException("Profile " + profileName + " not found");
		}
		IntegrationProfile p = resourceLoader.integrationProfile(FileUtils.readFileToString(profileFile), domain);
		p.setPreloaded(false);
		save.ip = p;

		// Constraints
		String constraintName = testCasesObj.findValue("constraints").asText();
		File constraintsFile = new File(dir + "/" + constraintName);
		if (!constraintsFile.exists()) {
			throw new IllegalArgumentException("Constraints " + constraintName + " not found");
		}
		Constraints c = resourceLoader.constraint(FileUtils.readFileToString(constraintsFile), domain);
		c.setPreloaded(false);
		save.ct = c;

		// VS
		String vocabName = testCasesObj.findValue("vs").asText();
		File vsFile = new File(dir + "/" + vocabName);
		if (!vsFile.exists()) {
			throw new IllegalArgumentException("VocabularyLibrary " + vocabName + " not found");
		}
		VocabularyLibrary v = resourceLoader.vocabLibrary(FileUtils.readFileToString(vsFile), domain);
		v.setPreloaded(false);

		save.vs = v;

		Iterator<JsonNode> testCasesIter = testCasesObj.findValue("testCases").elements();
		int i = 1;
		while (testCasesIter.hasNext()) {
			JsonNode tcO = testCasesIter.next();
			CFTestStep cfti = new CFTestStep();
			cfti.setPreloaded(false);
			cfti.setScope(tcO.get("scope") != null && tcO.get("scope").asText() != null
					? TestScope.valueOf(tcO.get("scope").asText()) : TestScope.USER);
			cfti.setPosition(i++);
			String messageId = tcO.findValue("messageId").asText();
			String name = tcO.findValue("name").asText();
			String description = tcO.findValue("description").asText();
			Long id = new Random().nextLong();
			cfti.setDomain(domain);
			// ---
			ConformanceProfile conformanceProfile = new ConformanceProfile();
			conformanceProfile.setJson(resourceLoader.jsonConformanceProfile(p.getXml(), messageId, c.getXml(), null));

			conformanceProfile.setIntegrationProfile(p);
			conformanceProfile.setSourceId(messageId);
			conformanceProfile.setDomain(domain);
			// ---
			HL7V2TestContext testContext = new HL7V2TestContext();
			testContext.setVocabularyLibrary(v);
			testContext.setConstraints(c);
			testContext.setConformanceProfile(conformanceProfile);
			testContext.setDqa(false);
			testContext.setStage(TestingStage.CF);
			testContext.setDomain(domain);

			// ---
			cfti.setName(name);
			cfti.setDescription(description);
			// cfti.setRoot(true);
			cfti.setTestContext(testContext);
			cfti.setPersistentId(id);
			// ---
			gtcg.getTestSteps().add(cfti);
		}

		return save;
	}

	@Override
	@Transactional(value = "transactionManager")
	public GVTSaveInstance createGVTSaveInstance(String dir, CFTestPlan tp) throws IOException, ProfileParserException {
		GVTSaveInstance save = new GVTSaveInstance();
		File testCasesFile = new File(dir + "/TestCases.json");
		if (!testCasesFile.exists()) {
			throw new IllegalArgumentException("No TestCases.json found");
		}

		save.tcg = tp;

		String descriptorContent = FileUtils.readFileToString(testCasesFile);
		ObjectMapper mapper = new ObjectMapper();
		JsonNode testCasesObj = mapper.readTree(descriptorContent);

		// Profile
		String profileName = testCasesObj.findValue("profile").asText();
		File profileFile = new File(dir + "/" + profileName);
		if (!profileFile.exists()) {
			throw new IllegalArgumentException("Profile " + profileName + " not found");
		}
		IntegrationProfile p = resourceLoader.integrationProfile(FileUtils.readFileToString(profileFile),
				tp.getDomain());
		p.setDomain(tp.getDomain());
		p.setPreloaded(false);
		save.ip = p;

		// Constraints
		String constraintName = testCasesObj.findValue("constraints").asText();
		File constraintsFile = new File(dir + "/" + constraintName);
		if (!constraintsFile.exists()) {
			throw new IllegalArgumentException("Constraints " + constraintName + " not found");
		}
		Constraints c = resourceLoader.constraint(FileUtils.readFileToString(constraintsFile), tp.getDomain());
		c.setPreloaded(false);
		save.ct = c;

		// VS
		String vocabName = testCasesObj.findValue("vs").asText();
		File vsFile = new File(dir + "/" + vocabName);
		if (!vsFile.exists()) {
			throw new IllegalArgumentException("VocabularyLibrary " + vocabName + " not found");
		}
		VocabularyLibrary v = resourceLoader.vocabLibrary(FileUtils.readFileToString(vsFile), tp.getDomain());
		v.setPreloaded(false);

		save.vs = v;

		Iterator<JsonNode> testCasesIter = testCasesObj.findValue("testCases").elements();
		int size = tp.getTestSteps().size();
		while (testCasesIter.hasNext()) {
			JsonNode tcO = testCasesIter.next();
			CFTestStep cfti = new CFTestStep();
			cfti.setPreloaded(false);
			cfti.setScope(tcO.get("scope") != null && tcO.get("scope").asText() != null
					? TestScope.valueOf(tcO.get("scope").asText()) : TestScope.USER);
			String messageId = tcO.findValue("messageId").asText();
			String name = tcO.findValue("name").asText();
			String description = tcO.findValue("description").asText();
			Long id = new Random().nextLong();
			cfti.setDomain(tp.getDomain());

			// ---
			ConformanceProfile conformanceProfile = new ConformanceProfile();
			conformanceProfile.setJson(resourceLoader.jsonConformanceProfile(p.getXml(), messageId, c.getXml(), null));

			conformanceProfile.setIntegrationProfile(p);
			conformanceProfile.setSourceId(messageId);
			conformanceProfile.setDomain(tp.getDomain());
			// ---
			HL7V2TestContext testContext = new HL7V2TestContext();
			testContext.setVocabularyLibrary(v);
			testContext.setConstraints(c);
			testContext.setConformanceProfile(conformanceProfile);
			testContext.setDqa(false);
			testContext.setStage(TestingStage.CF);
			testContext.setDomain(tp.getDomain());

			Message message = testContext.getMessage();
			if (tcO.findValue("exampleMessage") != null) {
				if (message == null) {
					message = new Message();
					message.setName(name);
					message.setDomain(tp.getDomain());
					message.setDescription(description);
					testContext.setMessage(message);
				}
				message.setContent(tcO.findValue("exampleMessage").asText());
			}
			// ---
			cfti.setName(name);
			cfti.setDescription(description);
			// cfti.setRoot(true);
			cfti.setTestContext(testContext);
			cfti.setPersistentId(id);
			cfti.setPosition(size + tcO.findValue("position").asInt());
			// ---
			tp.getTestSteps().add(cfti);
		}

		return save;
	}

	@Override
	public String getProfileContentFromZipDirectory(String dir) throws IOException {
		return FileUtils.readFileToString(findFileDirectory(dir, "Profile.xml"));
	}

	@Override
	public String getValueSetContentFromZipDirectory(String dir) throws IOException {
		return FileUtils.readFileToString(findFileDirectory(dir, "ValueSets.xml"));
	}

	@Override
	public String getConstraintContentFromZipDirectory(String dir) throws IOException {
		return FileUtils.readFileToString(findFileDirectory(dir, "Constraints.xml"));
	}

	// finds file in dir and sub-dir
	private File findFileDirectory(String dir, String fileName) {
		Collection<File> files = FileUtils.listFiles(new File(dir), null, true);
		for (Iterator<File> iterator = files.iterator(); iterator.hasNext();) {
			File file = iterator.next();
			if (file.getName().equals(fileName)) {
				return file;
			}
		}
		return null;
	}

}
