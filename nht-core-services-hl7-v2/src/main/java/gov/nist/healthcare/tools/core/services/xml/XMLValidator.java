package gov.nist.healthcare.tools.core.services.xml;

import gov.nist.healthcare.core.validation.message.v3.MessageFailureV3;
import gov.nist.healthcare.core.validation.soap.SoapMessage;
import gov.nist.healthcare.tools.core.services.hl7.v2.soap.SoapValidator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.xml.sax.SAXException;

public class XMLValidator {

	
	public XMLValidator() {

	}
 
    public gov.nist.healthcare.core.validation.soap.SoapValidationResult validate(SoapMessage soapMessage,InputStream schematron,String phase) throws XmlException {
    	ArrayList<MessageFailureV3> soapFailures = new ArrayList<MessageFailureV3>();
    	// schematron validation                                                                                                                                               
        SoapValidator soapValidator = new SoapValidator();
        XmlObject soapXml = soapMessage.getMessageDoc();
 		soapFailures.addAll(soapValidator.validate(soapXml,schematron, phase));
        return new gov.nist.healthcare.core.validation.soap.SoapValidationResult(soapMessage,soapFailures);
    }
    
    

    public static boolean validateXMLSchema(String xsdPath, String xmlPath){
     
    	try {
    		SchemaFactory factory = 
    				SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    		Schema schema = factory.newSchema(new File(xsdPath));
    		Validator validator = schema.newValidator();
    		validator.validate(new StreamSource(new File(xmlPath)));
    	} catch (IOException | SAXException e) {
    		System.out.println("Exception: "+e.getMessage());
    		return false;
    	}
    	return true;
    }
    

}

