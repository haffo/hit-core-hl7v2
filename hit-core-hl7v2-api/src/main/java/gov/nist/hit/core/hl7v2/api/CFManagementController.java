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
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import gov.nist.auth.hit.core.domain.Account;
import gov.nist.hit.core.api.SessionContext;
import gov.nist.hit.core.domain.AbstractTestCase;
import gov.nist.hit.core.domain.CFTestPlan;
import gov.nist.hit.core.domain.CFTestStep;
import gov.nist.hit.core.domain.CFTestStepGroup;
import gov.nist.hit.core.domain.Message;
import gov.nist.hit.core.domain.ResourceType;
import gov.nist.hit.core.domain.ResourceUploadAction;
import gov.nist.hit.core.domain.ResourceUploadResult;
import gov.nist.hit.core.domain.ResourceUploadStatus;
import gov.nist.hit.core.domain.TestScope;
import gov.nist.hit.core.domain.UploadedProfileModel;
import gov.nist.hit.core.service.AccountService;
import gov.nist.hit.core.service.AppInfoService;
import gov.nist.hit.core.service.CFTestPlanService;
import gov.nist.hit.core.service.CFTestStepGroupService;
import gov.nist.hit.core.service.CFTestStepService;
import gov.nist.hit.core.service.DomainService;
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
  private CFTestStepGroupService testStepGroupService;

  @Autowired
  private DomainService domainService;

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


  private void checkPermission(Long id, AbstractTestCase testObject, Principal p) throws Exception {
    String username = userIdService.getCurrentUserName(p);
    if (username == null)
      throw new NoUserFoundException("User could not be found");
    if (testObject == null)
      throw new Exception("No " + testObject.getType() + " (" + id + ") found");
    TestScope scope = testObject.getScope();
    if (scope.equals(TestScope.GLOBAL) && !userService.hasGlobalAuthorities(username)) {
      throw new NoUserFoundException("You do not have the permission to perform this task");
    }
    if (!username.equals(testObject.getAuthorUsername()) && !userService.isAdmin(username)) {
      throw new NoUserFoundException("You do not have the permission to perform this task");
    }
  }

  @PreAuthorize("hasRole('tester')")
  @RequestMapping(value = "/testPlans", method = RequestMethod.GET, produces = "application/json")
  public List<CFTestPlan> getTestPlansByScopeAndDomain(
      @ApiParam(value = "the scope of the test plans",
          required = false) @RequestParam(required = true) TestScope scope,
      HttpServletRequest request, HttpServletResponse response,
      @RequestParam(required = true) String domain) throws Exception {
    checkManagementSupport();
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
  @RequestMapping(value = "/testPlans/{testPlanId}/addChild", method = RequestMethod.POST,
      produces = "application/json", consumes = {"application/x-www-form-urlencoded;"})
  public CFTestStepGroup addTestPlanChild(HttpServletRequest request,
      @PathVariable("testPlanId") Long testPlanId,
      @RequestParam(value = "name", required = true) String name,
      @RequestParam(value = "description", required = false) String description,
      @RequestParam(value = "domain", required = true) String domain, Principal p,
      @RequestParam("position") Integer position, Authentication auth) throws Exception {
    checkManagementSupport();
    domainService.hasPermission(domain, auth);

    String username = auth.getName();
    CFTestPlan parent = testPlanService.findOne(testPlanId);
    if (parent == null) {
      throw new IllegalArgumentException("TestPlan[" + testPlanId + "] not found");
    }
    CFTestStepGroup testGroup = new CFTestStepGroup();
    testGroup.setAuthorUsername(username);
    testGroup.setScope(parent.getScope());
    testGroup.setDescription(description);
    testGroup.setName(name);
    testGroup.setPersistentId(new Date().getTime());
    testGroup.setPosition(position);
    testGroup.setDomain(domain);
    addChild(parent.getTestStepGroups(), testGroup, position);
    testStepGroupService.save(testGroup);
    testPlanService.save(parent);
    return testGroup;
  }


  @PreAuthorize("hasRole('tester')")
  @RequestMapping(value = "/testPlans/create", method = RequestMethod.POST,
      produces = "application/json", consumes = {"application/x-www-form-urlencoded;"})
  public CFTestPlan createNewTestPlan(HttpServletRequest request,
      @RequestParam(value = "scope", required = true) TestScope scope,
      @RequestParam(value = "name", required = true) String name,
      @RequestParam(value = "description", required = false) String description,
      @RequestParam(value = "domain", required = true) String domain, Authentication auth,
      @RequestParam("position") Integer position) throws Exception {
    checkManagementSupport();
    domainService.hasPermission(domain, auth);
    String username = auth.getName();
    CFTestPlan testPlan = new CFTestPlan();
    testPlan.setAuthorUsername(username);
    testPlan.setScope(scope);
    testPlan.setDescription(description);
    testPlan.setName(name);
    testPlan.setPersistentId(new Date().getTime());
    testPlan.setPosition(position);
    testPlan.setDomain(domain);
    testPlanService.save(testPlan);
    return testPlan;
  }



  @PreAuthorize("hasRole('tester')")
  @RequestMapping(value = "/testStepGroups/{testStepGroupId}/addChild", method = RequestMethod.POST,
      produces = "application/json", consumes = {"application/x-www-form-urlencoded;"})
  public CFTestStepGroup addTestStepGroupChild(HttpServletRequest request,
      @PathVariable("testStepGroupId") Long testStepGroupId,
      @RequestParam(value = "name", required = true) String name,
      @RequestParam(value = "description", required = false) String description,
      @RequestParam(value = "domain", required = true) String domain, Principal p,
      @RequestParam("position") Integer position, Authentication auth) throws Exception {
    checkManagementSupport();
    domainService.hasPermission(domain, auth);
    String username = auth.getName();
    CFTestStepGroup parent = testStepGroupService.findOne(testStepGroupId);
    if (parent == null) {
      throw new IllegalArgumentException("CFTestStepGroup[" + testStepGroupId + "] not found");
    }

    CFTestStepGroup testGroup = new CFTestStepGroup();
    testGroup.setAuthorUsername(username);
    testGroup.setScope(parent.getScope());
    testGroup.setDescription(description);
    testGroup.setName(name);
    testGroup.setPersistentId(new Date().getTime());
    testGroup.setPosition(position);
    testGroup.setDomain(domain);
    addChild(parent.getTestStepGroups(), testGroup, position);
    testStepGroupService.save(testGroup);
    testStepGroupService.save(parent);
    return testGroup;
  }


  private void updatePositions(Set<CFTestStepGroup> children) {
    List<CFTestStepGroup> list = orderByPositions(new ArrayList<CFTestStepGroup>(children));
    for (int index = 0; index < list.size(); index++) {
      list.get(index).setPosition(index + 1);
    }
  }

  private void updatePositionsByIndex(List<CFTestStepGroup> list) {
    for (int index = 0; index < list.size(); index++) {
      list.get(index).setPosition(index + 1);
    }
  }



  private List<CFTestStepGroup> orderByPositions(List<CFTestStepGroup> list) {
    Collections.sort(list, new Comparator<CFTestStepGroup>() {
      @Override
      public int compare(CFTestStepGroup o1, CFTestStepGroup o2) {
        return o1.getPosition() - o2.getPosition();
      }
    });
    return list;
  }



  @PreAuthorize("hasRole('tester')")
  @RequestMapping(value = "/testStepGroups/{testStepGroupId}/location", method = RequestMethod.POST,
      produces = "application/json", consumes = {"application/x-www-form-urlencoded;"})
  public ResourceUploadStatus updateLocation(HttpServletRequest request,
      @PathVariable("testStepGroupId") Long testStepGroupId,
      @RequestParam("oldParentId") Long oldParentId, @RequestParam("newParentId") Long newParentId,
      @RequestParam("oldParentType") String oldParentType,
      @RequestParam("newParentType") String newParentType, Principal p,
      @RequestParam("newPosition") Integer newPosition) {
    try {
      checkManagementSupport();
      // String username = null;
      String username = userIdService.getCurrentUserName(p);
      if (username == null)
        throw new NoUserFoundException("User could not be found");

      CFTestStepGroup testStepGroup = testStepGroupService.findOne(testStepGroupId);
      if (testStepGroup == null) {
        throw new IllegalArgumentException("CFTestStepGroup[" + testStepGroupId + "] not found");
      }

      if (oldParentType.equals(newParentType) && oldParentId.equals(newParentId)) {

        if (oldParentType.equals("TestStepGroup")) {
          CFTestStepGroup oldParent = testStepGroupService.findOne(oldParentId);
          if (oldParent == null) {
            throw new IllegalArgumentException("CFTestStepGroup[" + oldParentId + "] not found");
          }
          CFTestStepGroup tmp = findTestStepGroup(oldParent.getTestStepGroups(), testStepGroupId);
          shiftPositions(oldParent.getTestStepGroups(), tmp, newPosition);
          testStepGroupService.save(oldParent);
        } else {
          CFTestPlan oldParent = testPlanService.findOne(oldParentId);
          if (oldParent == null) {
            throw new IllegalArgumentException("CFTestPlan[" + oldParentId + "] not found");
          }
          CFTestStepGroup tmp = findTestStepGroup(oldParent.getTestStepGroups(), testStepGroupId);
          shiftPositions(oldParent.getTestStepGroups(), tmp, newPosition);
          testPlanService.save(oldParent);
        }

      } else {
        if (oldParentType.equals("TestStepGroup")) {
          CFTestStepGroup oldParent = testStepGroupService.findOne(oldParentId);
          if (oldParent == null) {
            throw new IllegalArgumentException("CFTestStepGroup[" + oldParentId + "] not found");
          }
          removeChild(oldParent.getTestStepGroups(), testStepGroupId);
          testStepGroupService.save(oldParent);
          if (newParentType.equals("TestStepGroup")) {
            CFTestStepGroup newParent = testStepGroupService.findOne(newParentId);
            addChild(newParent.getTestStepGroups(), testStepGroup, newPosition);
            testStepGroupService.save(newParent);
          } else {
            CFTestPlan newParent = testPlanService.findOne(newParentId);
            addChild(newParent.getTestStepGroups(), testStepGroup, newPosition);
            testPlanService.save(newParent);
          }
        } else {
          CFTestPlan oldParent = testPlanService.findOne(oldParentId);
          if (oldParent == null) {
            throw new IllegalArgumentException("CFTestPlan[" + oldParentId + "] not found");
          }
          removeChild(oldParent.getTestStepGroups(), testStepGroupId);
          testPlanService.save(oldParent);
          if (newParentType.equals("TestStepGroup")) {
            CFTestStepGroup newParent = testStepGroupService.findOne(newParentId);
            addChild(newParent.getTestStepGroups(), testStepGroup, newPosition);
            testStepGroupService.save(newParent);
          } else {
            CFTestPlan newParent = testPlanService.findOne(newParentId);
            addChild(newParent.getTestStepGroups(), testStepGroup, newPosition);
            testPlanService.save(newParent);
          }

        }
      }

      ResourceUploadStatus result = new ResourceUploadStatus();
      result.setType(ResourceType.TESTSTEPGROUP);
      result.setAction(ResourceUploadAction.UPDATE);
      result.setId(testStepGroupId);
      result.setStatus(ResourceUploadResult.SUCCESS);
      return result;
    } catch (Exception e) {
      ResourceUploadStatus result = new ResourceUploadStatus();
      result.setType(ResourceType.TESTSTEPGROUP);
      result.setAction(ResourceUploadAction.UPDATE);
      result.setId(testStepGroupId);
      result.setStatus(ResourceUploadResult.FAILURE);
      result.setMessage(e.getMessage());
      return result;
    }
  }


  public static <CFTestStepGroup> void moveItem(int sourceIndex, int targetIndex,
      List<CFTestStepGroup> list) {
    if (sourceIndex <= targetIndex) {
      Collections.rotate(list.subList(sourceIndex, targetIndex + 1), -1);
    } else {
      Collections.rotate(list.subList(targetIndex, sourceIndex + 1), 1);
    }
  }



  private void addChild(Set<CFTestStepGroup> collection, CFTestStepGroup child,
      Integer newPosition) {
    collection.add(child);
    child.setPosition(newPosition);
    List<CFTestStepGroup> tmp = orderByPositions(new ArrayList<CFTestStepGroup>(collection));
    moveItem(tmp.indexOf(child), newPosition - 1, tmp);
    updatePositionsByIndex(tmp);
  }

  private void shiftPositions(Set<CFTestStepGroup> collection, CFTestStepGroup child,
      Integer newPosition) {
    List<CFTestStepGroup> tmp = orderByPositions(new ArrayList<CFTestStepGroup>(collection));
    moveItem(tmp.indexOf(child), newPosition - 1, tmp);
    child.setPosition(newPosition);
    updatePositionsByIndex(tmp);
  }


  private void removeChild(Set<CFTestStepGroup> collection, Long childId) {
    CFTestStepGroup testStepGroup = findTestStepGroup(collection, childId);
    collection.remove(testStepGroup);
    updatePositions(collection);
  }



  @PreAuthorize("hasRole('tester')")
  @RequestMapping(value = "/testPlans/{testPlanId}/delete", method = RequestMethod.POST,
      produces = "application/json")
  public ResourceUploadStatus deleteTestPlan(HttpServletRequest request,
      @PathVariable("testPlanId") Long testPlanId, Principal p) throws Exception {
    checkManagementSupport();
    // String username = null;
    String username = userIdService.getCurrentUserName(p);
    if (username == null)
      throw new NoUserFoundException("User could not be found");
    CFTestPlan testPlan = testPlanService.findOne(testPlanId);
    if (testPlan == null)
      throw new Exception("No TestPlan Group(" + testPlanId + ") found");
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
  @RequestMapping(value = "/testPlans/{testPlanId}/testStepGroups/{testStepGroupId}/delete",
      method = RequestMethod.POST, produces = "application/json")
  public ResourceUploadStatus deleteTestStepGroup(HttpServletRequest request,
      @PathVariable("testStepGroupId") Long testStepGroupId,
      @PathVariable("testPlanId") Long testPlanId, Principal p) throws Exception {
    checkManagementSupport();
    CFTestPlan tp = testPlanService.findOne(testPlanId);
    if (tp != null) {
      CFTestStepGroup testStepGroup = testStepGroupService.findOne(testStepGroupId);
      if (testStepGroup != null) {
        checkPermission(testStepGroupId, testStepGroup, p);
        CFTestStepGroup found = findTestStepGroup(tp.getTestStepGroups(), testStepGroupId);
        tp.getTestStepGroups().remove(found);
        testPlanService.save(tp);
        ResourceUploadStatus result = deleteTestStepGroup(found);
        return result;
      } else {
        ResourceUploadStatus result = new ResourceUploadStatus();
        result.setType(ResourceType.TESTCASE);
        result.setAction(ResourceUploadAction.DELETE);
        result.setId(testStepGroupId);
        result.setMessage("TestStepGroup(" + testStepGroupId + ") Not found");
        result.setStatus(ResourceUploadResult.FAILURE);
        return result;
      }
    } else {
      ResourceUploadStatus result = new ResourceUploadStatus();
      result.setType(ResourceType.TESTCASE);
      result.setAction(ResourceUploadAction.DELETE);
      result.setId(testStepGroupId);
      result.setMessage("TestPlan(" + testPlanId + ") Not found");
      result.setStatus(ResourceUploadResult.FAILURE);
      return result;
    }
  }


  @PreAuthorize("hasRole('tester')")
  @RequestMapping(
      value = "/testStepGroups/{parentTestStepGroupId}/testStepGroups/{testStepGroupId}/delete",
      method = RequestMethod.POST, produces = "application/json")
  public ResourceUploadStatus deleteTestStepGroupGroupTestStepGroup(HttpServletRequest request,
      @PathVariable("testStepGroupId") Long testStepGroupId,
      @PathVariable("parentTestStepGroupId") Long parentTestStepGroupId, Principal p)
      throws Exception {
    checkManagementSupport();
    CFTestStepGroup tp = testStepGroupService.findOne(parentTestStepGroupId);
    if (tp != null) {
      CFTestStepGroup testStepGroup = testStepGroupService.findOne(testStepGroupId);
      if (testStepGroup != null) {
        checkPermission(testStepGroupId, testStepGroup, p);
        CFTestStepGroup found = findTestStepGroup(tp.getTestStepGroups(), testStepGroupId);
        tp.getTestStepGroups().remove(found);
        testStepGroupService.save(tp);
        ResourceUploadStatus result = deleteTestStepGroup(found);
        return result;
      } else {
        ResourceUploadStatus result = new ResourceUploadStatus();
        result.setType(ResourceType.TESTCASE);
        result.setAction(ResourceUploadAction.DELETE);
        result.setId(testStepGroupId);
        result.setMessage("CFTestStepGroup(" + testStepGroupId + ") Not found");
        result.setStatus(ResourceUploadResult.FAILURE);
        return result;
      }
    } else {
      ResourceUploadStatus result = new ResourceUploadStatus();
      result.setType(ResourceType.TESTCASE);
      result.setAction(ResourceUploadAction.DELETE);
      result.setId(parentTestStepGroupId);
      result.setMessage("CFTestStepGroup(" + parentTestStepGroupId + ") Not found");
      result.setStatus(ResourceUploadResult.FAILURE);
      return result;
    }

  }

  private CFTestStepGroup findTestStepGroup(Set<CFTestStepGroup> set, Long id) {
    for (CFTestStepGroup group : set) {
      if (group.getId().equals(id)) {
        return group;
      }
    }
    return null;
  }



  private ResourceUploadStatus deleteTestStepGroup(CFTestStepGroup tp) throws Exception {
    checkManagementSupport();
    testStepGroupService.delete(tp);
    // if (testSteps != null) {
    // for (TestStep testStep : testSteps) {
    // deleteTestStep(testStep);
    // }
    // }
    ResourceUploadStatus result = new ResourceUploadStatus();
    result.setType(ResourceType.TESTSTEPGROUP);
    result.setAction(ResourceUploadAction.DELETE);
    result.setId(tp.getId());
    result.setStatus(ResourceUploadResult.SUCCESS);
    return result;
  }



  @PreAuthorize("hasRole('tester')")
  @RequestMapping(value = "/testPlans/{testPlanId}/publish", method = RequestMethod.POST,
      produces = "application/json")
  public ResourceUploadStatus approvePublishing(HttpServletRequest request,
      @PathVariable("testPlanId") Long testPlanId, Principal p) throws Exception {
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

    CFTestPlan testPlan = testPlanService.findOne(testPlanId);
    if (testPlan == null)
      throw new Exception("No Profile Group(" + testPlanId + ") found");

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
  //
  // @PreAuthorize("hasRole('tester')")
  // @RequestMapping(value = "/groups/delete", method = RequestMethod.POST,
  // produces = "application/json")
  // public List<ResourceUploadStatus> createGroup(HttpServletRequest request,
  // @RequestBody Set<Long> groupIds, Principal p) throws Exception {
  // checkManagementSupport();
  // List<ResourceUploadStatus> status = new ArrayList<ResourceUploadStatus>();
  // // String username = null;
  // String username = userIdService.getCurrentUserName(p);
  // if (username == null)
  // throw new NoUserFoundException("User could not be found");
  //
  // for (Long groupId : groupIds) {
  // status.add(deleteGroup(request, groupId, p));
  // }
  // return status;
  // }

  // @RequestMapping(value = "/categories", method = RequestMethod.GET, produces =
  // "application/json")
  // public Set<String> getTestPlanCategories(
  // @ApiParam(value = "the scope of the test plans",
  // required = false) @RequestParam(required = true) TestScope scope,
  // @RequestParam(required = true) String domain, HttpServletRequest request,
  // HttpServletResponse response) throws Exception {
  // checkManagementSupport();
  // Set<String> results = null;
  // scope = scope == null ? TestScope.GLOBAL : scope;
  // String username = null;
  // Long userId = SessionContext.getCurrentUserId(request.getSession(false));
  // if (userId != null) {
  // Account account = accountService.findOne(userId);
  // if (account != null) {
  // username = account.getUsername();
  // }
  // }
  // if (scope.equals(TestScope.GLOBAL)) {
  // results = testPlanService.findAllCategoriesByScopeAndDomain(scope, domain);
  // } else {
  // results = testPlanService.findAllCategoriesByScopeAndUserAndDomain(scope, username, domain);
  // }
  // return results;
  // }

  // @PreAuthorize("hasRole('tester')")
  // @RequestMapping(value = "/categories/{category}/addGroup", method = RequestMethod.POST,
  // produces = "application/json")
  // public UploadStatus updateCategory(@PathVariable("category") String category,
  // @RequestBody Long groupId, HttpServletRequest request, HttpServletResponse response,
  // Principal p) throws IOException, NoUserFoundException {
  // try {
  // checkManagementSupport();
  // String username = userIdService.getCurrentUserName(p);
  // if (username == null)
  // throw new NoUserFoundException("User could not be found");
  //
  // CFTestPlan testPlan = testPlanService.findOne(groupId);
  // if (testPlan == null)
  // throw new Exception("Test Plan not found");
  //
  // if (!username.equals(testPlan.getAuthorUsername())) {
  // throw new NoUserFoundException("You do not have the permission to perform this task");
  // }
  //
  // TestScope scope = testPlan.getScope();
  // if (scope.equals(TestScope.GLOBAL) && !userService.hasGlobalAuthorities(username)) {
  // throw new NoUserFoundException("You do not have the permission to perform this task");
  // }
  //
  // if (category != null) {
  // testPlan.setCategory(category);
  // }
  //
  // testPlanService.save(testPlan);
  //
  // } catch (IOException e) {
  // return new UploadStatus(ResourceUploadResult.FAILURE, "Failed to save the group",
  // e.getMessage());
  // } catch (NoUserFoundException e) {
  // return new UploadStatus(ResourceUploadResult.FAILURE, "User could not be found",
  // e.getMessage());
  // } catch (Exception e) {
  // return new UploadStatus(ResourceUploadResult.FAILURE,
  // "An error occured while adding profiles", e.getMessage());
  // }
  // return new UploadStatus(ResourceUploadResult.SUCCESS, "Saved");
  // }

  @PreAuthorize("hasRole('tester')")
  @RequestMapping(value = "/testPlans/{testPlanId}/profiles", method = RequestMethod.GET,
      produces = "application/json")
  public List<UploadedProfileModel> getTestPlanProfiles(@PathVariable("testPlanId") Long testPlanId,
      HttpServletRequest request, HttpServletResponse response) throws Exception {
    checkManagementSupport();
    CFTestPlan testPlan = testPlanService.findOne(testPlanId);
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
  @RequestMapping(value = "/testStepGroups/{testStepGroupId}/profiles", method = RequestMethod.GET,
      produces = "application/json")
  public List<UploadedProfileModel> getGroupProfiles(
      @PathVariable("testStepGroupId") Long testStepGroupId, HttpServletRequest request,
      HttpServletResponse response) throws Exception {
    checkManagementSupport();
    CFTestStepGroup testStepGroup = testStepGroupService.findOne(testStepGroupId);
    if (testStepGroup != null) {
      Set<CFTestStep> steps = testStepGroup.getTestSteps();
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
  @RequestMapping(value = "/testPlans/{testPlanId}", method = RequestMethod.GET,
      produces = "application/json")
  public CFTestPlan getTestPlan(@PathVariable("testPlanId") Long testPlanId,
      HttpServletRequest request, HttpServletResponse response) throws Exception {
    checkManagementSupport();
    CFTestPlan testPlan = testPlanService.findOne(testPlanId);
    return testPlan;
  }


  // @PreAuthorize("hasRole('tester')")
  // @RequestMapping(value = "/categories/{category}", method = RequestMethod.POST,
  // produces = "application/json")
  // public boolean updateCategories(@PathVariable("category") String category,
  // @RequestBody Set<Long> groups, HttpServletRequest request, HttpServletResponse response,
  // Principal p) throws Exception {
  // checkManagementSupport();
  // for (Long id : groups) {
  // updateCategory(category, id, request, response, p);
  // }
  // return true;
  // }

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
