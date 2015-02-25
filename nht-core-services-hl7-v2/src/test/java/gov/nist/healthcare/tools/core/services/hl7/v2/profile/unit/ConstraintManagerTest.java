package gov.nist.healthcare.tools.core.services.hl7.v2.profile.unit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import gov.nist.healthcare.tools.core.models.Constraint;
import gov.nist.healthcare.tools.core.services.exception.ProfileParserException;
import gov.nist.healthcare.tools.core.services.hl7.v2.profile.ConstraintManager;

import java.io.IOException;
import java.util.Set;

import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

public class ConstraintManagerTest {
	
	ConstraintManager manager = null;
	
 
	
	@Test
	public void testFindByIdAndPath() throws ProfileParserException, IOException, XPathExpressionException{
 		
		// 1.0 Conformance statements 
		String constraints =  IOUtils.toString(ConstraintManagerTest.class.getResourceAsStream("/new_validation/ConformanceStatementConstraints.xml"));
 		manager = new ConstraintManager(constraints);
 		// check constraint of field
 		Set<Constraint> cols = manager.findConfStatementsByIdAndPath("Segment", "8dfae017-9261-46e0-98f9-d1bea58734af","1[1]"); 
 		System.out.println("--------------->Conformance Statements: 1[1]<-------------------");
 		for(Constraint co: cols){
 			System.out.println(co.getId() + "-" + co.getDescription());
 		}
 		assertFalse(cols.isEmpty());
 		assertTrue(cols.size() == 2);
 		System.out.println("--------------->Conformance Statements:20[1]<-------------------");
 		cols = manager.findConfStatementsByIdAndPath("Segment", "092444dd-273b-4b2d-84bd-0f0ddd06cf59","20[1]"); 
 		for(Constraint co: cols){
 			System.out.println(co.getId() + "-" + co.getDescription());
 		}
 		assertFalse(cols.isEmpty());
 		assertTrue(cols.size() == 2);
 		System.out.println("--------------->Conformance Statements:6[1]<-------------------");
 		cols = manager.findConfStatementsByIdAndPath("Segment", "092444dd-273b-4b2d-84bd-0f0ddd06cf59","6[1]"); 
 		for(Constraint co: cols){
 			System.out.println(co.getId() + "-" + co.getDescription());
 		}
 		assertFalse(cols.isEmpty());
 		assertTrue(cols.size() == 3);
 		
 		// check constraint of component 
 		System.out.println("--------------->Conformance Statements:7[1].1[1]<-------------------");
 		cols = manager.findConfStatementsByIdAndPath("Segment", "8dfae017-9261-46e0-98f9-d1bea58734af","7[1].1[1]"); 
 		for(Constraint co: cols){
 			System.out.println(co.getId() + "-" + co.getDescription());
 		}
 		assertFalse(cols.isEmpty());
 		assertTrue(cols.size() == 1);
	}
	
	
 
}
