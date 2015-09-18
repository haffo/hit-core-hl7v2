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
package gov.nist.hit.core.hl7v2.service;

import gov.nist.hit.core.domain.Constraint;
import gov.nist.hit.core.domain.Predicate;

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
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        doc = builder.parse(IOUtils.toInputStream(constraintXml));
      }
    } catch (ParserConfigurationException | SAXException | IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }


  public ConstraintManager(String constraintXml, String additionalConstraintXml) {
    try {
      if (constraintXml != null) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        doc = builder.parse(IOUtils.toInputStream(constraintXml));
      }
    } catch (ParserConfigurationException | SAXException | IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

  public Set<Constraint> findConfStatementsByIdAndPath(String type, String id, String targetPath)
      throws XPathExpressionException {
    return findConstraints("/ConformanceContext/Constraints/" + type + "/ByID[@ID='" + id
        + "']/Constraint[@Target='" + targetPath + "']");
  }

  public Set<Constraint> findConfStatementsByNameAndPath(String type, String name, String targetPath)
      throws XPathExpressionException {
    return findConstraints("/ConformanceContext/Constraints/" + type + "/ByName[@Name='" + name
        + "']/Constraint[@Target='" + targetPath + "']");
  }

  public Set<Predicate> findPredicatesByIdAndTarget(String type, String id, String targetPath)
      throws XPathExpressionException {
    return findPredicates("/ConformanceContext/Predicates/" + type + "/ByID[@ID='" + id
        + "']/Predicate[@Target='" + targetPath + "']");
  }

  public Set<Predicate> findPredicatesByNameAndTarget(String type, String name, String targetPath)
      throws XPathExpressionException {
    return findPredicates("/ConformanceContext/Predicates/" + type + "/ByName[@Name='" + name
        + "']/Predicate[@Target='" + targetPath + "']");
  }

  //
  // public Set<Constraint> findByNameAndPath(String type, String name,String
  // path)
  // throws XPathExpressionException {
  // return findConstraints("/ConformanceContext/"+ type +"/ByName[@Name='" +
  // name + "']/*[descendant::*[@Path='"
  // + path + "']]");
  // }

  public Set<Constraint> findConstraints(String query) throws XPathExpressionException {
    Set<Constraint> constraints = new HashSet<Constraint>();
    NodeList nl = find(query);
    if (nl != null && nl.getLength() > 0) {
      for (int i = 0; i < nl.getLength(); i++) {
        Node node = nl.item(i);
        if ("Constraint".equals(node.getNodeName())) {
          Constraint c = getConstraint(node);
          constraints.add(c);
        }
      }
    }
    return constraints;
  }

  public Set<Predicate> findPredicates(String query) throws XPathExpressionException {
    Set<Predicate> predicates = new HashSet<Predicate>();
    NodeList nl = find(query);
    if (nl != null && nl.getLength() > 0) {
      for (int i = 0; i < nl.getLength(); i++) {
        Node node = nl.item(i);
        if ("Predicate".equals(node.getNodeName())) {
          Predicate c = getPredicate(node);
          predicates.add(c);
        }
      }
    }
    return predicates;
  }

  public NodeList find(String query) throws XPathExpressionException {
    if (doc != null) {
      XPathFactory xPathfactory = XPathFactory.newInstance();
      XPath xpath = xPathfactory.newXPath();
      XPathExpression expr = xpath.compile(query); // FIX ME. Escape
      // single and double
      // quote from query
      return (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
    }
    return null;
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

  private Predicate getPredicate(Node node) {
    String id = getAttributeValue(node, "ID");
    if (id == null)
      throw new IllegalArgumentException("ID is null");
    String desc = getChildContent(node, "Description");
    String trueUsage = getAttributeValue(node, "TrueUsage");
    String falseUsage = getAttributeValue(node, "FalseUsage");
    // if (desc == null)
    // throw new IllegalArgumentException("Description is null");
    return new Predicate(id, desc, trueUsage, falseUsage);
  }

  // private String concat(String query)
  // {
  // String returnString = "";
  // String searchString = query;
  // char[] quoteChars = new char[] { '\'', '"' };
  //
  // int quotePos = searchString.indexOf(String.valueOf(quoteChars));
  // if (quotePos == -1)
  // {
  // returnString = "'" + searchString + "'";
  // }
  // else
  // {
  // returnString = "concat(";
  // while (quotePos != -1)
  // {
  // String subString = searchString.substring(0, quotePos);
  // returnString += "'" + subString + "', ";
  // if (searchString.substring(quotePos, 1) == "'")
  // {
  // returnString += "\"'\", ";
  // }
  // else
  // {
  // //must be a double quote
  // returnString += "'\"', ";
  // }
  // searchString = searchString.substring(quotePos + 1,
  // searchString.length() - quotePos - 1);
  // quotePos = searchString.indexOf(String.valueOf(quoteChars));
  // }
  // returnString += "'" + searchString + "')";
  // }
  // return returnString;
  // }
  //
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
