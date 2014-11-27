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
package gov.nist.healthcare.tools.core.services.hl7.v2.message;

import gov.nist.healthcare.core.message.v2.er7.Er7Message;
import gov.nist.healthcare.core.profile.Profile;
import gov.nist.healthcare.core.validation.message.v2.MessageValidationContextV2;
import gov.nist.healthcare.core.validation.message.v2.MessageValidationResultV2;
import gov.nist.healthcare.core.validation.message.v2.MessageValidationV2;
import gov.nist.healthcare.data.TableLibraryDocument;
import gov.nist.healthcare.tools.core.models.ValidationResult;
import gov.nist.healthcare.tools.core.models.hl7.v2.validation.Er7ValidationResult;
import gov.nist.healthcare.tools.core.services.exception.ValidationException;

import java.util.ArrayList;
import java.util.List;

import org.apache.xmlbeans.XmlException;

public class Er7MessageValidatorImpl implements Er7MessageValidator {

	/**
	 * 
	 * @param message
	 * @param title
	 * @param options
	 *            : options[0]: xmlProfile, options[1]: tableLibraries contents,
	 *            options[2]: validation context
	 * @return
	 */
	@Override
	public ValidationResult validate(String er7Message, String title,
			Object... options) {
		String xmlProfile = (String) options[0];
		String validationContext = (String) options[2];
		MessageValidationContextV2 context = getValidationContext(validationContext);
		MessageValidationV2 validator = new MessageValidationV2();
		MessageValidationResultV2 v2Result = null;
		try {
			if (options[1] instanceof String) {
				v2Result = validator.validate(new Er7Message(er7Message),
						new Profile(xmlProfile), context,
						getTableLibraries((String) options[1]), true);
			} else if (options[1] instanceof List<?>) {
				v2Result = validator.validate(new Er7Message(er7Message),
						new Profile(xmlProfile), context,
						getTableLibraries((List<String>) options[1]), true);
			}
			return  new Er7ValidationResult(v2Result, title);
 		} catch (RuntimeException e) {
			throw new ValidationException(e);
		} catch (Exception e) {
			throw new ValidationException(e);
		}
	}

	private List<TableLibraryDocument> getTableLibraries(String xmlTableLibrary) {
		if (xmlTableLibrary != null) {
			List<TableLibraryDocument> tableLibrary = new ArrayList<TableLibraryDocument>();
			try {

				tableLibrary.add(TableLibraryDocument.Factory
						.parse(xmlTableLibrary));
				return tableLibrary;

			} catch (XmlException e) {
				throw new ValidationException(
						"Cannot parse the table library content");
			}
		}
		return null;
	}

	private List<TableLibraryDocument> getTableLibraries(
			List<String> xmlTableLibraries) {
		List<TableLibraryDocument> tableLibraryDocuments = new ArrayList<TableLibraryDocument>();
		try {
			for (String xmlTableLibrary : xmlTableLibraries) {
				tableLibraryDocuments.add(TableLibraryDocument.Factory
						.parse(xmlTableLibrary));
			}
		} catch (XmlException e) {
			throw new ValidationException(
					"Cannot parse the table library content");
		}
		return tableLibraryDocuments;
	}

	private MessageValidationContextV2 getValidationContext(
			String validationContext) {
		MessageValidationContextV2 mvc = new MessageValidationContextV2();
		try {
			if (validationContext != null)
				mvc.load(validationContext);

		} catch (XmlException e) {
			throw new ValidationException(
					"Cannot parse the table validation context");
		}
		return mvc;
	}

}
