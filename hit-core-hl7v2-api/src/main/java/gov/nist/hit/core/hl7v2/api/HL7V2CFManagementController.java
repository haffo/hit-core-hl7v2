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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import gov.nist.healthcare.resources.domain.XMLError;
import gov.nist.hit.core.api.ContextFreeController;
import gov.nist.hit.core.domain.CFTestPlan;
import gov.nist.hit.core.domain.CFTestStep;
import gov.nist.hit.core.domain.GVTSaveInstance;
import gov.nist.hit.core.domain.Message;
import gov.nist.hit.core.domain.ResourceUploadResult;
import gov.nist.hit.core.domain.TestCaseWrapper;
import gov.nist.hit.core.domain.TestContext;
import gov.nist.hit.core.domain.TestScope;
import gov.nist.hit.core.domain.UploadStatus;
import gov.nist.hit.core.domain.UploadedProfileModel;
import gov.nist.hit.core.hl7v2.service.FileValidationHandler;
import gov.nist.hit.core.hl7v2.service.PackagingHandler;
import gov.nist.hit.core.hl7v2.service.impl.HL7V2ProfileParserImpl;
import gov.nist.hit.core.repo.ConstraintsRepository;
import gov.nist.hit.core.repo.IntegrationProfileRepository;
import gov.nist.hit.core.repo.UserTestCaseGroupRepository;
import gov.nist.hit.core.repo.VocabularyLibraryRepository;
import gov.nist.hit.core.service.AppInfoService;
import gov.nist.hit.core.service.BundleHandler;
import gov.nist.hit.core.service.CFTestPlanService;
import gov.nist.hit.core.service.ProfileParser;
import gov.nist.hit.core.service.UserIdService;
import gov.nist.hit.core.service.UserService;
import gov.nist.hit.core.service.exception.MessageUploadException;
import gov.nist.hit.core.service.exception.NoUserFoundException;
import gov.nist.hit.core.service.exception.NotValidToken;
import io.swagger.annotations.Api;

/**
 * @author Nicolas Crouzier (NIST)
 * @author Harold Affo (NIST)
 * 
 */

@RequestMapping("/cf/hl7v2/management")
@Api(hidden = true)
@Controller
public class HL7V2CFManagementController {

  static final Logger logger = LoggerFactory.getLogger(HL7V2CFManagementController.class);

  private static final String CF_UPLOAD_DIR = ContextFreeController.CF_UPLOAD_DIR;

  ProfileParser parser = new HL7V2ProfileParserImpl();

  @Autowired
  private CFTestPlanService tpService;

  @Autowired
  private UserTestCaseGroupRepository testCaseGroupRepository;

  @Autowired
  private IntegrationProfileRepository ipRepository;

  @Autowired
  private ConstraintsRepository csRepository;

  @Autowired
  private VocabularyLibraryRepository vsRepository;

  @Autowired
  private UserIdService userIdService;

  @Autowired
  private BundleHandler bundleHandler;

  @Autowired
  private PackagingHandler packagingHandler;

  @Autowired
  private FileValidationHandler fileValidationHandler;

  @Autowired
  private UserService userService;

  @Autowired
  private CFTestPlanService testPlanService;


  @Autowired
  private AppInfoService appInfoService;


  private void checkManagementSupport() throws Exception {
    if (!appInfoService.get().isCfManagementSupported()) {
      throw new Exception("This operation is not supported by this tool");
    }
  }



