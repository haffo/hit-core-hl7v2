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
	public void testFind() throws ProfileParserException, IOException, XPathExpressionException{
 		String constraints =  IOUtils.toString(ConstraintManagerTest.class.getResourceAsStream("/new_validation/ConformanceStatementConstraints.xml"));
 		manager = new ConstraintManager(constraints);
 		Set<Constraint> cols = manager.findById("Segment", "MSH");
 		assertFalse(cols.isEmpty());
 		assertTrue(cols.size() == 4);		
	}
	 
}
