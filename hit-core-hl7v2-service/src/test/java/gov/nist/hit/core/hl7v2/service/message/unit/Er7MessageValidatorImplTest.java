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

import gov.nist.hit.core.hl7v2.service.message.Er7MessageValidator;
import gov.nist.hit.core.service.exception.MessageParserException;
import gov.nist.hit.core.service.exception.MessageValidationException;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.Test;

public class Er7MessageValidatorImplTest {

  Er7MessageValidator validator = new Er7MessageValidator();

  @BeforeClass
  public static void setUp() throws IOException {

  }

  @Test
  public void testValidateWithProfile() throws MessageParserException, IOException,
      MessageValidationException {
    String report =
        validator
            .validatetoJson("JunitTest", getEr7Message(), getProfile(), getConstraints(), null);
    System.out.println(report);
  }

  /**
   * 
   * @return
   * @throws IOException
   */
  private static String getEr7Message() throws IOException {
    return IOUtils.toString(Er7MessageValidatorImplTest.class
        .getResourceAsStream("/messages/ELR.txt"));
  }

  /**
   * 
   * @return
   * @throws IOException
   */
  private static String getProfile() throws IOException {
    return IOUtils.toString(Er7MessageValidatorImplTest.class
        .getResourceAsStream("/new_validation/Profile.xml"));
  }

  private static String getConstraints() throws IOException {
    return IOUtils.toString(Er7MessageValidatorImplTest.class
        .getResourceAsStream("/new_validation/Constraints.xml"));
  }

}