  /**
   * Upload a single XML profile file and may returns errors
   * 
   * @param request Client request
   * @param part Profile XML file
   * @param token Token used for saving file
   * @param p Principal
   * @return A list of profiles or a list of errors
   * @throws Exception
   */
  @PreAuthorize("hasRole('tester')")
  @RequestMapping(value = "/uploadProfiles", method = RequestMethod.POST,
      consumes = {"multipart/form-data"})
  @ResponseBody
  public Map<String, Object> uploadProfile(ServletRequest request,
      @RequestPart("file") MultipartFile part, @RequestParam("token") String token, Principal p)
      throws Exception {
    checkManagementSupport();
    Map<String, Object> resultMap = new HashMap<String, Object>();
    try {
      if (!part.getContentType().equalsIgnoreCase("text/xml"))
        throw new MessageUploadException(
            "Unsupported content type. Supported content types are: '.xml' ");

      String userName = userIdService.getCurrentUserName(p);
      if (userName == null)
        throw new NoUserFoundException("User could not be found");

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      org.apache.commons.io.IOUtils.copy(part.getInputStream(), baos);
      byte[] bytes = baos.toByteArray();

      String content = IOUtils.toString(new ByteArrayInputStream(bytes));
      List<XMLError> errors =
          fileValidationHandler.validateProfile(content, new ByteArrayInputStream(bytes));

      if (errors.size() > 0) {
        resultMap.put("success", false);
        resultMap.put("errors", errors);
        logger.info("Uploaded profile file with errors " + part.getName());
      } else {
        List<UploadedProfileModel> list = packagingHandler.getUploadedProfiles(content);
        resultMap.put("success", true);
        resultMap.put("profiles", list);

        File profileFile = new File(CF_UPLOAD_DIR + "/" + userName + "/" + token + "/Profile.xml");
        FileUtils.writeStringToFile(profileFile, content);
        logger.info("Uploaded valid profile file " + part.getName());
      }

      return resultMap;
    } catch (NoUserFoundException e) {
      resultMap.put("success", false);
      resultMap.put("message", "An error occured. The tool could not upload the profile file sent");
      resultMap.put("debugError", ExceptionUtils.getMessage(e));
      return resultMap;
    } catch (MessageUploadException e) {
      resultMap.put("success", false);
      resultMap.put("message", "An error occured. The tool could not upload the profile file sent");
      resultMap.put("debugError", ExceptionUtils.getMessage(e));
      return resultMap;
    } catch (Exception e) {
      resultMap.put("success", false);
      resultMap.put("message", "An error occured. The tool could not upload the profile file sent");
      resultMap.put("debugError", ExceptionUtils.getStackTrace(e));
      return resultMap;
    }
  }

  /**
   * Upload a single XML value set file and may returns errors
   * 
   * @param request Client request
   * @param part Value Set XML file
   * @param token Token used for saving file
   * @param p Principal
   * @return May return a list of errors
   * @throws MessageUploadException
   */
  @PreAuthorize("hasRole('tester')")
  @RequestMapping(value = "/uploadValueSets", method = RequestMethod.POST,
      consumes = {"multipart/form-data"})
  @ResponseBody
  public Map<String, Object> uploadVS(ServletRequest request,
      @RequestPart("file") MultipartFile part, @RequestParam("token") String token, Principal p)
      throws MessageUploadException {
    checkManagementSupport();
    Map<String, Object> resultMap = new HashMap<String, Object>();
    try {
      if (!part.getContentType().equalsIgnoreCase("text/xml"))
        throw new MessageUploadException(
            "Unsupported content type. Supported content types are: '.xml' ");

      String userName = userIdService.getCurrentUserName(p);
      if (userName == null)
        throw new NoUserFoundException("User could not be found");

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      org.apache.commons.io.IOUtils.copy(part.getInputStream(), baos);
      byte[] bytes = baos.toByteArray();
      String content = IOUtils.toString(new ByteArrayInputStream(bytes));
      List<XMLError> errors =
          fileValidationHandler.validateVocabulary(content, new ByteArrayInputStream(bytes));

      if (errors.size() > 0) {
        resultMap.put("success", false);
        resultMap.put("errors", errors);
        logger.info("Uploaded value set file with errors " + part.getName());
      } else {
        resultMap.put("success", true);

        File vsFile = new File(CF_UPLOAD_DIR + "/" + userName + "/" + token + "/ValueSets.xml");
        FileUtils.writeStringToFile(vsFile, content);
        logger.info("Uploaded value set file " + part.getName());
      }
      return resultMap;
    } catch (NoUserFoundException e) {
      resultMap.put("success", false);
      resultMap.put("message",
          "An error occured. The tool could not upload the valueset file sent");
      resultMap.put("debugError", ExceptionUtils.getMessage(e));
      return resultMap;
    } catch (MessageUploadException e) {
      resultMap.put("success", false);
      resultMap.put("message",
          "An error occured. The tool could not upload the valueset file sent");
      resultMap.put("debugError", ExceptionUtils.getMessage(e));
      return resultMap;
    } catch (Exception e) {
      resultMap.put("success", false);
      resultMap.put("message",
          "An error occured. The tool could not upload the valueset file sent");
      resultMap.put("debugError", ExceptionUtils.getStackTrace(e));
      return resultMap;
    }
  }

