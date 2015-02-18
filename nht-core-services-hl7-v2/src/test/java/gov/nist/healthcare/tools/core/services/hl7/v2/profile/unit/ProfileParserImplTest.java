package gov.nist.healthcare.tools.core.services.hl7.v2.profile.unit;

import gov.nist.healthcare.tools.core.models.Constraint;
import gov.nist.healthcare.tools.core.models.ProfileElement;
import gov.nist.healthcare.tools.core.models.ProfileModel;
import gov.nist.healthcare.tools.core.services.ProfileParser;
import gov.nist.healthcare.tools.core.services.exception.ProfileParserException;
import gov.nist.healthcare.tools.core.services.hl7.v2.profile.ProfileParserImpl;

import java.io.IOException;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

public class ProfileParserImplTest {
	
	ProfileParser parser = new ProfileParserImpl();
	
	@Test
	public void testParse() throws ProfileParserException, IOException{
		String profile = IOUtils.toString(ProfileParserImplTest.class.getResourceAsStream("/IZ_V1.5_ACK_Z23/Profile.xml"));
		String consStatements =  IOUtils.toString(ProfileParserImplTest.class.getResourceAsStream("/IZ_V1.5_ACK_Z23/ConformanceStatementConstraints.xml"));
		String predicates =  IOUtils.toString(ProfileParserImplTest.class.getResourceAsStream("/IZ_V1.5_ACK_Z23/PredicateConstraints.xml"));
		ProfileModel model = parser.parse(profile, consStatements,predicates);
		List<ProfileElement> elements = model.getElements();
		for(ProfileElement element: elements){
			printConstraints(element);
		}
		
	}
	 
	
	
	private void printConstraints(ProfileElement element){
 		if(element.getConformanceStatements() != null  && !element.getConformanceStatements() .isEmpty()){
			System.out.println("--------------->Conformance Statements:" + element.getPath()+"<-------------------");
			for(Constraint co: element.getConformanceStatements()){
				System.out.println("Conformance Statement: "+ co.getId() + "--" + co.getDescription());
			}
		}
		
		if(element.getPredicates() != null && !element.getPredicates() .isEmpty()){
			System.out.println("--------------->Predicates:" + element.getPath()+"<-------------------");
			for(Constraint pre: element.getPredicates()){
				System.out.println("Predicate: "+ pre.getId() + "--" + pre.getDescription());
			}
		}  
		
		List<ProfileElement> elements = element.getChildren();
		for(ProfileElement child: elements){
			printConstraints(child);
		}
		
	}
}
