package gov.nist.healthcare.tools.core.services.hl7.v2.message;

import hl7.v2.validation.vs.ValueSet;

import javax.xml.xpath.XPathExpressionException;

import scala.collection.immutable.Map;
import scala.collection.immutable.Map$;

public class ValueSetLibGenerator {
	
 	
 
	public static Map<String, ValueSet> getValueSetLib(String valueSetsXml)
			throws XPathExpressionException { 
		Map<String, ValueSet> valueSetLib =Map$.MODULE$.empty();
		
		
//		try {
//			if (valueSetsXml != null) {
//				DocumentBuilderFactory factory = DocumentBuilderFactory
//						.newInstance();
//				DocumentBuilder builder = factory.newDocumentBuilder();
//				Document doc = builder.parse(IOUtils.toInputStream(valueSetsXml));
//				
//				String query = "/TableLibrary/TableDefinition/";
//				Set<Constraint> constraints = new HashSet<Constraint>();
//				if (doc != null) {
//					XPathFactory xPathfactory = XPathFactory.newInstance();
//					XPath xpath = xPathfactory.newXPath();
//					XPathExpression expr = xpath.compile(query);
//					NodeList nl = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
//					if (nl != null && nl.getLength() > 0) {
//						for (int i = 0; i < nl.getLength(); i++) {
//							Node node = nl.item(i);
//							NamedNodeMap attrs = node.getAttributes();
//							String id = attrs.getNamedItem("Id").getLocalName();
//							java.awt.List codes =  new java.awt.List();
//							NodeList children = node.getChildNodes();
//							if (children != null && children.getLength() > 0) {
//								for (int j = 0; j < children.getLength(); j++) {
//									Node codeNode = children.item(i);
//									NamedNodeMap codeAttrs = codeNode.getAttributes();
//									String code = codeAttrs.getNamedItem("Code").getLocalName();
//									String displayName = codeAttrs.getNamedItem("DisplayName").getLocalName();
//									String codesys = codeAttrs.getNamedItem("Codesys").getLocalName();
//									String source = codeAttrs.getNamedItem("Source").getLocalName();
//									Code c = new Code(code,displayName,CodeUsage.R$.MODULE$,codesys);
//		 						}
//							}
////							ValueSet valueSet = new ValueSet(id, Extensibility.Closed$.MODULE$, Stability.Static$.MODULE$,codes);
////							valueSetLib.
//							
//							 
//						}
//					}
//				}
//			}
//		} catch (ParserConfigurationException | SAXException | IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		

		return valueSetLib;
	}
	
}
