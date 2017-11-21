package gov.nist.hit.core.hl7v2.service.impl;

import gov.nist.hit.core.domain.MessageValidationCommand;
import gov.nist.hit.core.domain.MessageValidationResult;
import gov.nist.hit.core.domain.TestContext;
import gov.nist.hit.core.hl7v2.service.HL7V2MessageValidator;
import gov.nist.hit.core.service.exception.MessageValidationException;


public class HL7V2MessageValidatorImpl extends HL7V2MessageValidator {

  @Override
  public MessageValidationResult validate(TestContext testContext, MessageValidationCommand command)
      throws MessageValidationException {
    return super.validate(testContext, command);
  }


}
