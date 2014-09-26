package gov.nist.healthcare.tools.core.services.hl7.v2.profile.unit;

import gov.nist.healthcare.tools.core.models.profile.ProfileElement;
import gov.nist.healthcare.tools.core.models.profile.ProfileModel;
import gov.nist.healthcare.tools.core.services.hl7.v2.profile.ProfileParserImpl;
import gov.nist.healthcare.tools.core.services.profile.ProfileParser;
import gov.nist.healthcare.tools.core.services.profile.ProfileParserException;

import java.io.IOException;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

public class ProfileParserImplTest {
	
	ProfileParser parser = new ProfileParserImpl();
	
	@Test
	public void testParse() throws ProfileParserException, IOException{
		ProfileModel model = parser.parse(IOUtils.toString(ProfileParserImplTest.class.getResourceAsStream("/profiles/IZ_VXU_1.5_IZ22-PROFILE-NIST.xml")));
		List<ProfileElement> elements = model.getElements();
		ProfileElement element = elements.get(1).getChildren().get(0);
	}
	 
}
