package gov.nist.healthcare.tools.core.services.hl7.v2.profile;

import gov.nist.healthcare.tools.core.models.Constraint;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * 
 * @author Harold Affo
 * 
 */
public class ConstraintManager {

	Document doc = null;

	public ConstraintManager(String constraintXml) {
		try {
			if (constraintXml != null) {
				DocumentBuilderFactory factory = DocumentBuilderFactory
						.newInstance();
				DocumentBuilder builder = factory.newDocumentBuilder();
				doc = builder.parse(IOUtils.toInputStream(constraintXml));
			}
		} catch (ParserConfigurationException | SAXException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public Set<Constraint> findById(String type, String id)
			throws XPathExpressionException {
		return find("/ConformanceContext/" + type
				+ "/ByID[@ID='" + id + "']/Constraint");
	}

	public Set<Constraint> findByName(String type, String name)
			throws XPathExpressionException {
		return find("/ConformanceContext/" + type
				+ "/ByName[@Name='" + name + "']/Constraint");
	}

	public Set<Constraint> findByIdAndPath(String type, String id,String path)
			throws XPathExpressionException {
		return find("/ConformanceContext/"+ type +"/ByID[@ID='" + id + "']/*[descendant::*[@Path='"
				+ path + "']]");
	}
	
	
	public Set<Constraint> findByNameAndPath(String type, String name,String path)
			throws XPathExpressionException {
		System.out.println("Type=" + type + " , name="+ name + " ,path=" + path);
		return find("/ConformanceContext/"+ type +"/ByName[@Name='" + name + "']/*[descendant::*[@Path='"
				+ path + "']]");
	} 
	
	
	public Set<Constraint> find(String query)
			throws XPathExpressionException {
		Set<Constraint> constraints = new HashSet<Constraint>();
		if (doc != null) {
			XPathFactory xPathfactory = XPathFactory.newInstance();
			XPath xpath = xPathfactory.newXPath();
			XPathExpression expr = xpath.compile(query);
			NodeList nl = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
			if (nl != null && nl.getLength() > 0) {
				for (int i = 0; i < nl.getLength(); i++) {
					Node node = nl.item(i);
					if ("Constraint".equals(node.getNodeName())) {
						Constraint c = getConstraint(node);
						constraints.add(c);
					}
				}
			}
		}
		return constraints;
	}
	
	

	private Constraint getConstraint(Node node) {
		String id = getAttributeValue(node, "ID");
		if (id == null)
			throw new IllegalArgumentException("ID is null");
		String desc = getChildContent(node, "Description");
		// if (desc == null)
		// throw new IllegalArgumentException("Description is null");
		return new Constraint(id, desc);
	}

	private String getAttributeValue(Node node, String attName) {
		NamedNodeMap attrs = node.getAttributes();
		if (attrs != null) {
			Node attrNode = attrs.getNamedItem(attName);
			return attrNode != null ? attrNode.getNodeValue() : null;
		}
		return null;
	}

	private String getChildContent(Node node, String childName) {
		NodeList children = node.getChildNodes();
		if (children != null && children.getLength() > 0) {
			for (int index = 0; index < children.getLength(); index++) {
				Node child = children.item(index);
				if (child.getNodeName().equals(childName)) {
					return child.getTextContent();
				}
			}
		}
		return null;
	}

}
