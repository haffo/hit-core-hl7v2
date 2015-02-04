package gov.nist.healthcare.tools.core.services.hl7.v2.profile.unit;

import gov.nist.healthcare.tools.core.models.ProfileModel;
import gov.nist.healthcare.tools.core.services.ProfileParser;
import gov.nist.healthcare.tools.core.services.exception.ProfileParserException;
import gov.nist.healthcare.tools.core.services.hl7.v2.profile.ProfileParserImpl;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class ProfileParserImplTest {
	
	ProfileParser parser = new ProfileParserImpl();
	
	@Test
	public void testParse() throws ProfileParserException, IOException{
		String profile = IOUtils.toString(ProfileParserImplTest.class.getResourceAsStream("/new_validation/Profile.xml"));
		String consStatements =  IOUtils.toString(ProfileParserImplTest.class.getResourceAsStream("/new_validation/ConformanceStatementConstraints.xml"));
		String predicates =  IOUtils.toString(ProfileParserImplTest.class.getResourceAsStream("/new_validation/PredicateConstraints.xml"));
		ProfileModel model = parser.parse(profile, consStatements,predicates);
		ObjectMapper obm = new ObjectMapper();
		obm.disable(SerializationFeature.INDENT_OUTPUT);
		obm.setSerializationInclusion(Include.NON_NULL);
		obm.writeValue(new File("/Users/haffo/Documents/DEV/CHECKOUT/nht-core-hl7-v2/nht-core-services-hl7-v2/src/test/resources/new_validation/Profile.json"),model);
		 
		
	}
	 
}
