package gov.nist.hit.core.hl7v2.api;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import gov.nist.hit.core.domain.SessionTestCases;
import gov.nist.hit.core.repo.ConstraintsRepository;
import gov.nist.hit.core.repo.IntegrationProfileRepository;
import gov.nist.hit.core.repo.UserTestCaseGroupRepository;
import gov.nist.hit.core.repo.VocabularyLibraryRepository;
import gov.nist.hit.core.service.AccountService;
import gov.nist.hit.core.service.BundleHandler;
import gov.nist.hit.core.service.CFTestPlanService;
import gov.nist.hit.core.service.ResourceLoader;
import gov.nist.hit.core.service.Streamer;
import gov.nist.hit.core.service.UserIdService;
import gov.nist.hit.core.service.UserService;
import gov.nist.hit.core.service.exception.NoUserFoundException;
import io.swagger.annotations.Api;



@RequestMapping("/cf/hl7v2/groups")
@Api(hidden = true)
@Controller
public class HL7V2CFTestCaseController {

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
  private CFTestPlanService testPlanService;

  @Autowired
  private AccountService accountService;

  @Autowired
  private Streamer streamer;

  @Autowired
  private ResourceLoader resouceLoader;

  @Autowired
  private UserService userService;

  @PreAuthorize("hasRole('tester')")
  @RequestMapping(value = "/", method = RequestMethod.GET)
  @ResponseBody
  public SessionTestCases testcases(Principal p) throws NoUserFoundException {
    String userName = userIdService.getCurrentUserName(p);

    if (userName == null)
      throw new NoUserFoundException("User could not be found");

    SessionTestCases stc = new SessionTestCases();
    stc.setPreloaded(testCaseGroupRepository.findByPreloaded(true));
    stc.setUser(testCaseGroupRepository.userExclusive(userName));
    return stc;
  }

}
