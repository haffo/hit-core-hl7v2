/**
 * This software was developed at the National Institute of Standards and Technology by employees of
 * the Federal Government in the course of their official duties. Pursuant to title 17 Section 105
 * of the United States Code this software is not subject to copyright protection and is in the
 * public domain. This is an experimental system. NIST assumes no responsibility whatsoever for its
 * use by other parties, and makes no guarantees, expressed or implied, about its quality,
 * reliability, or any other characteristic. We would appreciate acknowledgement if the software is
 * used. This software can be redistributed and/or modified freely provided that any derivative
 * works bear some notice that they are derived from it, and any modified versions bear some notice
 * that they have been modified.
 */

package gov.nist.hit.core.hl7v2.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import gov.nist.hit.core.hl7v2.domain.HL7V2TestContext;
import gov.nist.hit.core.hl7v2.repo.HL7V2TestContextRepository;
import gov.nist.hit.core.service.Streamer;
import gov.nist.hit.core.service.ZipGenerator;
import gov.nist.hit.core.service.exception.DownloadDocumentException;
import io.swagger.annotations.ApiParam;

/**
 * @author Harold Affo (NIST)
 * 
 */
@RequestMapping("/hl7v2/documentation")
@RestController
public class HL7V2DocumentationController {

  static final Logger logger = LoggerFactory.getLogger(HL7V2DocumentationController.class);


  @Autowired
  protected HL7V2TestContextRepository testContextRepository;

  @Autowired
  private ZipGenerator zipGenerator;


  @Autowired
  private Streamer streamer;

  @RequestMapping(value = "/message.txt", method = RequestMethod.POST, produces = "text/plain",
      consumes = "application/x-www-form-urlencoded; charset=UTF-8")
  public String downloadExampeMessage(
      @ApiParam(value = "the id of the example message",
          required = true) @RequestParam("targetId") Long targetId,
      @ApiParam(value = "the title of the downloaded example message",
          required = true) @RequestParam("targetTitle") String targetTitle,
      HttpServletRequest request, HttpServletResponse response) {
    try {
      logger.info("Downloading message of element with id " + targetId);
      InputStream content = null;
      HL7V2TestContext testContext = testContextRepository.findOne(targetId);
      String message = testContext.getMessage().getContent();
      content = IOUtils.toInputStream(message, "UTF-8");
      response.setContentType("text/plain");
      targetTitle = targetTitle + "-" + "Message.txt";
      targetTitle = targetTitle.replaceAll(" ", "-");
      response.setHeader("Content-disposition", "attachment;filename=" + targetTitle);
      FileCopyUtils.copy(content, response.getOutputStream());
    } catch (Exception e) {
      logger.debug(e.getMessage(), e);
      throw new DownloadDocumentException("Failed to download the message");
    }
    return null;
  }

  @RequestMapping(value = "/profile.xml", method = RequestMethod.POST, produces = "application/xml",
      consumes = "application/x-www-form-urlencoded; charset=UTF-8")
  public String downloadProfile(
      @ApiParam(value = "the id of the conformance profile",
          required = true) @RequestParam("targetId") Long targetId,
      @ApiParam(value = "the title of the downloaded conformance profile",
          required = true) @RequestParam("targetTitle") String targetTitle,
      HttpServletRequest request, HttpServletResponse response) {
    try {
      logger.info("Downloading Profile of element with id " + targetId);
      InputStream content = null;
      HL7V2TestContext testContext = testContextRepository.findOne(targetId);
      String profile = testContext.getConformanceProfile().getXml();
      content = IOUtils.toInputStream(profile, "UTF-8");
      response.setContentType("application/xml");
      targetTitle = targetTitle + "-" + "Profile.xml";
      targetTitle = targetTitle.replaceAll(" ", "-");
      response.setHeader("Content-disposition", "attachment;filename=" + targetTitle);
      FileCopyUtils.copy(content, response.getOutputStream());
    } catch (Exception e) {
      logger.debug(e.getMessage(), e);
      throw new DownloadDocumentException("Failed to download the conformance profile");
    }
    return null;
  }

