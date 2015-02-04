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
package gov.nist.healthcare.tools.core.services.hl7.v2.profile;

import gov.nist.healthcare.tools.core.models.ProfileModel;
import gov.nist.healthcare.tools.core.services.ProfileParser;
import gov.nist.healthcare.tools.core.services.exception.ProfileParserException;
import hl7.v2.profile.Profile;
import hl7.v2.profile.XMLDeserializer;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;

public class ProfileParserImpl implements ProfileParser {


	public ProfileParserImpl() {
	}

	@Override
	/**
	 * TODO: we are only parsing one message. 
	 * Determine if we should parse all messages in a profile.
	 * 
	 */
	public ProfileModel parse(String content, Object... options)
			throws ProfileParserException {
		try {
			String confStatementXml  = options != null && options.length > 0  ? (String) options[0]: null;
			String predicateXml  = options != null && options.length > 1  ? (String) options[1]: null;
 			InputStream profileStream = IOUtils.toInputStream(content);
			Profile p = XMLDeserializer.deserialize(profileStream).get();
			scala.collection.Iterable<String> keys = p.messages().keys();
			String key = keys.iterator().next();
			hl7.v2.profile.Message m = p.messages().apply(key);
			return new MessageParser().parse(m, confStatementXml,predicateXml);
		} catch (Exception e) {
			e.printStackTrace();
			throw new ProfileParserException(e.getMessage());
		}
	}

}
