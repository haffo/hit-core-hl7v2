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

import gov.nist.healthcare.tools.core.models.ValidationResult;
import gov.nist.healthcare.tools.core.services.Validator;
import gov.nist.healthcare.tools.core.services.exception.ValidationException;

public interface Er7MessageValidator extends Validator {

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
 	@Deprecated
	public ValidationResult validate(String message, String title,
			String... options); 
 	
 	
 	/**
 	 * Validate the message and return a json representation of the report
 	 * @param message
 	 * @param title
 	 * @param options
 	 * @return
 	 * @throws ValidationException 
 	 */
 	public String validatetoJson(String title, String message,
			String profile, String constraints,String valueSets) throws ValidationException;
 	
	

}
