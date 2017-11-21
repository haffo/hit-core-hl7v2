package gov.nist.hit.core.hl7v2.service.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import gov.nist.healthcare.resources.domain.XMLError;
import gov.nist.healthcare.resources.xds.XMLResourcesValidator;
import gov.nist.hit.core.hl7v2.service.FileValidationHandler;
import gov.nist.hit.core.service.ResourceLoader;

@Service
public class FileValidationHandlerImpl implements FileValidationHandler{

	@Autowired
	private ResourceLoader resourceLoader;
	
	
	public List<XMLError> validateProfile(String contentTxt,InputStream contentIS) throws Exception{
		//Check if not a Constraint file or Value Set file
		List<XMLError> errors = new ArrayList<XMLError>();
		if (contentTxt.contains("<ConformanceContext")){
			XMLError error = new XMLError(0, 0, "File is a Constraint file.");
			errors.add(error);
		}else if (contentTxt.contains("<ValueSetLibrary")){
			XMLError error = new XMLError(0, 0, "File is a Value Set file.");
			errors.add(error);
		}else{
			XMLResourcesValidator v = XMLResourcesValidator.createValidatorFromClasspath("/xsd");
			errors = v.validateProfile(contentIS);
		}

		return errors;
		
	}
	public List<XMLError> validateConstraints(String contentTxt,InputStream contentIS) throws Exception{
		//Check if not a Profile file or Value Set file
		List<XMLError> errors = new ArrayList<XMLError>();
		if (contentTxt.contains("<ConformanceProfile")){
			XMLError error = new XMLError(0, 0, "File is a Profile file.");
			errors.add(error);
		}else if (contentTxt.contains("<ValueSetLibrary")){
			XMLError error = new XMLError(0, 0, "File is a Value Set file.");
			errors.add(error);
		}else{
			XMLResourcesValidator v = XMLResourcesValidator.createValidatorFromClasspath("/xsd");
			errors = v.validateConstraints(contentIS);
		}
		return errors;
	}
	
	public List<XMLError> validateVocabulary(String contentTxt,InputStream contentIS) throws Exception{
		//Check if not a Profile file or Constraint file
		List<XMLError> errors = new ArrayList<XMLError>();
		if (contentTxt.contains("<ConformanceProfile")){
			XMLError error = new XMLError(0, 0, "File is a Profile file.");
			errors.add(error);
		}else if (contentTxt.contains("<ConformanceContext")){
			XMLError error = new XMLError(0, 0, "File is a Constraint file.");
			errors.add(error);
		}else{
			XMLResourcesValidator v = XMLResourcesValidator.createValidatorFromClasspath("/xsd");
			errors = v.validateVocabulary(contentIS);
		}
		return errors;
	}
	
	

	public Map<String, List<XMLError>> unbundleAndValidate(String dir) throws Exception{
		XMLResourcesValidator v = XMLResourcesValidator.createValidatorFromClasspath("/xsd");
		Map<String, List<XMLError>> errorsMap = new HashMap<String, List<XMLError>>();
		resourceLoader.setDirectory(findFileDirectory(dir,"Profile.xml")+"/");
		
		// Profile
		Resource profile = resourceLoader.getResource("Profile.xml");
		errorsMap.put("profileErrors",v.validateProfile(profile.getInputStream()));
		
		// Constraints
		Resource constraints = resourceLoader.getResource("Constraints.xml");
		errorsMap.put("constraintsErrors",v.validateConstraints(constraints.getInputStream()));
		
		// VS
		Resource vs = resourceLoader.getResource("ValueSets.xml");
		errorsMap.put("vsErrors",v.validateVocabulary(vs.getInputStream()));
		
		return errorsMap;
	}
	
	// finds folder where file is found (first occurence)
	private String findFileDirectory(String dir, String fileName) {
		Collection files = FileUtils.listFiles(new File(dir), null, true);
		for (Iterator iterator = files.iterator(); iterator.hasNext();) {
			File file = (File) iterator.next();
			if (file.getName().equals(fileName)) {
				return file.getParentFile().getAbsolutePath();
			}
		}
		return null;
	}
	
	

	
	
}
