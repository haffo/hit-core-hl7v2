package gov.nist.hit.core.hl7v2.service.impl;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;

import gov.nist.hit.core.Constant;
import gov.nist.hit.core.domain.MessageValidationCommand;
import gov.nist.hit.core.domain.MessageValidationResult;
import gov.nist.hit.core.domain.TestContext;
import gov.nist.hit.core.hl7v2.service.HL7V2MessageValidator;
import gov.nist.hit.core.service.AppInfoService;
import gov.nist.hit.core.service.exception.MessageValidationException;

public class HL7V2MessageValidatorImpl extends HL7V2MessageValidator {

	@Autowired
	AppInfoService appInfoService;

	private String organizationName;

	@PostConstruct
	public void init() {
		organizationName = appInfoService.get().getOptions().get(Constant.ORGANIZATION_NAME);
	}

	@Override
	public MessageValidationResult validate(TestContext testContext, MessageValidationCommand command)
			throws MessageValidationException {
		return super.validate(testContext, command);
	}

	@Override
	public String getProviderName() {
		// TODO Auto-generated method stub
		return organizationName;
	}

	@Override
	public String getValidationServiceName() {
		// TODO Auto-generated method stub
		return organizationName + " Validation Tool";
	}

}
