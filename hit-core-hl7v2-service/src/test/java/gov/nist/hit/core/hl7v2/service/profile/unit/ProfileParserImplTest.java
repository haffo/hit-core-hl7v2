package gov.nist.hit.core.hl7v2.service.profile.unit;


public class ProfileParserImplTest {

  // ProfileParser parser = new ProfileParserImpl();
  //
  // @Test
  // public void testParse() throws ProfileParserException, IOException {
  // String profile =
  // IOUtils.toString(ProfileParserImplTest.class
  // .getResourceAsStream("/IZ_V1.5_ACK_Z23/IntegrationProfile.xml"));
  // String consStatements =
  // IOUtils.toString(ProfileParserImplTest.class
  // .getResourceAsStream("/IZ_V1.5_ACK_Z23/ConformanceStatementConstraints.xml"));
  // String predicates =
  // IOUtils.toString(ProfileParserImplTest.class
  // .getResourceAsStream("/IZ_V1.5_ACK_Z23/PredicateConstraints.xml"));
  // ProfileModel model = parser.parse(profile, consStatements, predicates);
  // List<ProfileElement> elements = model.getElements();
  // for (ProfileElement element : elements) {
  // printConstraints(element);
  // }
  //
  // }
  //
  // private void printConstraints(ProfileElement element) {
  // if (element.getConformanceStatements() != null &&
  // !element.getConformanceStatements().isEmpty()) {
  // System.out.println("--------------->Conformance Statements:" + element.getPath()
  // + "<-------------------");
  // for (Constraint co : element.getConformanceStatements()) {
  // System.out.println("Conformance Statement: " + co.getId() + "--" + co.getDescription());
  // }
  // }
  //
  // if (element.getPredicates() != null && !element.getPredicates().isEmpty()) {
  // System.out
  // .println("--------------->Predicates:" + element.getPath() + "<-------------------");
  // for (Constraint pre : element.getPredicates()) {
  // System.out.println("Predicate: " + pre.getId() + "--" + pre.getDescription());
  // }
  // }
  //
  // List<ProfileElement> elements = element.getChildren();
  // for (ProfileElement child : elements) {
  // printConstraints(child);
  // }
  //
  // }
}