  /**
   * Upload a single XML constraints file and may returns errors
   * 
   * @param request Client request
   * @param part Constraints XML file
   * @param token Token used for saving file
   * @param p Principal
   * @return May return a list of errors
   * @throws MessageUploadException
   */
  @PreAuthorize("hasRole('tester')")
  @RequestMapping(value = "/uploadConstraints", method = RequestMethod.POST,
      consumes = {"multipart/form-data"})
  @ResponseBody
  public Map<String, Object> uploadContraints(ServletRequest request,
      @RequestPart("file") MultipartFile part, @RequestParam("token") String token, Principal p)
      throws MessageUploadException {
    checkManagementSupport();

    Map<String, Object> resultMap = new HashMap<String, Object>();
    try {
      if (!part.getContentType().equalsIgnoreCase("text/xml"))
        throw new MessageUploadException(
            "Unsupported content type. Supported content types are: '.xml' ");

      String userName = userIdService.getCurrentUserName(p);
      if (userName == null)
        throw new NoUserFoundException("User could not be found");

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      org.apache.commons.io.IOUtils.copy(part.getInputStream(), baos);
      byte[] bytes = baos.toByteArray();
      String content = IOUtils.toString(new ByteArrayInputStream(bytes));
      List<XMLError> errors =
          fileValidationHandler.validateConstraints(content, new ByteArrayInputStream(bytes));

      if (errors.size() > 0) {
        resultMap.put("success", false);
        resultMap.put("errors", errors);
        logger.info("Uploaded constraints file with errors " + part.getName());
      } else {
        resultMap.put("success", true);

        File constraintFile =
            new File(CF_UPLOAD_DIR + "/" + userName + "/" + token + "/Constraints.xml");
        FileUtils.writeStringToFile(constraintFile, content);
        logger.info("Uploaded constraints file " + part.getName());
      }
      return resultMap;
    } catch (NoUserFoundException e) {
      resultMap.put("success", false);
      resultMap.put("message",
          "An error occured. The tool could not upload the constraints file sent");
      resultMap.put("debugError", ExceptionUtils.getMessage(e));
      return resultMap;
    } catch (MessageUploadException e) {
      resultMap.put("success", false);
      resultMap.put("message",
          "An error occured. The tool could not upload the constraints file sent");
      resultMap.put("debugError", ExceptionUtils.getMessage(e));
      return resultMap;
    } catch (Exception e) {
      resultMap.put("success", false);
      resultMap.put("message",
          "An error occured. The tool could not upload the constraints file sent");
      resultMap.put("debugError", ExceptionUtils.getStackTrace(e));
      return resultMap;
    }
  }

