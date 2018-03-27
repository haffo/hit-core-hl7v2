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
import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import gov.nist.auth.hit.core.domain.Account;
import gov.nist.hit.core.api.SessionContext;
import gov.nist.hit.core.domain.CFTestPlan;
import gov.nist.hit.core.domain.CFTestStep;
import gov.nist.hit.core.domain.CFTestStepGroup;
import gov.nist.hit.core.domain.Message;
import gov.nist.hit.core.domain.ResourceType;
import gov.nist.hit.core.domain.ResourceUploadAction;
import gov.nist.hit.core.domain.ResourceUploadResult;
import gov.nist.hit.core.domain.ResourceUploadStatus;
import gov.nist.hit.core.domain.TestScope;
import gov.nist.hit.core.domain.UploadStatus;
import gov.nist.hit.core.domain.UploadedProfileModel;
import gov.nist.hit.core.service.AccountService;
import gov.nist.hit.core.service.AppInfoService;
import gov.nist.hit.core.service.CFTestPlanService;
import gov.nist.hit.core.service.CFTestStepService;
import gov.nist.hit.core.service.UserIdService;
import gov.nist.hit.core.service.UserService;
import gov.nist.hit.core.service.exception.NoUserFoundException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;

/**
 * @author Harold Affo (NIST)
 * 
 */
@RequestMapping("/cf/management")
@RestController
@Api(value = "Context-free Testing", tags = "Context-free Testing", position = 1)
public class CFManagementController {

  static final Logger logger = LoggerFactory.getLogger(CFManagementController.class);

  public static final String CF_UPLOAD_DIR =
      new File(System.getProperty("java.io.tmpdir")).getAbsolutePath() + "/cf";

  @Autowired
  private CFTestStepService testStepService;

  @Autowired
  private CFTestPlanService testPlanService;

  @Autowired
  private AccountService accountService;

  @Autowired
  private UserService userService;

  @Autowired
  private UserIdService userIdService;

  @Autowired
  private MailSender mailSender;

  @Autowired
  private SimpleMailMessage templateMessage;

  @Value("${server.email}")
  private String SERVER_EMAIL;

  @Autowired
  private AppInfoService appInfoService;

  @Value("${mail.tool}")
  private String TOOL_NAME;

  private void checkManagementSupport() throws Exception {
    if (!appInfoService.get().isCfManagementSupported()) {
      throw new Exception("This operation is not supported by this tool");
    }
  }

  @PreAuthorize("hasRole('tester')")
  @RequestMapping(value = "/groups", method = RequestMethod.GET, produces = "application/json")
  public List<CFTestPlan> getGroupsByScopeAndDomain(
      @ApiParam(value = "the scope of the test plans",
          required = false) @RequestParam(required = true) TestScope scope,
      HttpServletRequest request, HttpServletResponse response,
      @RequestParam(required = true) String domain) throws Exception {
    checkManagementSupport();
    scope = scope == null ? TestScope.GLOBAL : scope;
    String username = null;
    Long userId = SessionContext.getCurrentUserId(request.getSession(false));
    if (userId != null) {
      Account account = accountService.findOne(userId);
      if (account != null) {
        username = account.getUsername();
      }
    }
    return testPlanService.findShortAllByScopeAndUsernameAndDomain(scope, username, domain);
  }

  @PreAuthorize("hasRole('tester')")
  @RequestMapping(value = "/groups/create", method = RequestMethod.POST,
      produces = "application/json", consumes = {"application/x-www-form-urlencoded;"})
  public CFTestPlan createGroup(HttpServletRequest request,
      @RequestParam("category") String category, @RequestParam("scope") TestScope scope,
      @RequestParam(required = true) String domain, Principal p,
      @RequestParam("position") Integer position) throws Exception {
    checkManagementSupport();
    // String username = null;
    String username = userIdService.getCurrentUserName(p);
    if (username == null)
      throw new NoUserFoundException("User could not be found");

    if (scope == null)
      throw new NoUserFoundException("No scope provided");

    if (category == null)
      throw new NoUserFoundException("No category provided");

    if (scope.equals(TestScope.GLOBAL) && !userService.hasGlobalAuthorities(username)) {
      throw new NoUserFoundException("You do not have the permission to perform this task");
    }

    CFTestPlan testPlan = new CFTestPlan();
    testPlan.setAuthorUsername(username);
    testPlan.setScope(scope);
    testPlan.setCategory(category);
    testPlan.setDescription("Desc");
    testPlan.setName("Group" + new Date().getTime());
    testPlan.setPersistentId(new Date().getTime());
    testPlan.setPosition(position);
    testPlan.setDomain(domain);
    testPlanService.save(testPlan);
    return testPlan;
  }

