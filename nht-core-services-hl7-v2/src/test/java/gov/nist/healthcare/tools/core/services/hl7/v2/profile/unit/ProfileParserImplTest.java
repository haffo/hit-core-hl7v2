package gov.nist.healthcare.tools.core.services.hl7.v2.profile.unit;

import gov.nist.healthcare.tools.core.models.ProfileModel;
import gov.nist.healthcare.tools.core.services.ProfileParser;
import gov.nist.healthcare.tools.core.services.exception.ProfileParserException;
import gov.nist.healthcare.tools.core.services.hl7.v2.profile.ProfileParserImpl;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

public class ProfileParserImplTest {
	
	ProfileParser parser = new ProfileParserImpl();
	
	@Test
	public void testParse() throws ProfileParserException, IOException{
		String profile = IOUtils.toString(ProfileParserImplTest.class.getResourceAsStream("/new_validation/Profile.xml"));
		String consStatements =  IOUtils.toString(ProfileParserImplTest.class.getResourceAsStream("/new_validation/ConformanceStatementConstraints.xml"));
		String predicates =  IOUtils.toString(ProfileParserImplTest.class.getResourceAsStream("/new_validation/PredicateConstraints.xml"));
		ProfileModel model = parser.parse(profile, consStatements,predicates);
 
	}
	 
}
