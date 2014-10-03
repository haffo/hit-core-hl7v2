/**
 * This software was developed at the National Institute of Standards and Technology by employees
 * of the Federal Government in the course of their official duties. Pursuant to title 17 Section 105 of the
 * United States Code this software is not subject to copyright protection and is in the public domain.
 * This is an experimental system. NIST assumes no responsibility whatsoever for its use by other parties,
 * and makes no guarantees, expressed or implied, about its quality, reliability, or any other characteristic.
 * We would appreciate acknowledgement if the software is used. This software can be redistributed and/or
 * modified freely provided that any derivative works bear some notice that they are derived from it, and any
 * modified versions bear some notice that they have been modified.
 */
package gov.nist.healthcare.tools.core.services.hl7.v2.soap;

import gov.nist.healthcare.core.MalformedMessageException;
import gov.nist.healthcare.core.validation.soap.SoapMessage;
import gov.nist.healthcare.core.validation.soap.SoapValidationResult;
import gov.nist.healthcare.tools.core.models.validation.ValidationResult;
import gov.nist.healthcare.tools.core.services.validation.soap.SoapMessageValidator;
import gov.nist.healthcare.tools.core.services.validation.soap.SoapValidationException;

import org.apache.xmlbeans.XmlException;

public class SoapMessageValidatorImpl implements SoapMessageValidator {

	@Override  
	public ValidationResult validate(String soap, String testCaseTitle,
			Object... options) {
		try {
			SoapMessage message = new SoapMessage(soap);
			gov.nist.healthcare.core.validation.soap.SoapValidation validation = new gov.nist.healthcare.core.validation.soap.SoapValidation();
			SoapValidationResult tmp = validation
					.validate(
							message, 
							SoapMessageValidator.class
									.getResourceAsStream("/soap/schematron.sch"));
			return new gov.nist.healthcare.tools.core.models.hl7.v2.soap.SoapValidationResult(
					tmp, testCaseTitle);
		} catch (XmlException e) {
			throw new SoapValidationException(e);
		} catch (MalformedMessageException e) {
			throw new SoapValidationException(e);
		}
	}
}
