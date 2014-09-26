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


import gov.nist.healthcare.tools.core.models.message.MessageModel;
import gov.nist.healthcare.tools.core.services.message.MessageParser;
import gov.nist.healthcare.tools.core.services.message.MessageParserException;

public interface Er7MessageParser  extends MessageParser{

	/**
	 * 
	 * @param message
	 * @return
	 * @throws SoapParserException
	 */
 	@Override
	public MessageModel parse(String er7Message, Object... options) throws MessageParserException;
 
}
