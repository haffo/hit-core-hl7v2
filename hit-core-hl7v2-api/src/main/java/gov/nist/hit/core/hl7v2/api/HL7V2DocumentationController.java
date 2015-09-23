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

import gov.nist.hit.core.hl7v2.domain.HL7V2TestContext;
import gov.nist.hit.core.hl7v2.repo.HL7V2TestContextRepository;
import gov.nist.hit.core.repo.TestCaseDocumentationRepository;
import gov.nist.hit.core.service.exception.DownloadDocumentException;

import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Harold Affo (NIST)
 * 
 */
@RequestMapping("/hl7v2/documentation")
@RestController
public class HL7V2DocumentationController {

  static final Logger logger = LoggerFactory.getLogger(HL7V2DocumentationController.class);

  @Autowired
  private TestCaseDocumentationRepository testCaseDocumentationRepository;

  @Autowired
  protected HL7V2TestContextRepository testContextRepository;

  @RequestMapping(value = "/message", method = RequestMethod.POST,
      consumes = "application/x-www-form-urlencoded; charset=UTF-8")
  public String downloadMessage(@RequestParam("targetId") Long targetId,
      @RequestParam("targetType") String targetType,
      @RequestParam("targetTitle") String targetTitle, HttpServletRequest request,
      HttpServletResponse response) {
    try {
      logger.info("Downloading message of element with id " + targetId);
      InputStream content = null;
      HL7V2TestContext testContext = testContextRepository.findOne(targetId);
      String message = testContext.getMessage().getContent();
      content = IOUtils.toInputStream(message, "UTF-8");
      response.setContentType("text/plain");
      targetTitle = targetTitle + "-" + "ExampleMessage.txt";
      targetTitle = targetTitle.replaceAll(" ", "-");
      response.setHeader("Content-disposition", "attachment;filename=" + targetTitle);
      FileCopyUtils.copy(content, response.getOutputStream());
    } catch (Exception e) {
      logger.debug(e.getMessage(), e);
      throw new DownloadDocumentException("Failed to download the message");
    }
    return null;
  }

  @RequestMapping(value = "/profile", method = RequestMethod.POST,
      consumes = "application/x-www-form-urlencoded; charset=UTF-8")
  public String downloadProfile(@RequestParam("targetId") Long targetId,
      @RequestParam("targetType") String targetType,
      @RequestParam("targetTitle") String targetTitle, HttpServletRequest request,
      HttpServletResponse response) {
    try {
      logger.info("Downloading Profile of element with id " + targetId);
      InputStream content = null;
      HL7V2TestContext testContext = testContextRepository.findOne(targetId);
      String profile = testContext.getConformanceProfile().getJson();
      content = IOUtils.toInputStream(profile, "UTF-8");
      response.setContentType("text/plain");
      targetTitle = targetTitle + "-" + "Profile.json";
      targetTitle = targetTitle.replaceAll(" ", "-");
      response.setHeader("Content-disposition", "attachment;filename=" + targetTitle);
      FileCopyUtils.copy(content, response.getOutputStream());
    } catch (Exception e) {
      logger.debug(e.getMessage(), e);
      throw new DownloadDocumentException("Failed to download the conformance profile");
    }
    return null;
  }

  @RequestMapping(value = "/valuesetlib", method = RequestMethod.POST,
      consumes = "application/x-www-form-urlencoded; charset=UTF-8")
  public String downloadValueSetlib(@RequestParam("targetId") Long targetId,
      @RequestParam("targetType") String targetType,
      @RequestParam("targetTitle") String targetTitle, HttpServletRequest request,
      HttpServletResponse response) {
    try {
      logger.info("Downloading ValueSetLibrary of element with id " + targetId);
      InputStream content = null;
      HL7V2TestContext testContext = testContextRepository.findOne(targetId);
      String profile = testContext.getVocabularyLibrary().getJson();
      content = IOUtils.toInputStream(profile, "UTF-8");
      response.setContentType("text/plain");
      targetTitle = targetTitle + "-" + "ValueSetLibrary.json";
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
