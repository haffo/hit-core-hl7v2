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

import static org.junit.Assert.assertEquals;
import gov.nist.hit.core.hl7v2.service.HL7V2Util;
import gov.nist.hit.core.service.exception.MessageParserException;

import org.junit.Test;

public class Er7UtilTest {

  @Test
  public void testGetPosition() throws MessageParserException {
    String path = "MSH[1]";
    assertEquals(1, HL7V2Util.getPosition(path, "SEGMENT"));

    path = "MSH[1].7[1]";
    assertEquals(7, HL7V2Util.getPosition(path, "FIELD"));

    path = "MSH[1].7[1].3[3]";
    assertEquals(3, HL7V2Util.getPosition(path, "COMPONENT"));

    path = "MSH[1].7[1].3[3].4[1]";
    assertEquals(4, HL7V2Util.getPosition(path, "SUB_COMPONENT"));

    path = "MSH[1].13[1]";
    assertEquals(13, HL7V2Util.getPosition(path));

    path = "MSH[1].7[1].50[3]";
    assertEquals(50, HL7V2Util.getPosition(path));

    path = "MSH[1].7[1].3[3].56[1]";
    assertEquals(56, HL7V2Util.getPosition(path));

  }

}
