package gov.nist.hit.core.hl7v2.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import gov.nist.healthcare.resources.domain.XMLError;
import gov.nist.hit.hl7.profile.validation.domain.ProfileValidationReport;

public interface FileValidationHandler {

	public List<XMLError> validateProfile(String contentTxt,InputStream contentIS) throws Exception;
	public List<XMLError> validateConstraints(String contentTxt,InputStream contentIS) throws Exception;
	public List<XMLError> validateVocabulary(String contentTxt,InputStream contentIS) throws Exception;
	
	public Map<String, List<XMLError>> unbundleAndValidate(String dir) throws Exception;
	
	public ProfileValidationReport getHTMLValidatioReport(String dir) throws Exception;
	
	
}
