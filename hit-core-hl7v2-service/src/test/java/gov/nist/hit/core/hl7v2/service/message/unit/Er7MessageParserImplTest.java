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

package gov.nist.hit.core.hl7v2.service.message.unit;


public class Er7MessageParserImplTest {

  // private static String er7Message;
  //
  // private static String xmlProfile;
  //
  // MessageParser parser = new Er7MessageParser();
  //
  // @BeforeClass
  // public static void setUp() throws IOException {
  // er7Message = getEr7Message();
  // xmlProfile = getProfile();
  // }
  //
  // @Test
  // public void testParse() throws MessageParserException {
  // MessageModel model = parser.parse(er7Message, xmlProfile);
  // List<MessageElement> elements = model.getElements();
  // for (int index = 0; index < elements.size(); index++) {
  // MessageElement element = elements.get(index);
  // assertEndIndex(element);
  // }
  // }
  //
  // /**
  // *
  // * @param element
  // */
  // private void assertEndIndex(MessageElement element) {
  // MessageElementData elementData = element.getData();
  // Assert.assertTrue(elementData.getStartIndex() > 0);
  // List<MessageElement> children = element.getChildren();
  // if (children != null) {
  // for (MessageElement child : children) {
  // assertEndIndex(child);
  // }
  // }
  // }
  //
  // /**
  // *
  // * @return
  // * @throws IOException
  // */
  // private static String getEr7Message() throws IOException {
  // return IOUtils
  // .toString(Er7MessageParserImplTest.class.getResourceAsStream("/messages/ELR.txt"));
  // }
  //
  // /**
  // *
  // * @return
  // * @throws IOException
  // */
  // private static String getProfile() throws IOException {
  // return IOUtils.toString(Er7MessageParserImplTest.class
  // .getResourceAsStream("/new_validation/IntegrationProfile.xml"));
  // }
}
