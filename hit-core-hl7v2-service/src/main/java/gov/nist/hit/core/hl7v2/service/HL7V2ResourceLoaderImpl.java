package gov.nist.hit.core.hl7v2.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;

import com.fasterxml.jackson.core.JsonProcessingException;

import gov.nist.hit.core.domain.AbstractTestCase;
import gov.nist.hit.core.domain.CFTestInstance;
import gov.nist.hit.core.domain.Constraints;
import gov.nist.hit.core.domain.IntegrationProfile;
import gov.nist.hit.core.domain.TestCase;
import gov.nist.hit.core.domain.TestCaseGroup;
import gov.nist.hit.core.domain.TestPlan;
import gov.nist.hit.core.domain.TestStep;
import gov.nist.hit.core.domain.TestingStage;
import gov.nist.hit.core.domain.VocabularyLibrary;
import gov.nist.hit.core.repo.TestCaseGroupRepository;
import gov.nist.hit.core.repo.TestCaseRepository;
import gov.nist.hit.core.repo.TestStepRepository;
import gov.nist.hit.core.service.ResourceLoader;
import gov.nist.hit.core.service.exception.NotFoundException;
import gov.nist.hit.core.service.exception.ProfileParserException;
import gov.nist.hit.core.service.util.FileUtil;
import gov.nist.hit.core.service.util.ResourcebundleHelper;

