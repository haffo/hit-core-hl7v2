package gov.nist.hit.core.hl7v2.service.table.unit;

import static org.junit.Assert.assertFalse;
import gov.nist.hit.core.domain.TableLibrary;
import gov.nist.hit.core.hl7v2.service.vocabulary.TableLibrarySerializer;
import gov.nist.hit.core.service.exception.ProfileParserException;

import java.io.IOException;

import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

public class TableLibrarySerializerTest {

	TableLibrarySerializer serializer = null;

	@Test
	public void testSerialize() throws ProfileParserException, IOException,
			XPathExpressionException {
		String valueSets = IOUtils.toString(TableLibrarySerializerTest.class
				.getResourceAsStream("/ValueSets.xml"));
		serializer = new TableLibrarySerializer();
		TableLibrary tableLibrary = serializer.toTableLibrary(valueSets);
		assertFalse(tableLibrary == null);
	}

	@Test
	public void testDeSerialize() throws ProfileParserException, IOException,
			XPathExpressionException {
		String valueSets = IOUtils.toString(TableLibrarySerializerTest.class
				.getResourceAsStream("/ValueSets.xml"));
		serializer = new TableLibrarySerializer();
		TableLibrary tableLibrary = serializer.toTableLibrary(valueSets);
		assertFalse(tableLibrary == null);
		String xml = serializer.toString(tableLibrary);
		assertFalse(xml == null);
	}

}
