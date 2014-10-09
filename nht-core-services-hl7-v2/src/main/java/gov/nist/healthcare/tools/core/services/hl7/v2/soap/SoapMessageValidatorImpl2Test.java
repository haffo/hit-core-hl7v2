package gov.nist.healthcare.tools.core.services.hl7.v2.soap;

import java.io.*;

import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.IOUtils;

public class SoapMessageValidatorImpl2Test {

	
	public static void main(String[] args) {
		
		SoapMessageValidatorImpl2 tmp = new SoapMessageValidatorImpl2();
		File f = new File("/Users/indovina/XMLValidator/soap_msg2.xml");
		String soapMsg=null;
		
		try {
			soapMsg=IOUtils.toString(new FileReader(f));
		}
		catch (Exception e){//Catch exception if any                                                                                                                 
            System.err.println("Error: " + e.getMessage());
		}
				
		System.out.println("soapMsg= " + soapMsg);
		
		System.out.println("Result of validating soap_msg2.xml against soap12_cdc-iisb-2011.xsd: "
				+ tmp.validate(soapMsg, "Basic SOAP 1.2 Validation").toString());
	}	

	
}