public class HL7V2ResourceLoaderImpl extends HL7V2ResourcebundleLoaderImpl
		implements ResourceLoader {

	@Autowired
	private TestCaseRepository testCaseRepository;

	@Autowired
	private TestCaseGroupRepository testCaseGroupRepository;

	@Autowired
	private TestStepRepository testStepRepository;

	@Autowired
	@PersistenceContext(unitName = "base-tool")
	protected EntityManager entityManager;

	private String directory;

	@Override
	public void setDirectory(String dir) {
		this.directory = dir;
	}

	@Override
	public String getDirectory() {
		return this.directory;
	}

	@Override
	public List<Resource> getDirectories(String pattern) throws IOException {
		// System.out.println("GET DIRS " + directory + pattern);
		List<Resource> res = ResourcebundleHelper.getDirectoriesFile(directory
				+ pattern);
		// System.out.println(res.size());
		return res;
	}

	@Override
	public Resource getResource(String pattern) throws IOException {
		// System.out.println("GET RES " + directory + pattern);
		return ResourcebundleHelper.getResourceFile(directory + pattern);
	}

	@Override
	public List<Resource> getResources(String pattern) throws IOException {
		// System.out.println("GET RESS " + directory + pattern);
		return ResourcebundleHelper.getResourcesFile(directory + pattern);
	}

	@Override
	protected VocabularyLibrary getVocabularyLibrary(String id)
			throws IOException {
		return this.vocabularyLibraryRepository.findOneBySourceId(id);
	}

	@Override
	protected Constraints getConstraints(String id) throws IOException {
		return this.constraintsRepository.findOneBySourceId(id);
	}

	@Override
	protected IntegrationProfile getIntegrationProfile(String id)
			throws IOException {
		return this.integrationProfileRepository.findByMessageId(id);
	}

	// ----- Global -> ValueSet, Constraints, IntegrationProfile

	@Override
	public void addOrReplaceValueSet() throws IOException {
		System.out.println("AddOrReplace VS");

		List<Resource> resources = this.getResources("*.xml");
		if (resources != null && !resources.isEmpty()) {
			for (Resource resource : resources) {
				String content = FileUtil.getContent(resource);
				try {
					VocabularyLibrary vocabLibrary = vocabLibrary(content);

					VocabularyLibrary exist = this
							.getVocabularyLibrary(vocabLibrary.getSourceId());
					if (exist != null) {
						System.out.println("Replace");
						vocabLibrary.setId(exist.getId());
					} else {
						System.out.println("Add");
					}

					this.vocabularyLibraryRepository.save(vocabLibrary);

				} catch (UnsupportedOperationException e) {

				}
			}
		}

	}

	@Override
	public void addOrReplaceConstraints() throws IOException {
		System.out.println("AddOrReplace Constraints");

		List<Resource> resources = this.getResources("*.xml");
		if (resources != null && !resources.isEmpty()) {
			for (Resource resource : resources) {
				String content = FileUtil.getContent(resource);
				try {
					Constraints constraint = constraint(content);

					Constraints exist = this.getConstraints(constraint
							.getSourceId());
					if (exist != null) {
						System.out.println("Replace");
						constraint.setId(exist.getId());
					} else {
						System.out.println("Add");
					}

					this.constraintsRepository.save(constraint);

				} catch (UnsupportedOperationException e) {

				}
			}
		}

	}

	@Override
	public void addOrReplaceIntegrationProfile() throws IOException {
		System.out.println("AddOrReplace integration profile");

		List<Resource> resources = this.getResources("*.xml");
		if (resources != null && !resources.isEmpty()) {
			for (Resource resource : resources) {
				String content = FileUtil.getContent(resource);
				try {
					IntegrationProfile integrationP = integrationProfile(content);

					IntegrationProfile exist = this.integrationProfileRepository
							.findBySourceId(integrationP.getSourceId());
					if (exist != null) {
						System.out.println("Replace");
						integrationP.setId(exist.getId());
					} else {
						System.out.println("Add");
					}

					this.integrationProfileRepository.save(integrationP);

				} catch (UnsupportedOperationException e) {

				}
			}
		}
	}

	// ------ Context-Free test Case

	@Override
	public void addOrReplaceCFTestCase() throws IOException {
		System.out.println("AddOrReplace CFTestCase");

		List<Resource> resources = this.getDirectories("*");
		if (resources != null && !resources.isEmpty()) {
			for (Resource resource : resources) {
				String fileName = resource.getFilename();
				CFTestInstance testObject = testObject(fileName);
				if (testObject != null) {
					testObject.setRoot(true);
					testInstanceRepository.save(testObject);
				}
			}
		}
	}

	// ------ Context-Based test Case

	@Override
//	@Transactional
	public void handleTS(Long testCaseId, TestStep ts) throws NotFoundException {

		if (!this.testStepRepository.exists(ts.getId())) {
			System.out.println("Not Exists");
			TestCase tc = this.testCaseRepository.findOne(testCaseId);
			if (tc == null)
				throw new NotFoundException();
			tc.addTestStep(ts);
			this.testCaseRepository.save(tc);
		} else {
			System.out.println("Exists");
			TestStep ex = this.testStepRepository.findOne(ts.getId());
			ts.setTestCase(ex.getTestCase());
			this.testStepRepository.save(ts);
			
		}
		this.flush();
	}

	@Override
	public void handleTCg(Long testCaseGroup, TestCase tc)
			throws NotFoundException {

		if (!this.testCaseRepository.exists(tc.getId())) {
			TestCaseGroup tcg = this.testCaseGroupRepository
					.findOne(testCaseGroup);
			if (tcg == null)
				throw new NotFoundException();
			tcg.getTestCases().add(tc);
			this.testCaseGroupRepository.save(tcg);
		} else {
			TestCase existing = this.testCaseRepository.findOne(tc.getId());
			List<TestStep> merged = this.mergeTS(tc.getTestSteps(),
					existing.getTestSteps());
			tc.setDataMappings(existing.getDataMappings());
			tc.setTestSteps(merged);
			this.testCaseRepository.save(tc);
		}
		this.flush();
	}

	@Override
	public void handleTCG(Long testPlan, TestCaseGroup tcg)
			throws NotFoundException {

		if (!this.testCaseGroupRepository.exists(tcg.getId())) {
			TestPlan tp = this.testPlanRepository.findOne(testPlan);
			if (tp == null)
				throw new NotFoundException();
			tp.getTestCaseGroups().add(tcg);
			this.testPlanRepository.save(tp);
		} else {
			TestCaseGroup existing = this.testCaseGroupRepository.findOne(tcg
					.getId());
			List<TestCase> mergedTc = this.mergeTC(tcg.getTestCases(),
					existing.getTestCases());
			List<TestCaseGroup> mergedTcg = this.mergeTCG(
					tcg.getTestCaseGroups(), existing.getTestCaseGroups());
			tcg.setTestCases(mergedTc);
			tcg.setTestCaseGroups(mergedTcg);
			this.testCaseGroupRepository.save(tcg);
		}
		this.flush();
	}

	@Override
	public void handleTP(TestPlan tp) {

		if (!this.testPlanRepository.exists(tp.getId())) {
			this.testPlanRepository.save(tp);
		} else {
			TestPlan existing = this.testPlanRepository.findOne(tp.getId());
			List<TestCase> mergedTc = this.mergeTC(tp.getTestCases(),
					existing.getTestCases());
			List<TestCaseGroup> mergedTcg = this.mergeTCG(
					tp.getTestCaseGroups(), existing.getTestCaseGroups());
			tp.setTestCases(mergedTc);
			tp.setTestCaseGroups(mergedTcg);
			System.out.println("TestCaseGroups");
			for(TestCaseGroup tcg : mergedTcg){
				System.out.println(tcg.getId());
			}
			
			this.testPlanRepository.save(tp);
		}
		this.flush();
	}

	// ---- Helper Functions

	@Override
	public List<TestStep> mergeTS(List<TestStep> newL, List<TestStep> oldL) {
		int index = -1;
		List<TestStep> tmp = new ArrayList<TestStep>();
		tmp.addAll(oldL);

		for (TestStep tcs : newL) {

			if ((index = tmp.indexOf(tcs)) != -1) {
				tmp.set(index, tcs);
			} else
				tmp.add(tcs);
		}

		return tmp;
	}

	@Override
	public List<TestStep> createTS() throws IOException {
		System.out.println("Creating TS");
		List<TestStep> tmp = new ArrayList<TestStep>();
		List<Resource> resources = getDirectories("*");
		System.out.println("Directories found : " + resources.size());
		for (Resource resource : resources) {
			String fileName = resource.getFilename();
			System.out.println("Handling folder = " + fileName);
			TestStep testStep = testStep(fileName + "/", null, false);
			if (testStep != null) {
				tmp.add(testStep);
			}
		}
		return tmp;
	}

	@Override
	public List<TestCase> createTC() throws IOException {
		List<TestCase> tmp = new ArrayList<TestCase>();
		List<Resource> resources = getDirectories("*");
		for (Resource resource : resources) {
			String fileName = resource.getFilename();
			TestCase testCase = testCase(fileName + "/", null, false);
			if (testCase != null) {
				tmp.add(testCase);
			}
		}
		return tmp;
	}

	@Override
	public List<TestCaseGroup> createTCG() throws IOException {
		List<TestCaseGroup> tmp = new ArrayList<TestCaseGroup>();
		List<Resource> resources = getDirectories("*");
		for (Resource resource : resources) {
			String fileName = resource.getFilename();
			TestCaseGroup testCaseGroup = testCaseGroup(fileName +"/", null, false);
			if (testCaseGroup != null) {
				tmp.add(testCaseGroup);
			}
		}
		return tmp;
	}

	@Override
	public List<TestPlan> createTP() throws IOException {
		List<TestPlan> tmp = new ArrayList<TestPlan>();
		List<Resource> resources = getDirectories("*");
		for (Resource resource : resources) {
			String fileName = resource.getFilename();
			TestPlan testPlan = testPlan(fileName +"/", TestingStage.CB);
			if (testPlan != null) {
				tmp.add(testPlan);
			}
		}
		return tmp;
	}

	@Override
	public List<TestCase> mergeTC(List<TestCase> newL, List<TestCase> oldL) {
		int index = -1;
		List<TestCase> tmp = new ArrayList<TestCase>();
		tmp.addAll(oldL);

		for (TestCase tcs : newL) {

			if ((index = tmp.indexOf(tcs)) != -1) {
				List<TestStep> newLs = mergeTS(tcs.getTestSteps(),
						tmp.get(index).getTestSteps());
				tcs.setTestSteps(newLs);
				TestCase existing = tmp.get(index);
				tcs.setDataMappings(existing.getDataMappings());
				tmp.set(index, tcs);
			} else
				tmp.add(tcs);
		}
		return tmp;
	}

	@Override
	public List<TestCaseGroup> mergeTCG(List<TestCaseGroup> newL,
			List<TestCaseGroup> oldL) {
		int index = -1;
		List<TestCaseGroup> tmp = new ArrayList<TestCaseGroup>();
		tmp.addAll(oldL);

		for (TestCaseGroup tcs : newL) {

			if ((index = tmp.indexOf(tcs)) != -1) {
				List<TestCase> newLs = mergeTC(tcs.getTestCases(),
						tmp.get(index).getTestCases());
				tcs.setTestCases(newLs);
				if (tcs.getTestCaseGroups() != null
						&& tcs.getTestCaseGroups().size() > 0) {
					List<TestCaseGroup> newLsg = mergeTCG(
							tcs.getTestCaseGroups(), tmp.get(index)
									.getTestCaseGroups());
					tcs.setTestCaseGroups(newLsg);
				}
				tmp.set(index, tcs);
			} else
				tmp.add(tcs);
		}
		return tmp;
	}

	public void flush() {
		this.testStepRepository.flush();
		this.testCaseRepository.flush();
		this.testCaseGroupRepository.flush();
		this.testPlanRepository.flush();
	}

}