  @PreAuthorize("hasRole('tester')")
  @RequestMapping(value = "/groups/{groupId}/delete", method = RequestMethod.POST,
      produces = "application/json")
  public ResourceUploadStatus deleteGroup(HttpServletRequest request,
      @PathVariable("groupId") Long groupId, Principal p) throws Exception {
    checkManagementSupport();
    // String username = null;
    String username = userIdService.getCurrentUserName(p);
    if (username == null)
      throw new NoUserFoundException("User could not be found");
    CFTestPlan testPlan = testPlanService.findOne(groupId);
    if (testPlan == null)
      throw new Exception("No Profile Group(" + groupId + ") found");
    TestScope scope = testPlan.getScope();
    if (scope.equals(TestScope.GLOBAL) && !userService.hasGlobalAuthorities(username)) {
      throw new NoUserFoundException("You do not have the permission to perform this task");
    }

    if (!username.equals(testPlan.getAuthorUsername())) {
      throw new NoUserFoundException("You do not have the permission to perform this task");
    }

    testPlanService.delete(testPlan);
    ResourceUploadStatus result = new ResourceUploadStatus();
    result.setType(ResourceType.TESTPLAN);
    result.setAction(ResourceUploadAction.DELETE);
    result.setId(testPlan.getId());
    result.setStatus(ResourceUploadResult.SUCCESS);
    return result;
  }

  @PreAuthorize("hasRole('tester')")
  @RequestMapping(value = "/groups/{groupId}/publish", method = RequestMethod.POST,
      produces = "application/json")
  public ResourceUploadStatus approvePublishing(HttpServletRequest request,
      @PathVariable("groupId") Long groupId, Principal p) throws Exception {
    checkManagementSupport();
    // String username = null;

    String username = null;
    Account account = null;
    Long userId = SessionContext.getCurrentUserId(request.getSession(false));
    if (userId != null) {
      account = accountService.findOne(userId);
      if (account != null) {
        username = account.getUsername();
      }
    }
    if (username == null)
      throw new NoUserFoundException("User could not be found");

    CFTestPlan testPlan = testPlanService.findOne(groupId);
    if (testPlan == null)
      throw new Exception("No Profile Group(" + groupId + ") found");

    if (!username.equals(testPlan.getAuthorUsername())) {
      throw new NoUserFoundException("You do not have the permission to perform this task");
    }

    TestScope scope = testPlan.getScope();
    if (scope.equals(TestScope.GLOBAL)) {
      throw new IllegalArgumentException("This Group is not already publicly available ");
    } else if (!userService.hasGlobalAuthorities(username)) {
      throw new IllegalArgumentException("You do not have the permission to perform this task");
    }
    publish(testPlan);
    testPlanService.save(testPlan);
    ResourceUploadStatus result = new ResourceUploadStatus();
    result.setType(ResourceType.TESTPLAN);
    result.setAction(ResourceUploadAction.UPDATE);
    result.setStatus(ResourceUploadResult.SUCCESS);
    return result;
  }

  private void publish(CFTestPlan testPlan) {
    testPlan.setScope(TestScope.GLOBAL);
    Set<CFTestStep> testSteps = testPlan.getTestSteps();
    if (testSteps != null) {
      for (CFTestStep step : testSteps) {
        step.setScope(TestScope.GLOBAL);
      }
    }
    Set<CFTestStepGroup> testStepGroups = testPlan.getTestStepGroups();
    if (testStepGroups != null) {
      for (CFTestStepGroup testStepGroup : testStepGroups) {
        publish(testStepGroup);
      }
    }
  }

  private void publish(CFTestStepGroup testStepGroup) {
    testStepGroup.setScope(TestScope.GLOBAL);
    Set<CFTestStep> testSteps = testStepGroup.getTestSteps();
    if (testSteps != null) {
      for (CFTestStep step : testSteps) {
        step.setScope(TestScope.GLOBAL);
      }
    }
    Set<CFTestStepGroup> testStepGroups = testStepGroup.getTestStepGroups();

    if (testStepGroups != null) {
      for (CFTestStepGroup testStepGr : testStepGroups) {
        publish(testStepGr);
      }
    }
  }

  @PreAuthorize("hasRole('tester')")
  @RequestMapping(value = "/groups/delete", method = RequestMethod.POST,
      produces = "application/json")
  public List<ResourceUploadStatus> createGroup(HttpServletRequest request,
      @RequestBody Set<Long> groupIds, Principal p) throws Exception {
    checkManagementSupport();
    List<ResourceUploadStatus> status = new ArrayList<ResourceUploadStatus>();
    // String username = null;
    String username = userIdService.getCurrentUserName(p);
    if (username == null)
      throw new NoUserFoundException("User could not be found");

    for (Long groupId : groupIds) {
      status.add(deleteGroup(request, groupId, p));
    }
    return status;
  }

  @RequestMapping(value = "/categories", method = RequestMethod.GET, produces = "application/json")
  public Set<String> getTestPlanCategories(
      @ApiParam(value = "the scope of the test plans",
          required = false) @RequestParam(required = true) TestScope scope,
      @RequestParam(required = true) String domain, HttpServletRequest request,
      HttpServletResponse response) throws Exception {
    checkManagementSupport();
    Set<String> results = null;
    scope = scope == null ? TestScope.GLOBAL : scope;
    String username = null;
    Long userId = SessionContext.getCurrentUserId(request.getSession(false));
    if (userId != null) {
      Account account = accountService.findOne(userId);
      if (account != null) {
        username = account.getUsername();
      }
    }
    if (scope.equals(TestScope.GLOBAL)) {
      results = testPlanService.findAllCategoriesByScopeAndDomain(scope, domain);
    } else {
      results = testPlanService.findAllCategoriesByScopeAndUserAndDomain(scope, username, domain);
    }
    return results;
  }

