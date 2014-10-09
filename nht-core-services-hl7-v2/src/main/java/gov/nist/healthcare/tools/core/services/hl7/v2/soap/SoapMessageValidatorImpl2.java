package gov.nist.healthcare.tools.core.services.hl7.v2.soap;

import gov.nist.healthcare.core.MalformedMessageException;
import gov.nist.healthcare.core.validation.soap.SoapMessage;
import gov.nist.healthcare.core.validation.soap.SoapValidationResult;
import gov.nist.healthcare.tools.core.models.validation.ValidationResult;
import gov.nist.healthcare.tools.core.services.validation.soap.SoapMessageValidator;
import gov.nist.healthcare.tools.core.services.validation.soap.SoapValidationException;
import gov.nist.healthcare.tools.core.services.xml.XMLValidator;



public class SoapMessageValidatorImpl2 implements SoapMessageValidator {

	String schema ="/Users/indovina/XMLValidator/soap12_cdc-iisb-2011.xsd";
	
	@Override  
	public ValidationResult validate(String soap, String testCaseTitle,
			Object... options) {
		try {
			SoapMessage message = new SoapMessage(soap);
			XMLValidator validator = new XMLValidator();
			SoapValidationResult tmp = validator.validate(message, schema);
			return new gov.nist.healthcare.tools.core.models.hl7.v2.soap.SoapValidationResult(tmp, testCaseTitle);
		} catch (MalformedMessageException e) {
			throw new SoapValidationException(e);
		}
	}
}

