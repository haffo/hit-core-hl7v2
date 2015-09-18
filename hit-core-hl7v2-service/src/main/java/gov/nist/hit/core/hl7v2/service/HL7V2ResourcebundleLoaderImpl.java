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
import gov.nist.hit.core.domain.TestCaseDocument;
import gov.nist.hit.core.domain.TestContext;
import gov.nist.hit.core.domain.TestDomain;
import gov.nist.hit.core.hl7v2.domain.HL7V2TestContext;
import gov.nist.hit.core.hl7v2.repo.HL7V2TestContextRepository;
import gov.nist.hit.core.service.ResourcebundleLoader;
import gov.nist.hit.core.service.ValueSetLibrarySerializer;
import gov.nist.hit.core.service.exception.ProfileParserException;
import gov.nist.hit.core.service.impl.ValueSetLibrarySerializerImpl;
import gov.nist.hit.core.service.util.FileUtil;

import java.io.IOException;

import org.codehaus.jackson.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class HL7V2ResourcebundleLoaderImpl extends ResourcebundleLoader {

  static final Logger logger = LoggerFactory.getLogger(HL7V2ResourcebundleLoaderImpl.class);

  @Autowired
  HL7V2TestContextRepository testContextRepository;

  HL7V2ProfileParser profileParser = new HL7V2ProfileParserImpl();
  ValueSetLibrarySerializer valueSetLibrarySerializer = new ValueSetLibrarySerializerImpl();

  public HL7V2ResourcebundleLoaderImpl() {
    super();
  }


  @Override
  public TestCaseDocument setTestContextDocument(TestContext c, TestCaseDocument doc)
      throws IOException {
    if (c != null) {
      HL7V2TestContext context = testContextRepository.findOne(c.getId());
      doc.setExMsgPresent(context.getMessage() != null && context.getMessage().getContent() != null);
      doc.setXmlConfProfilePresent(context.getConformanceProfile() != null
          && context.getConformanceProfile().getXml() != null);
      doc.setXmlValueSetLibraryPresent(context.getVocabularyLibrary() != null
          && context.getVocabularyLibrary().getXml() != null);
    }
    return doc;
  }


  @Override
  public TestContext testContext(String path, JsonNode domainObj) throws IOException {
    // for backward compatibility
    domainObj = domainObj.findValue("hl7v2") != null ? domainObj.findValue("hl7v2") : domainObj;
    HL7V2TestContext testContext = new HL7V2TestContext();
    testContext.setDomain(TestDomain.HL7V2);
    JsonNode messageId = domainObj.findValue("messageId");
    JsonNode constraintId = domainObj.findValue("constraintId");
    JsonNode valueSetLibraryId = domainObj.findValue("valueSetLibraryId");
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



}
