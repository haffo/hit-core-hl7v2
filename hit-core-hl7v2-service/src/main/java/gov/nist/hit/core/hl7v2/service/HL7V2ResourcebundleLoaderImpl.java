/**
 * This software was developed at the National Institute of Standards and Technology by employees of
 * the Federal Government in the course of their official duties. Pursuant to title 17 Section 105
 * of the United States Code this software is not subject to copyright protection and is in the
 * public domain. This is an experimental system. NIST assumes no responsibility whatsoever for its
 * use by other parties, and makes no guarantees, expressed or implied, about its quality,
 * reliability, or any other characteristic. We would appreciate acknowledgement if the software is
 * used. This software can be redistributed and/or modified freely provided that any derivative
 * works bear some notice that they are derived from it, and any modified versions bear some notice
 * that they have been modified.
 */

package gov.nist.hit.core.hl7v2.service;

import gov.nist.hit.core.domain.ConformanceProfile;
import gov.nist.hit.core.domain.IntegrationProfile;
import gov.nist.hit.core.domain.ProfileModel;
import gov.nist.hit.core.domain.Stage;
import gov.nist.hit.core.domain.TestCaseDocument;
import gov.nist.hit.core.domain.TestContext;
import gov.nist.hit.core.domain.VocabularyLibrary;
import gov.nist.hit.core.hl7v2.domain.HL7V2TestContext;
import gov.nist.hit.core.hl7v2.domain.HLV2TestCaseDocument;
import gov.nist.hit.core.hl7v2.repo.HL7V2TestContextRepository;
import gov.nist.hit.core.repo.VocabularyLibraryRepository;
import gov.nist.hit.core.service.ResourcebundleLoader;
import gov.nist.hit.core.service.ValueSetLibrarySerializer;
import gov.nist.hit.core.service.exception.ProfileParserException;
import gov.nist.hit.core.service.impl.ValueSetLibrarySerializerImpl;
import gov.nist.hit.core.service.util.FileUtil;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.JsonMappingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class HL7V2ResourcebundleLoaderImpl extends ResourcebundleLoader {

  static final Logger logger = LoggerFactory.getLogger(HL7V2ResourcebundleLoaderImpl.class);
  static final String FORMAT = "hl7v2";

  @Autowired
  HL7V2TestContextRepository testContextRepository;

  @Autowired
  VocabularyLibraryRepository vocabularyLibraryRepository;

  HL7V2ProfileParser profileParser = new HL7V2ProfileParserImpl();
  ValueSetLibrarySerializer valueSetLibrarySerializer = new ValueSetLibrarySerializerImpl();

  public HL7V2ResourcebundleLoaderImpl() {
    super();
  }


  @Override
  public TestCaseDocument generateTestCaseDocument(TestContext c) throws IOException {
    HLV2TestCaseDocument doc = new HLV2TestCaseDocument();
    if (c != null) {
      HL7V2TestContext context = testContextRepository.findOne(c.getId());
      doc.setExMsgPresent(context.getMessage() != null && context.getMessage().getContent() != null);
      doc.setXmlConfProfilePresent(context.getConformanceProfile() != null
          && context.getConformanceProfile().getJson() != null);
      doc.setXmlValueSetLibraryPresent(context.getVocabularyLibrary() != null
          && context.getVocabularyLibrary().getJson() != null);
    }
    return doc;
  }


  @Override
  public TestContext testContext(String path, JsonNode formatObj, Stage stage) throws IOException {
    // for backward compatibility
    formatObj = formatObj.findValue(FORMAT) != null ? formatObj.findValue(FORMAT) : formatObj;
    HL7V2TestContext testContext = new HL7V2TestContext();
    testContext.setFormat(FORMAT);
    testContext.setStage(stage);
    JsonNode messageId = formatObj.findValue("messageId");
    JsonNode constraintId = formatObj.findValue("constraintId");
    JsonNode valueSetLibraryId = formatObj.findValue("valueSetLibraryId");
    if (valueSetLibraryId != null && !"".equals(valueSetLibraryId.getTextValue())) {
      testContext.setVocabularyLibrary((getVocabularyLibrary(valueSetLibraryId.getTextValue())));
    }
    if (constraintId != null && !"".equals(constraintId.getTextValue())) {
      testContext.setConstraints(getConstraints(constraintId.getTextValue()));
    }
    testContext.setAddditionalConstraints(additionalConstraints(path + CONSTRAINTS_FILE_PATTERN));
    testContext.setMessage(message(FileUtil.getContent(getResource(path + "Message.txt"))));
    if (testContext.getMessage() == null) {
      testContext.setMessage(message(FileUtil.getContent(getResource(path + "Message.text"))));
    }
    if (messageId != null) {
      try {
        ConformanceProfile conformanceProfile = new ConformanceProfile();
        IntegrationProfile integrationProfile = getIntegrationProfile(messageId.getTextValue());
        conformanceProfile.setJson(jsonConformanceProfile(integrationProfile.getXml(), messageId
            .getTextValue(), testContext.getConstraints() != null ? testContext.getConstraints()
            .getXml() : null, testContext.getAddditionalConstraints() != null ? testContext
            .getAddditionalConstraints().getXml() : null));
        conformanceProfile.setIntegrationProfile(integrationProfile);
        conformanceProfile.setSourceId(messageId.getTextValue());
        testContext.setConformanceProfile(conformanceProfile);
      } catch (ProfileParserException e) {
        throw new RuntimeException("Failed to parse integrationProfile at " + path);
      }
    }
    return testContext;
  }


  @Override
  public ProfileModel parseProfile(String integrationProfileXml, String conformanceProfileId,
      String constraintsXml, String additionalConstraintsXml) throws ProfileParserException {
    return profileParser.parse(integrationProfileXml, conformanceProfileId, constraintsXml,
        additionalConstraintsXml);
  }



  @Override
  protected VocabularyLibrary vocabLibrary(String content) throws JsonGenerationException,
      JsonMappingException, IOException {
    Document doc = this.stringToDom(content);
    VocabularyLibrary vocabLibrary = new VocabularyLibrary();
    Element valueSetLibraryeElement = (Element) doc.getElementsByTagName("ValueSetLibrary").item(0);
    vocabLibrary.setSourceId(valueSetLibraryeElement.getAttribute("ValueSetLibraryIdentifier"));
    vocabLibrary.setName(valueSetLibraryeElement.getAttribute("Name"));
    vocabLibrary.setDescription(valueSetLibraryeElement.getAttribute("Description"));
    vocabLibrary.setXml(content);
    vocabLibrary.setJson(obm.writeValueAsString(valueSetLibrarySerializer.toObject(content)));
    vocabularyLibraryRepository.save(vocabLibrary);
    return vocabLibrary;
  }



}