  /**
   * Uploads zip file and stores it in a temporary directory
   * 
   * @param request Client request
   * @param part Zip file
   * @param p Principal
   * @return a token or some errors
   * @throws MessageUploadException
   */
  @PreAuthorize("hasRole('tester')")
  @RequestMapping(value = "/uploadZip", method = RequestMethod.POST,
      consumes = {"multipart/form-data"})
  @ResponseBody
  public Map<String, Object> uploadZip(ServletRequest request,
      @RequestPart("file") MultipartFile part, Principal p) throws MessageUploadException {
    checkManagementSupport();

    Map<String, Object> resultMap = new HashMap<String, Object>();
    try {
      if (!part.getContentType().equalsIgnoreCase("application/zip"))
        throw new MessageUploadException(
            "Unsupported content type. Supported content types are: '.zip' ");

      String userName = userIdService.getCurrentUserName(p);
      if (userName == null)
        throw new NoUserFoundException("User could not be found");

      String token = UUID.randomUUID().toString();
      String directory =
          bundleHandler.unzip(part.getBytes(), CF_UPLOAD_DIR + "/" + userName + "/" + token);
      Map<String, List<XMLError>> errorMap = fileValidationHandler.unbundleAndValidate(directory);

      if (errorMap.get("profileErrors").size() > 0 || errorMap.get("constraintsErrors").size() > 0
          || errorMap.get("vsErrors").size() > 0) {
        resultMap.put("success", false);
        resultMap.put("profileErrors", errorMap.get("profileErrors"));
        resultMap.put("constraintsErrors", errorMap.get("constraintsErrors"));
        resultMap.put("vsErrors", errorMap.get("vsErrors"));
        FileUtils.deleteDirectory(new File(directory));
        logger.info("Uploaded profile file with errors " + part.getName());
      } else {
        resultMap.put("success", true);
        resultMap.put("token", token);

        logger.info("Uploaded valid zip File file " + part.getName());
      }

    } catch (NoUserFoundException e) {
      resultMap.put("success", false);
      resultMap.put("message", "An error occured. The tool could not upload the zip file sent");
      resultMap.put("debugError", ExceptionUtils.getMessage(e));
      return resultMap;
    } catch (MessageUploadException e) {
      resultMap.put("success", false);
      resultMap.put("message", "An error occured. The tool could not upload the zip file sent");
      resultMap.put("debugError", ExceptionUtils.getMessage(e));
      return resultMap;
    } catch (Exception e) {
      resultMap.put("success", false);
      resultMap.put("message", "An error occured. The tool could not upload the zip file sent");
      resultMap.put("debugError", ExceptionUtils.getStackTrace(e));
      return resultMap;
    }
    return resultMap;
  }

  /**
   * Retrieves a list of profiles from previously uploaded files
   * 
   * @param request Client request
   * @param token token from uploaded files
   * @param p Principal
   * @return A list of profiles
   * @throws Exception
   */
  @PreAuthorize("hasRole('tester')")
  @RequestMapping(value = "/tokens/{token}/profiles", method = RequestMethod.GET)
  @ResponseBody
  public Map<String, Object> getTokenProfiles(ServletRequest request,
      @PathVariable("token") String token, Principal p) throws Exception {
    checkManagementSupport();

    Map<String, Object> resultMap = new HashMap<String, Object>();
    try {

      String userName = userIdService.getCurrentUserName(p);
      if (userName == null)
        throw new NoUserFoundException("User could not be found");

      String directory = CF_UPLOAD_DIR + "/" + userName + "/" + token;
      if (!new File(directory).exists())
        throw new NotValidToken("The provided token is not valid for this account.");

      String profileContent = bundleHandler.getProfileContentFromZipDirectory(directory);

      if (profileContent == null)
        throw new MessageUploadException("Could not retrieve the profile list");

      List<UploadedProfileModel> list = packagingHandler.getUploadedProfiles(profileContent);
      resultMap.put("success", true);
      resultMap.put("profiles", list);

      logger.info("retrieved profile info from upload");

    } catch (NoUserFoundException e) {
      resultMap.put("success", false);
      resultMap.put("message",
          "An error occured. The tool could not retrieve the uploaded profiles");
      resultMap.put("debugError", ExceptionUtils.getMessage(e));
      return resultMap;
    } catch (NotValidToken e) {
      resultMap.put("success", false);
      resultMap.put("message",
          "An error occured. The tool could not retrieve the uploaded profiles");
      resultMap.put("debugError", ExceptionUtils.getMessage(e));
      return resultMap;
    } catch (Exception e) {
      resultMap.put("success", false);
      resultMap.put("message",
          "An error occured. The tool could not retrieve the uploaded profiles");
      resultMap.put("debugError", ExceptionUtils.getStackTrace(e));
      return resultMap;
    }
    return resultMap;
  }



