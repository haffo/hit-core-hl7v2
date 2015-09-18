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

package gov.nist.hit.core.hl7v2.api;

import gov.nist.hit.core.domain.MessageParserCommand;
import gov.nist.hit.core.domain.MessageValidationCommand;
import gov.nist.hit.core.domain.MessageValidationResult;
import gov.nist.hit.core.hl7v2.domain.HL7V2TestContext;
import gov.nist.hit.core.hl7v2.repo.HL7V2TestContextRepository;
import gov.nist.hit.core.hl7v2.service.HL7V2MessageParser;
import gov.nist.hit.core.hl7v2.service.HL7V2MessageValidator;
import gov.nist.hit.core.service.exception.MessageParserException;
import gov.nist.hit.core.service.exception.MessageValidationException;
import gov.nist.hit.core.service.exception.TestCaseException;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Harold Affo (NIST)
 * 
 */

@RequestMapping("/hl7v2/testcontext")
@RestController
public class HL7V2TestContextController {

  Logger logger = LoggerFactory.getLogger(HL7V2TestContextController.class);

  @Autowired
  protected HL7V2TestContextRepository testContextRepository;

  @Autowired
  protected HL7V2MessageValidator messageValidator;

  @Autowired
  protected HL7V2MessageParser messageParser;


  @RequestMapping(value = "/{testContextId}")
  public HL7V2TestContext testContext(@PathVariable final Long testContextId) {
    logger.info("Fetching testContext with id=" + testContextId);
    HL7V2TestContext testContext = testContextRepository.findOne(testContextId);
    if (testContext == null) {
      throw new TestCaseException("No test context available with id=" + testContextId);
    }
    return testContext;
  }

  @RequestMapping(value = "/{testContextId}/parseMessage", method = RequestMethod.POST)
  public List<gov.nist.hit.core.domain.MessageElement> parse(
      @PathVariable final Long testContextId, @RequestBody final MessageParserCommand command)
      throws MessageParserException {
    logger.info("Parsing message");
    HL7V2TestContext testContext = testContext(testContextId);
    return messageParser.parse(testContext, command).getElements();
  }

  @RequestMapping(value = "/{testContextId}/validateMessage", method = RequestMethod.POST)
  public MessageValidationResult validate(@PathVariable final Long testContextId,
      @RequestBody final MessageValidationCommand command) throws MessageValidationException {
    try {
      return messageValidator.validate(testContext(testContextId), command);
    } catch (MessageValidationException e) {
      throw new MessageValidationException(e.getMessage());
    } catch (Exception e) {
      throw new MessageValidationException(e.getMessage());
    }
  }

  public HL7V2TestContextRepository getTestContextRepository() {
    return testContextRepository;
  }

  public void setTestContextRepository(HL7V2TestContextRepository testContextRepository) {
    this.testContextRepository = testContextRepository;
  }

  public HL7V2MessageValidator getMessageValidator() {
    return messageValidator;
  }

  public void setMessageValidator(HL7V2MessageValidator messageValidator) {
    this.messageValidator = messageValidator;
  }

  public HL7V2MessageParser getMessageParser() {
    return messageParser;
  }

  public void setMessageParser(HL7V2MessageParser messageParser) {
    this.messageParser = messageParser;
  }



}
