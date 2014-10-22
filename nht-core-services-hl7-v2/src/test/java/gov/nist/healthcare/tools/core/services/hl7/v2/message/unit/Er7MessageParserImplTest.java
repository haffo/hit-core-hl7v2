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

package gov.nist.healthcare.tools.core.services.hl7.v2.message.unit;

import static org.junit.Assert.assertEquals;
import gov.nist.healthcare.tools.core.models.message.MessageElement;
import gov.nist.healthcare.tools.core.models.message.MessageElementData;
import gov.nist.healthcare.tools.core.models.message.MessageModel;
import gov.nist.healthcare.tools.core.services.hl7.v2.message.Er7MessageParserImpl;
import gov.nist.healthcare.tools.core.services.message.MessageParser;
import gov.nist.healthcare.tools.core.services.message.MessageParserException;

import java.io.IOException;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;

public class Er7MessageParserImplTest {

	private static String er7Message;

	private static String xmlProfile;

	MessageParser parser = new Er7MessageParserImpl();

	@BeforeClass
	public static void setUp() throws IOException {
		er7Message = getEr7Message();
		xmlProfile = getProfile();
	}

	//@Test
	public void testIndex() throws MessageParserException {
		MessageModel model = parser.parse(er7Message, xmlProfile);
		List<MessageElement> elements = model.getElements();
		for (int index = 0; index < elements.size(); index++) {
			MessageElement element = elements.get(index);
			assertEndIndex(element);
		}
	}

	private void assertEndIndex(MessageElement element) {
		MessageElementData elementData = element.getData();
		assertEquals(
				elementData.getStringRepresentation() != null ? elementData
						.getStringRepresentation().length()
						: elementData.getStartIndex(),
				elementData.getEndIndex());
		List<MessageElement> children = element.getChildren();
		if (children != null) {
			for (MessageElement child : children) {
				assertEndIndex(child);
			}
		}
	}

	private static String getEr7Message() throws IOException {
		return IOUtils.toString(Er7MessageParserImplTest.class
				.getResourceAsStream("/messages/ELR.txt"));
	}

	private static String getProfile() throws IOException {
		return IOUtils.toString(Er7MessageParserImplTest.class
				.getResourceAsStream("/profiles/IZ_VXU_1.5_IZ22-PROFILE-NIST.xml"));
	}
}