  private CFTestStep findTestStep(Long id, CFTestPlan testPlan) {
    if (testPlan != null && testPlan.getTestCases() != null) {
      for (CFTestStep testStep : testPlan.getTestCases()) {
        if (testStep.getId().equals(id)) {
          return testStep;
        }
      }
    }
    return null;
  }


  /**
   * Add selected profiles to the database
   * 
   * @param request Client request
   * @param wrapper Selected profile information
   * @param p Principal
   * @return UploadStatus
   */
  @PreAuthorize("hasRole('tester')")
  @RequestMapping(value = "/groups/{groupId}", method = RequestMethod.POST)
  @ResponseBody
  public UploadStatus saveGrop(HttpServletRequest request, @PathVariable("groupId") Long groupId,
      @RequestBody TestCaseWrapper wrapper, Principal p) {
    try {
      checkManagementSupport();
      // String username = null;
      String username = userIdService.getCurrentUserName(p);
      if (username == null)
        throw new NoUserFoundException("User could not be found");

      if (wrapper.getScope() == null)
        throw new NoUserFoundException("Scope not be found");

      TestScope scope = TestScope.valueOf(wrapper.getScope().toUpperCase());
      if (scope.equals(TestScope.GLOBAL) && !userService.hasGlobalAuthorities(username)) {
        throw new NoUserFoundException("You do not have the permission to perform this task");
      }

      CFTestPlan testPlan = testPlanService.findOne(groupId);

      if (testPlan == null) {
        throw new Exception("Profile Group could not be found");
      }

      if (!username.equals(testPlan.getAuthorUsername())) {
        throw new Exception("You do not have sufficient right to change this profile group");
      }

      testPlan.setName(wrapper.getTestcasename());
      testPlan.setDescription(wrapper.getTestcasedescription());

      Set<UploadedProfileModel> removed = wrapper.getRemoved();
      if (removed != null && !removed.isEmpty()) {
        for (UploadedProfileModel model : removed) {
          Long id = Long.valueOf(model.getId());
          CFTestStep found = findTestStep(id, testPlan);
          if (found != null && testPlan.getTestCases() != null) {
            testPlan.getTestCases().remove(found);
          }
        }
      }

      Set<UploadedProfileModel> updated = wrapper.getUpdated();
      if (updated != null && !updated.isEmpty()) {
        for (UploadedProfileModel model : updated) {
          Long id = Long.valueOf(model.getId());
          CFTestStep found = findTestStep(id, testPlan);
          if (found != null) {
            found.setName(model.getName());
            found.setDescription(model.getDescription());
            found.setPosition(model.getPosition());
            TestContext context = found.getTestContext();
            Message message = context.getMessage();
            if (model.getExampleMessage() != null) {
              if (message == null) {
                message = new Message();
                message.setName(model.getName());
                message.setDescription(model.getDescription());
                context.setMessage(message);
              }
              message.setContent(model.getExampleMessage());
            }
          }
        }
      }

      if (wrapper.getToken() != null) {
        // Create needed files
        JSONObject testCaseJson = new JSONObject();
        testCaseJson.put("name", wrapper.getTestcasename());
        testCaseJson.put("description", wrapper.getTestcasedescription());
        testCaseJson.put("profile", "Profile.xml");
        testCaseJson.put("constraints", "Constraints.xml");
        testCaseJson.put("vs", "ValueSets.xml");
        testCaseJson.put("scope", scope);
        testCaseJson.put("category", wrapper.getCategory());

        Set<UploadedProfileModel> added = wrapper.getAdded();
        if (added != null && !added.isEmpty()) {
          JSONArray testSteps = new JSONArray();
          for (UploadedProfileModel upm : added) {
            JSONObject ts = new JSONObject();
            ts.put("name", upm.getName());
            ts.put("messageId", upm.getId());
            ts.put("description", upm.getDescription());
            ts.put("position", upm.getPosition());
            ts.put("scope", scope);
            if (upm.getExampleMessage() != null) {
              ts.put("exampleMessage", upm.getExampleMessage());
            }
            testSteps.put(ts);
          }
          testCaseJson.put("testCases", testSteps);
        }

        File jsonFile =
            new File(CF_UPLOAD_DIR + "/" + username + "/" + wrapper.getToken() + "/TestCases.json");
        File profileFile =
            new File(CF_UPLOAD_DIR + "/" + username + "/" + wrapper.getToken() + "/Profile.xml");
        File constraintsFile = new File(
            CF_UPLOAD_DIR + "/" + username + "/" + wrapper.getToken() + "/Constraints.xml");
        File vsFile =
            new File(CF_UPLOAD_DIR + "/" + username + "/" + wrapper.getToken() + "/ValueSets.xml");

        if (constraintsFile != null) {
          packagingHandler.changeConstraintId(constraintsFile);
        }
        if (vsFile != null) {
          packagingHandler.changeVsId(vsFile);
        }
        InputStream targetStream = new FileInputStream(profileFile);
        String content = IOUtils.toString(targetStream);
        String cleanedContent = packagingHandler.removeUnusedAndDuplicateMessages(content, added);
        FileUtils.writeStringToFile(profileFile, cleanedContent);
        packagingHandler.changeProfileId(profileFile);
        FileUtils.writeStringToFile(jsonFile, testCaseJson.toString());
        // Use files to save to database
        GVTSaveInstance si = bundleHandler.createGVTSaveInstance(
            CF_UPLOAD_DIR + "/" + username + "/" + wrapper.getToken(), testPlan);
        ipRepository.save(si.ip);
        csRepository.save(si.ct);
        vsRepository.save(si.vs);
        si.tcg.setAuthorUsername(username);
        testCaseGroupRepository.saveAndFlush(si.tcg);
        FileUtils
            .deleteDirectory(new File(CF_UPLOAD_DIR + "/" + username + "/" + wrapper.getToken()));
      } else {
        testPlanService.save(testPlan);
      }
      return new UploadStatus(ResourceUploadResult.SUCCESS, "Profile Group save successfully",
          testPlan.getId());

    } catch (IOException e) {
      return new UploadStatus(ResourceUploadResult.FAILURE, "IO Error could not read files",
          ExceptionUtils.getStackTrace(e));
    } catch (NoUserFoundException e) {
      return new UploadStatus(ResourceUploadResult.FAILURE, "User could not be found",
          ExceptionUtils.getStackTrace(e));
    } catch (Exception e) {
      return new UploadStatus(ResourceUploadResult.FAILURE,
          "An error occured while adding profiles", ExceptionUtils.getStackTrace(e));
    }

  }

