package gov.nist.hit.core.hl7v2.service.profile.unit;

import gov.nist.hit.core.domain.ProfileModel;
import gov.nist.hit.core.hl7v2.service.HL7V2ProfileParserImpl;
import gov.nist.hit.core.service.ProfileParser;
import gov.nist.hit.core.service.exception.ProfileParserException;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.junit.Test;


public class ProfileParserImplTest {

  ProfileParser parser = new HL7V2ProfileParserImpl();

  @Test
  public void testParse() throws ProfileParserException, IOException {
    String profile =
        IOUtils.toString(ProfileParserImplTest.class
            .getResourceAsStream("/profiles/1_1_1_Profile.xml"));
    String constraints =
        IOUtils.toString(ProfileParserImplTest.class
            .getResourceAsStream("/constraints/1_1_1_Constraints.xml"));
    ProfileModel model = parser.parse(profile, "bfb1c703-c96e-4f2b-8950-3f5b1c9cd2d8", constraints);

  }


}
