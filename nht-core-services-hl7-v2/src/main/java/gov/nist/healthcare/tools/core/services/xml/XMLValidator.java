package gov.nist.healthcare.tools.core.services.xml;

import gov.nist.healthcare.core.validation.message.v3.MessageFailureV3;
import gov.nist.healthcare.core.validation.soap.SoapMessage;
import gov.nist.healthcare.core.validation.soap.SoapValidationResult;
import gov.nist.healthcare.validation.AssertionTypeV3Constants;
import gov.nist.healthcare.validation.ErrorSeverityConstants;

import java.io.IOException;
import java.util.ArrayList;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;

import org.apache.commons.io.IOUtils;
import org.xml.sax.SAXException;

public class XMLValidator {
	

//    public static void main(String[] args) {
//        System.out.println("Result of validating soap_msg2.xml against soap12_cdc-iisb-2011.xsd: "
//             + validateXMLSchema("/Users/indovina/XMLvalidator/soap12_cdc-iisb-2011.xsd", "/Users/indovina/XMLValidator/soap_msg2.xml"));
//    }
	
	public XMLValidator() {

	}

    public SoapValidationResult validate(SoapMessage soapMessage,Schema schema ) {

    	ArrayList<MessageFailureV3> soapFailures = new ArrayList<MessageFailureV3>();

        try {
           // validate xml (soap msg)
            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(IOUtils.toInputStream(soapMessage.getMessageAsString(), "UTF-8")));
        } catch (IOException e) {
            System.out.println("Exception: "+e.getMessage());
            MessageFailureV3 mf = new MessageFailureV3();
            mf.setFailureType(AssertionTypeV3Constants.SOAP);
            mf.setFailureSeverity(ErrorSeverityConstants.FATAL);
            mf.setDescription(e.getMessage());
            soapFailures.add(mf);
        } catch (SAXException e) {
            System.out.println("Exception: "+e.getMessage());
            MessageFailureV3 mf = new MessageFailureV3();
            mf.setFailureType(AssertionTypeV3Constants.SOAP);
            mf.setFailureSeverity(ErrorSeverityConstants.NORMAL);
            mf.setDescription(e.getMessage());
            soapFailures.add(mf);	
        }
       
        return new SoapValidationResult(soapMessage,soapFailures);
        
        //return true;
    }

}

