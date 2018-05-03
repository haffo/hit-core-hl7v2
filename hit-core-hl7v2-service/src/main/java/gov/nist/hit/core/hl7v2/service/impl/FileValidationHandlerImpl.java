package gov.nist.hit.core.hl7v2.service.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
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
import gov.nist.hit.hl7.profile.validation.domain.ProfileValidationReport;
import gov.nist.hit.hl7.profile.validation.service.impl.ValidationServiceImpl;

@Service
public class FileValidationHandlerImpl implements FileValidationHandler {

	@Autowired
	private ResourceLoader resourceLoader;

	
	
	
	
	@Override
	public List<XMLError> validateProfile(String contentTxt, InputStream contentIS) throws Exception {
		// Check if not a Constraint file or Value Set file
		List<XMLError> errors = new ArrayList<XMLError>();
		if (contentTxt.contains("<ConformanceContext")) {
			XMLError error = new XMLError(0, 0, "File is a Constraint file.");
			errors.add(error);
		} else if (contentTxt.contains("<ValueSetLibrary")) {
			XMLError error = new XMLError(0, 0, "File is a Value Set file.");
			errors.add(error);
		}

		return errors;

	}

	@Override
	public List<XMLError> validateConstraints(String contentTxt, InputStream contentIS) throws Exception {
		// Check if not a Profile file or Value Set file
		List<XMLError> errors = new ArrayList<XMLError>();
		if (contentTxt.contains("<ConformanceProfile")) {
			XMLError error = new XMLError(0, 0, "File is a Profile file.");
			errors.add(error);
		} else if (contentTxt.contains("<ValueSetLibrary")) {
			XMLError error = new XMLError(0, 0, "File is a Value Set file.");
			errors.add(error);
		}
		return errors;
	}

	@Override
	public List<XMLError> validateVocabulary(String contentTxt, InputStream contentIS) throws Exception {
		// Check if not a Profile file or Constraint file
		List<XMLError> errors = new ArrayList<XMLError>();
		if (contentTxt.contains("<ConformanceProfile")) {
			XMLError error = new XMLError(0, 0, "File is a Profile file.");
			errors.add(error);
		} else if (contentTxt.contains("<ConformanceContext")) {
			XMLError error = new XMLError(0, 0, "File is a Constraint file.");
			errors.add(error);
		}
		return errors;
	}

	@Override
	public Map<String, List<XMLError>> unbundleAndValidate(String dir) throws Exception {
		XMLResourcesValidator v = XMLResourcesValidator.createValidatorFromClasspath("/xsd");
		Map<String, List<XMLError>> errorsMap = new HashMap<String, List<XMLError>>();
		String rootPath = findFileDirectory(dir, "Profile.xml") + "/";

		// Profile
		Resource profile = resourceLoader.getResource("Profile.xml", rootPath);
		errorsMap.put("profileErrors", v.validateProfile(profile.getInputStream()));

		// Constraints
		Resource constraints = resourceLoader.getResource("Constraints.xml", rootPath);
		errorsMap.put("constraintsErrors", v.validateConstraints(constraints.getInputStream()));

		// VS
		Resource vs = resourceLoader.getResource("ValueSets.xml", rootPath);
		errorsMap.put("vsErrors", v.validateVocabulary(vs.getInputStream()));

		return errorsMap;
	}
	
	@Override
	public ProfileValidationReport getHTMLValidatioReport(String dir) throws Exception {		
		ValidationServiceImpl vsi = new ValidationServiceImpl();
		
		String rootPath = findFileDirectory(dir, "Profile.xml") + "/";

		// Profile
		Resource profile = resourceLoader.getResource("Profile.xml", rootPath);
		
		List<String> fileTypeErrors = new ArrayList<String>();
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		org.apache.commons.io.IOUtils.copy(profile.getInputStream(), baos);
		byte[] bytes = baos.toByteArray();
		String content = IOUtils.toString(new ByteArrayInputStream(bytes));	
		if (content.contains("<ConformanceContext")) {
			fileTypeErrors.add("Profile file provided is a Constraint file.");
		} else if (content.contains("<ValueSetLibrary")) {
			fileTypeErrors.add("Profile file provided is a Value Set file.");
		}
		
		// Constraints
		Resource constraints = resourceLoader.getResource("Constraints.xml", rootPath);
		baos = new ByteArrayOutputStream();
		org.apache.commons.io.IOUtils.copy(constraints.getInputStream(), baos);
		bytes = baos.toByteArray();
		content = IOUtils.toString(new ByteArrayInputStream(bytes));	
		if (content.contains("<ConformanceProfile")) {
			fileTypeErrors.add("Constraints file provided is a Profile file.");
		} else if (content.contains("<ValueSetLibrary")) {
			fileTypeErrors.add("Constraints file provided is a Value Set file.");
		}
		
		
		// VS
		Resource vs = resourceLoader.getResource("ValueSets.xml", rootPath);
		baos = new ByteArrayOutputStream();
		org.apache.commons.io.IOUtils.copy(vs.getInputStream(), baos);
		bytes = baos.toByteArray();
		content = IOUtils.toString(new ByteArrayInputStream(bytes));	
		if (content.contains("<ConformanceProfile")) {
			fileTypeErrors.add("Constraints file provided is a Profile file.");
		} else if (content.contains("<ConformanceContext")) {
			fileTypeErrors.add("Profile file provided is a Constraint file.");
		}
		
		if (fileTypeErrors.size() >0) {
			throw new InvalidFileTypeException(fileTypeErrors);
		}
		
		
		ProfileValidationReport report = null;
		if (profile != null && profile.exists() && constraints != null && constraints.exists() && vs != null &&  vs.exists()) {
			report = vsi.validationXMLs(profile.getInputStream(),constraints.getInputStream(),vs.getInputStream());
		}

		return report;
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

	public class InvalidFileTypeException extends Exception {

		private List<String>errors = new ArrayList<String>();
		
		public InvalidFileTypeException(List<String> errors_) {
			super();			
			errors = errors_;
		}

		public List<String> getErrors() {
			return errors;
		}

		public void setErrors(List<String> errors) {
			this.errors = errors;
		}

		
		
	}
	
}



