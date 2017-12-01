package gov.nist.hit.core.hl7v2.service;

import java.io.File;
import java.util.List;
import java.util.Set;

import gov.nist.hit.core.domain.UploadedProfileModel;

public interface PackagingHandler {

	public List<UploadedProfileModel> getUploadedProfiles(String xml);

	public String removeUnusedAndDuplicateMessages(String content, Set<UploadedProfileModel> presentMessages);

	public File changeProfileId(File file) throws Exception;

	public File changeConstraintId(File file) throws Exception;

	public File changeVsId(File file) throws Exception;

	public File zip(List<File> files, String filename) throws Exception;

}