  /**
   * Delete a profile from the database
   * 
   * @param request Client request
   * @param lr Profile id
   * @param p Principal
   * @return True/False as success indicator
   * @throws Exception
   */
  @PreAuthorize("hasRole('tester')")
  @RequestMapping(value = "/profiles/{profileId}/delete", method = RequestMethod.POST)
  @ResponseBody
  @Transactional(value = "transactionManager")
  public boolean deleteProfile(ServletRequest request, @PathVariable("profileId") Long profileId,
      Principal p) throws Exception {
    checkManagementSupport();

    boolean res = true;
    String userName = userIdService.getCurrentUserName(p);

    if (userName == null) {
      throw new NoUserFoundException("User could not be found");
    }

    List<CFTestPlan> list = testCaseGroupRepository.userExclusive(userName);
    boolean found = false;
    for (CFTestPlan utg : list) {

      for (Iterator<CFTestStep> iterator = utg.getTestCases().iterator(); iterator.hasNext();) {
        CFTestStep ucf = iterator.next();
        if (ucf.getId().equals(profileId)) {
          iterator.remove();
          found = true;
        }
      }
      if (found) {
        if (utg.getTestCases().size() == 0) {
          testCaseGroupRepository.delete(utg);
          res = true;
        } else {
          testCaseGroupRepository.save(utg);
          res = false;
        }
        break;
      }

    }

    // return true if testPlan is also deleted, false otherwise
    return res;
  }

}