  @RequestMapping(value = "/constraints.zip", method = RequestMethod.POST,
      produces = "application/zip", consumes = "application/x-www-form-urlencoded; charset=UTF-8")
  public String downloadConstraint(
      @ApiParam(value = "the id of the constraint",
          required = true) @RequestParam("targetId") Long targetId,
      @ApiParam(value = "the title of the downloaded constraint",
          required = true) @RequestParam("targetTitle") String targetTitle,
      HttpServletRequest request, HttpServletResponse response) {
    try {
      logger.info("Downloading constraint of element with id " + targetId);
      HL7V2TestContext testContext = testContextRepository.findOne(targetId);
      InputStream steram = null;
      targetTitle = targetTitle.replaceAll(" ", "-");
      if (testContext != null && (testContext.getAddditionalConstraints() != null
          || testContext.getConstraints() != null)) {
        steram = createConstraintsFile(targetTitle, testContext);
      }
      response.setContentType("application/zip");
      response.setHeader("Content-disposition",
          "attachment;filename=" + targetTitle + "-Constraints.zip");
      streamer.stream(response.getOutputStream(), steram);
    } catch (Exception e) {
      logger.debug(e.getMessage(), e);
      throw new DownloadDocumentException("Failed to download the conformance profile");
    }
    return null;
  }


  public InputStream createConstraintsFile(String targetTitle, HL7V2TestContext testContext)
      throws Exception {
    Path path = Files.createTempDirectory(null);
    File rootFolder = path.toFile();
    if (!rootFolder.exists()) {
      rootFolder.mkdir();
    }
    String folderToZip = rootFolder.getAbsolutePath() + File.separator + "ToZip";
    File folder = new File(folderToZip + File.separator + targetTitle + File.separator);
    if (testContext != null && (testContext.getAddditionalConstraints() != null
        || testContext.getConstraints() != null)) {
      if (!folder.exists()) {
        folder.mkdirs();
      }
      int i = 1;
      if (testContext.getAddditionalConstraints() != null) {
        String filename = folder.getAbsolutePath() + File.separator + "Constraints" + i + ".xml";
        File file = new File(filename);
        if (!file.exists()) {
          file.createNewFile();
        }
        FileUtils.copyInputStreamToFile(
            IOUtils.toInputStream(testContext.getAddditionalConstraints().getXml()), file);
        i++;
      }

      if (testContext.getConstraints() != null) {
        String filename = folder.getAbsolutePath() + File.separator + "Constraints" + i + ".xml";
        File file = new File(filename);
        if (!file.exists()) {
          file.createNewFile();
        }
        FileUtils.copyInputStreamToFile(
            IOUtils.toInputStream(testContext.getConstraints().getXml()), file);
      }
      String zipFilename = rootFolder + File.separator + targetTitle + "Constraints.zip";
      zipGenerator.zip(zipFilename, folderToZip);
      FileInputStream io = new FileInputStream(new File(zipFilename));
      return io;
    }
    return null;
  }


  @RequestMapping(value = "/valueset.xml", method = RequestMethod.POST,
      produces = "application/xml", consumes = "application/x-www-form-urlencoded; charset=UTF-8")
  public String downloadValueSetlib(
      @ApiParam(value = "the id of the value set library",
          required = true) @RequestParam("targetId") Long targetId,
      @ApiParam(value = "the title of the downloaded value set library",
          required = true) @RequestParam("targetTitle") String targetTitle,
      HttpServletRequest request, HttpServletResponse response) {
    try {
      logger.info("Downloading ValueSetLibrary of element with id " + targetId);
      InputStream content = null;
      HL7V2TestContext testContext = testContextRepository.findOne(targetId);
      String profile = testContext.getVocabularyLibrary().getXml();
      content = IOUtils.toInputStream(profile, "UTF-8");
      response.setContentType("application/xml");
      targetTitle = targetTitle + "-" + "Valuesets.xml";
      targetTitle = targetTitle.replaceAll(" ", "-");
      response.setHeader("Content-disposition", "attachment;filename=" + targetTitle);
      FileCopyUtils.copy(content, response.getOutputStream());
    } catch (Exception e) {
      logger.debug(e.getMessage(), e);
      throw new DownloadDocumentException("Failed to download the ValueSetLibrary");
    }
    return null;
  }



}