  @PreAuthorize("hasRole('tester')")
  @RequestMapping(value = "/categories/{category}/addGroup", method = RequestMethod.POST,
      produces = "application/json")
  public UploadStatus updateCategory(@PathVariable("category") String category,
      @RequestBody Long groupId, HttpServletRequest request, HttpServletResponse response,
      Principal p) throws IOException, NoUserFoundException {
    try {
      checkManagementSupport();
      String username = userIdService.getCurrentUserName(p);
      if (username == null)
        throw new NoUserFoundException("User could not be found");

      CFTestPlan testPlan = testPlanService.findOne(groupId);
      if (testPlan == null)
        throw new Exception("Test Plan not found");

      if (!username.equals(testPlan.getAuthorUsername())) {
        throw new NoUserFoundException("You do not have the permission to perform this task");
      }

      TestScope scope = testPlan.getScope();
      if (scope.equals(TestScope.GLOBAL) && !userService.hasGlobalAuthorities(username)) {
        throw new NoUserFoundException("You do not have the permission to perform this task");
      }

      if (category != null) {
        testPlan.setCategory(category);
      }

      testPlanService.save(testPlan);

    } catch (IOException e) {
      return new UploadStatus(ResourceUploadResult.FAILURE, "Failed to save the group",
          e.getMessage());
    } catch (NoUserFoundException e) {
      return new UploadStatus(ResourceUploadResult.FAILURE, "User could not be found",
          e.getMessage());
    } catch (Exception e) {
      return new UploadStatus(ResourceUploadResult.FAILURE,
          "An error occured while adding profiles", e.getMessage());
    }
    return new UploadStatus(ResourceUploadResult.SUCCESS, "Saved");
  }

  @PreAuthorize("hasRole('tester')")
  @RequestMapping(value = "/groups/{groupId}/profiles", method = RequestMethod.GET,
      produces = "application/json")
  public List<UploadedProfileModel> getGroupProfiles(@PathVariable("groupId") Long groupId,
      HttpServletRequest request, HttpServletResponse response) throws Exception {
    checkManagementSupport();
    CFTestPlan testPlan = testPlanService.findOne(groupId);
    if (testPlan != null) {
      Set<CFTestStep> steps = testPlan.getTestSteps();
      List<UploadedProfileModel> models = new ArrayList<UploadedProfileModel>();
      for (CFTestStep step : steps) {
        UploadedProfileModel model = new UploadedProfileModel();
        model.setDescription(step.getDescription());
        model.setName(step.getName());
        model.setId(step.getId() + "");
        model.setPosition(step.getPosition());
        if (step.getTestContext() != null) {
          Message message = step.getTestContext().getMessage();
          if (message != null) {
            model.setExampleMessage(message.getContent());
          }
        }
        models.add(model);
      }
      return models;
    }
    return null;
  }

  @PreAuthorize("hasRole('tester')")
  @RequestMapping(value = "/categories/{category}", method = RequestMethod.POST,
      produces = "application/json")
  public boolean updateCategories(@PathVariable("category") String category,
      @RequestBody Set<Long> groups, HttpServletRequest request, HttpServletResponse response,
      Principal p) throws Exception {
    checkManagementSupport();
    for (Long id : groups) {
      updateCategory(category, id, request, response, p);
    }
    return true;
  }

  /**
   * Clear files in tmp directory
   * 
   * @param request Client request
   * @param token files' token
   * @param p Principal
   * @return True/False as success indicator
   * @throws Exception
   */
  @PreAuthorize("hasRole('tester')")
  @RequestMapping(value = "/tokens/{token}/delete", method = RequestMethod.POST)
  @ResponseBody
  public boolean clearFiles(ServletRequest request, @PathVariable("token") String token,
      Principal p) throws Exception {
    checkManagementSupport();
    Long userId = userIdService.getCurrentUserId(p);
    if (userId == null)
      throw new NoUserFoundException("User could not be found");
    FileUtils.deleteDirectory(new File(CF_UPLOAD_DIR + "/" + userId + "/" + token));
    return true;
  }

  public CFTestStepService getTestStepService() {
    return testStepService;
  }

  public void setTestStepService(CFTestStepService testStepService) {
    this.testStepService = testStepService;
  }

  public CFTestPlanService getTestPlanService() {
    return testPlanService;
  }

  public void setTestPlanService(CFTestPlanService testPlanService) {
    this.testPlanService = testPlanService;
  }

  public AccountService getUserService() {
    return accountService;
  }

  public void setUserService(AccountService accountService) {
    this.accountService = accountService;
  }

}
