package gov.nist.hit.core.hl7v2.service.table.unit;

import static org.junit.Assert.assertFalse;
import gov.nist.hit.core.domain.ValueSetLibrary;
import gov.nist.hit.core.service.exception.ProfileParserException;
import gov.nist.hit.core.service.impl.ValueSetLibrarySerializerImpl;

import java.io.IOException;

import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

public class TableLibrarySerializerTest {

  ValueSetLibrarySerializerImpl serializer = null;

  @Test
  public void testSerialize() throws ProfileParserException, IOException, XPathExpressionException {
    String valueSets =
        IOUtils.toString(TableLibrarySerializerTest.class.getResourceAsStream("/ValueSetDefinitions.xml"));
    serializer = new ValueSetLibrarySerializerImpl();
    ValueSetLibrary valueSetLibrary = serializer.toTableLibrary(valueSets);
    assertFalse(valueSetLibrary == null);
  }

  @Test
  public void testDeSerialize() throws ProfileParserException, IOException,
      XPathExpressionException {
    String valueSets =
        IOUtils.toString(TableLibrarySerializerTest.class.getResourceAsStream("/ValueSetDefinitions.xml"));
    serializer = new ValueSetLibrarySerializerImpl();
    ValueSetLibrary valueSetLibrary = serializer.toTableLibrary(valueSets);
    assertFalse(valueSetLibrary == null);
    String xml = serializer.toString(valueSetLibrary);
    assertFalse(xml == null);
  }

}
