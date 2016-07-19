package gov.nist.hit.core.hl7v2.service.profile.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import gov.nist.hit.core.domain.ProfileElement;
import gov.nist.hit.core.domain.ProfileModel;
import gov.nist.hit.core.hl7v2.service.HL7V2ProfileParserImpl;
import gov.nist.hit.core.service.ProfileParser;
import gov.nist.hit.core.service.exception.ProfileParserException;


public class ProfileParserImplTest {

  ProfileParser parser = new HL7V2ProfileParserImpl();

  //
  // @Test
  // public void testParse() throws ProfileParserException, IOException {
  // String profile =
  // IOUtils.toString(ProfileParserImplTest.class
  // .getResourceAsStream("/profiles/1_1_1_Profile.xml"));
  // String constraints =
  // IOUtils.toString(ProfileParserImplTest.class
  // .getResourceAsStream("/constraints/1_1_1_Constraints.xml"));
  // ProfileModel model = parser.parse(profile, "bfb1c703-c96e-4f2b-8950-3f5b1c9cd2d8",
  // constraints);
  //
  // }


  @Test
  public void testParseLRIProfile() throws ProfileParserException, IOException {
    String profile = IOUtils
        .toString(ProfileParserImplTest.class.getResourceAsStream("/profiles/1_1_2_Profile.xml"));
    String constraints = IOUtils.toString(
        ProfileParserImplTest.class.getResourceAsStream("/constraints/1_1_2_Constraints.xml"));
    ProfileModel model = parser.parse(profile, "ORU_R01:LRI_GU_FRN", constraints);
    ProfileElement message = model.getMessage();
    ProfileElement group = message.getChildren().get(2);
    assertEquals("PATIENT_RESULT", group.getName());
    group = group.getChildren().get(1);
    assertEquals("ORDER_OBSERVATION", group.getName());
    List<gov.nist.hit.core.domain.constraints.Predicate> predicates = group.getPredicates();
    assertFalse(predicates.size() == 0);
    group = group.getChildren().get(5);
    assertEquals("OBSERVATION", group.getName());
    predicates = group.getPredicates();
    assertFalse(predicates.size() == 0);


  }



}
