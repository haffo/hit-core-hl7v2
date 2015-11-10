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
import gov.nist.hit.core.domain.ProfileElement;
import gov.nist.hit.core.domain.ProfileModel;
import gov.nist.hit.core.hl7v2.domain.util.Util;
import gov.nist.hit.core.service.ProfileParser;
import gov.nist.hit.core.service.exception.ProfileParserException;
import hl7.v2.profile.Component;
import hl7.v2.profile.Composite;
import hl7.v2.profile.Datatype;
import hl7.v2.profile.Field;
import hl7.v2.profile.Group;
import hl7.v2.profile.Message;
import hl7.v2.profile.Profile;
import hl7.v2.profile.Range;
import hl7.v2.profile.Req;
import hl7.v2.profile.SegRefOrGroup;
import hl7.v2.profile.Segment;
import hl7.v2.profile.SegmentRef;
import hl7.v2.profile.Usage;
import hl7.v2.profile.ValueSetSpec;
import hl7.v2.profile.XMLDeserializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import scala.collection.Iterator;
import scala.collection.immutable.List;

/**
 * 
 * @author Harold Affo
 * 
 */
public abstract class HL7V2ProfileParser extends ProfileParser {

  public HL7V2ProfileParser() {}

  private final Map<String, Profile> cachedIntegrationProfilesMap = new HashMap<String, Profile>();
  private Map<String, ProfileElement> segmentsMap;
  private Map<String, ProfileElement> datatypesMap;
  private Map<String, ProfileElement> groupsMap;

  @Override
  /**
   * integrationProfileXml: integration profile xml content 
   * conformanceProfileId: conformance profile id
   * Options: constraints xml content
   */
  public ProfileModel parse(String integrationProfileXml, String conformanceProfileId,
      String... constraints) throws ProfileParserException {
    try {
      Profile p = null;
      String integrationProfileId = integrationProfileId(integrationProfileXml);
      if (cachedIntegrationProfilesMap.containsKey(integrationProfileId)) {
        p = cachedIntegrationProfilesMap.get(integrationProfileId);
      } else {
        InputStream profileStream = IOUtils.toInputStream(integrationProfileXml);
        p = XMLDeserializer.deserialize(profileStream).get();
        cachedIntegrationProfilesMap.put(integrationProfileId, p);
      }
      Message m = p.messages().apply(conformanceProfileId);
      return parse(m, constraints);
    } catch (Exception e) {
      e.printStackTrace();
      throw new ProfileParserException(e.getMessage());
    }
  }



  /**
   * TODO: Include additional Constraints
   * 
   * @param message
   * @param constraintsXml
   * @param additionalConstraintsXml
   * @return
   * @throws ProfileParserException
   */
  @Override
  public ProfileModel parse(Object conformanceProfile, String... constraints)
      throws ProfileParserException {
    try {
      if (!(conformanceProfile instanceof Message)) {
        throw new IllegalArgumentException("Conformance Profile is not a valid instanceof "
            + Message.class.getCanonicalName());
      }
      String constraintsXml = constraints != null && constraints.length > 0 ? constraints[0] : null;
      String additionalConstraintsXml =
          constraints != null && constraints.length > 1 ? constraints[1] : null;
      Message message = (Message) conformanceProfile;
      this.segmentsMap = new LinkedHashMap<String, ProfileElement>();
      this.datatypesMap = new LinkedHashMap<String, ProfileElement>();
      this.groupsMap = new LinkedHashMap<String, ProfileElement>();
      model = new ProfileModel();
      ProfileElement structure = new ProfileElement("Message Structure");
      structure.setType("MESSAGE");
      structure.setRelevent(true);
      scala.collection.immutable.List<SegRefOrGroup> children = message.structure();
      if (children != null && !children.isEmpty()) {
        Iterator<SegRefOrGroup> it = children.iterator();
        while (it.hasNext()) {
          process(it.next(), structure);
        }
      }
      model.getElements().add(structure);
      model.getElements().addAll(this.segmentsMap.values());
      ProfileElement datatypes = new ProfileElement("Datatypes");
      datatypes.setType("DATATYPE");
      datatypes.setRelevent(true);
      datatypes.getChildren().addAll(this.datatypesMap.values());
      Collections.sort(datatypes.getChildren(), new Comparator<ProfileElement>() {
        @Override
        public int compare(ProfileElement o1, ProfileElement o2) {
          return o1.getName().compareTo(o2.getName());
        }
      });
      model.getElements().add(datatypes);
      addConstraints(constraintsXml);
      addConstraints(additionalConstraintsXml);
      fixIconsAndTypes();
      return model;
    } catch (XPathExpressionException e) {
      throw new ProfileParserException(e.getLocalizedMessage());
    } catch (CloneNotSupportedException e) {
      throw new ProfileParserException(e.getLocalizedMessage());
    }
  }

  /**
   * 
   * @param ref
   * @param parentElement
   * @param model
   * @param segmentsMap
   * @throws XPathExpressionException
   */
  private ProfileElement process(SegRefOrGroup ref, ProfileElement parentElement)
      throws XPathExpressionException {
    if (ref == null)
      return parentElement;
    if (ref instanceof SegmentRef) {
      return process((SegmentRef) ref, ((SegmentRef) ref).req(), parentElement);
    } else if (ref instanceof Group) {
      return process((Group) ref, ((Group) ref).req(), parentElement);
    } else {
      throw new IllegalArgumentException("Unknown type of SegRefOrGroup");
    }
  }


  private ProfileElement process(SegmentRef ref, Req req, ProfileElement parentElement)
      throws XPathExpressionException {
    ProfileElement element = process(req, new ProfileElement(), parentElement);
    Segment s = ref.ref();
    element.setName(s.name());
    element.setType(TYPE_SEGMENT_REF);
    element.setLongName(s.desc());
    element.setIcon(ICON_SEGMENT);
    element.setParent(parentElement);
    element.setPosition(req.position() + "");
    ProfileElement pe = null;
    if (segmentsMap.containsKey(s.id())) {
      pe = segmentsMap.get(s.id());
    } else {
      pe = process(ref.ref(), ref.req());
      segmentsMap.put(pe.getId(), pe);
    }
    pe.setRelevent(pe.isRelevent() || element.isRelevent());
    element.setReference(new gov.nist.hit.core.domain.SegmentRef(pe.getId(), "Segment", pe
        .getName()));
    parentElement.getChildren().add(element);
    return element;
  }

  private boolean relevent(ProfileElement child, ProfileElement parent) {
    boolean isUsageRelevent = relevent(child.getUsage(), child.isHide());
    if (parent != null) {
      return isUsageRelevent && relevent(parent, parent.getParent());
    } else {
      return isUsageRelevent;
    }
  }

  private boolean relevent(String usage, boolean hide) {
    return usage == null || !hide || usage.equals("R") || usage.equals("RE") || usage.equals("C")
        || usage.startsWith("C");
  }

  /**
   * 
   * @param s
   * @param req
   * @param parentElement
   * @param model
   * @param segmentsMap
   * @return
   * @throws XPathExpressionException
   */
  private ProfileElement process(Segment s, Req req) throws XPathExpressionException {
    ProfileElement element = new ProfileElement();
    element.setName(s.name());
    element.setType(TYPE_SEGMENT);
    element.setLongName(s.desc());
    element.setIcon(ICON_SEGMENT);
    element.setId(s.id());

    scala.collection.immutable.List<Field> children = s.fields();
    if (children != null && !children.isEmpty()) {
      Iterator<Field> it = children.iterator();
      while (it.hasNext()) {
        process(it.next(), element);
      }
    }

    return element;
  }

  /**
   * 
   * @param g
   * @param req
   * @param parentElement
   * @param model
   * @param segmentsMap
   * @return
   * @throws XPathExpressionException
   */
  private ProfileElement process(Group g, Req req, ProfileElement parentElement)
      throws XPathExpressionException {
    ProfileElement element = process(req, new ProfileElement(), parentElement);
    element.setType(TYPE_GROUP);
    element.setIcon(ICON_GROUP);
    element.setName(g.name());
    element.setLongName(g.name());
    element.setParent(parentElement);
    element.setPosition(req.position() + "");
    element.setId(g.id());
    parentElement.getChildren().add(element);
    groupsMap.put(g.id(), element);
    scala.collection.immutable.List<SegRefOrGroup> children = g.structure();
    if (children != null) {
      Iterator<SegRefOrGroup> it = children.iterator();
      while (it.hasNext()) {
        process(it.next(), element);
      }
    }
    return element;
  }

  /**
   * 
   * @param req
   * @param element
   * @return
   */
  private ProfileElement process(Req req, ProfileElement element, ProfileElement parent) {
    if (req == null)
      return element;
    Range card = Util.getOption(req.cardinality());
    Usage usage = req.usage();
    if (usage != null) {
      element.setUsage(req.usage().toString());
    }
    if (card != null) {
      element.setMinOccurs(card.min());
      element.setMaxOccurs(card.max());
    }
    Range length = Util.getOption(req.length());
    if (length != null) {
      element.setMinLength(length.min() + "");
      element.setMaxLength(length.max());
    }

    boolean relevent = relevent(element, parent);
    element.setRelevent(relevent);

    return element;
  }

  /**
   * Fix icons and types of component and subcomponents
   * 
   * @throws CloneNotSupportedException
   */
  private void fixIconsAndTypes() throws CloneNotSupportedException {
    Collection<ProfileElement> segments = segmentsMap.values();
    java.util.Iterator<ProfileElement> it = segments.iterator();
    while (it.hasNext()) {
      ProfileElement segment = it.next();
      if (segment.getChildren() != null && segment.getChildren().size() > 0) {
        for (int j = 0; j < segment.getChildren().size(); j++) {
          fixIconsAndTypes(segment.getChildren().get(j));
        }
      }
    }

  }



  private void fixIconsAndTypes(ProfileElement fieldOrComponent) throws CloneNotSupportedException {
    ArrayList<ProfileElement> newChildren = new ArrayList<ProfileElement>();
    java.util.List<ProfileElement> children = fieldOrComponent.getChildren();
    fieldOrComponent.setChildren(newChildren);
    if (fieldOrComponent.getType().equals(TYPE_FIELD)) {
      for (int i = 0; i < children.size(); i++) {
        ProfileElement child = children.get(i).clone();
        child.setType(TYPE_COMPONENT);
        child.setIcon(ICON_COMPONENT);
        child.setPath(fieldOrComponent.getPath() + "." + child.getPosition());
        newChildren.add(child);
        fixIconsAndTypes(child);
      }
    } else {
      for (int i = 0; i < children.size(); i++) {
        ProfileElement child = children.get(i).clone();
        child.setType(TYPE_SUBCOMPONENT);
        child.setIcon(ICON_SUBCOMPONENT);
        child.setPath(fieldOrComponent.getPath() + "." + child.getPosition());
        newChildren.add(child);
        fixIconsAndTypes(child);
      }
    }
  }

  /**
   * 
   * @param f
   * @param parentElement
   * @throws XPathExpressionException
   */
  private void process(Field f, ProfileElement parent) throws XPathExpressionException {
    if (f == null)
      return;
    ProfileElement element = process(f.req(), new ProfileElement(), parent);
    element.setName(f.name());
    element.setType(TYPE_FIELD);
    element.setIcon(ICON_FIELD);
    element.setParent(parent);
    String table = table(f.req());
    if (table != null) {
      element.setTable(table);
    }
    element.setDataType(f.datatype().id()); // use id for flavors
    element.setPosition(f.req().position() + "");
    element.setPath(parent.getName() + "-" + f.req().position());
    parent.getChildren().add(element);
    ProfileElement datatypeElement = process(f.datatype());
    element.setChildren(datatypeElement.getChildren());
  }

  private String table(Req req) {
    List<ValueSetSpec> vsSpec = req.vsSpec();
    if (vsSpec != null && !vsSpec.isEmpty()) {
      Iterator<ValueSetSpec> it = vsSpec.iterator();
      while (it.hasNext()) {
        return it.next().valueSetId();
      }
    }

    return null;
  }


  private ProfileElement process(Datatype d) throws XPathExpressionException {
    if (!datatypesMap.containsKey(d.id())) {
      ProfileElement element = new ProfileElement();
      element.setId(d.id());
      element.setName(d.name());
      element.setLongName(d.desc());
      element.setType(TYPE_DATATYPE);
      element.setIcon(ICON_DATATYPE);
      element.setRelevent(true);
      datatypesMap.put(d.id(), element);
      if (d instanceof Composite) {
        Composite c = (Composite) d;
        scala.collection.immutable.List<Component> children = c.components();
        if (children != null) {
          Iterator<Component> it = children.iterator();
          while (it.hasNext()) {
            process(it.next(), element);
          }
        }
      }

      return element;
    } else {
      return datatypesMap.get(d.id());
    }

  }

  private ProfileElement process(Component c, ProfileElement parent)
      throws XPathExpressionException {
    if (c == null)
      return parent;
    ProfileElement element = new ProfileElement();
    process(c.req(), element, parent);
    element.setName(c.name());
    element.setType(parent.getType().equals(TYPE_FIELD) ? TYPE_COMPONENT : TYPE_SUBCOMPONENT);
    element.setIcon(parent.getType().equals(TYPE_FIELD) ? ICON_COMPONENT : ICON_SUBCOMPONENT);
    String table = table(c.req());
    if (table != null)
      element.setTable(table);
    element.setDataType(c.datatype().id());
    element.setPosition(c.req().position() + "");
    element.setParent(parent);
    element.setPath(parent.getPath() + "." + c.req().position());
    parent.getChildren().add(element);

    ProfileElement datatypeElement = process(c.datatype());
    element.setChildren(datatypeElement.getChildren());
    return element;
  }



  private Document toDoc(String xmlSource) {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    factory.setIgnoringComments(false);
    factory.setIgnoringElementContentWhitespace(true);
    DocumentBuilder builder;
    try {
      builder = factory.newDocumentBuilder();
      return builder.parse(new InputSource(new StringReader(xmlSource)));
    } catch (ParserConfigurationException e) {
      e.printStackTrace();
    } catch (SAXException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  public String integrationProfileId(String xml) {
    Document doc = this.toDoc(xml);
    Element elmIntegrationProfile =
        (Element) doc.getElementsByTagName("ConformanceProfile").item(0);
    return elmIntegrationProfile.getAttribute("ID");
  }


  private Predicate predicate(Element element) {
    String id = element.getAttribute("ID");
    String trueUsage = element.getAttribute("TrueUsage");
    String falseUsage = element.getAttribute("FalseUsage");
    String desc = element.getElementsByTagName("Description").item(0).getTextContent();
    return new Predicate(id, desc, trueUsage, falseUsage);
  }

  private Constraint conformanceStatement(Element element) {
    String id = element.getAttribute("ID");
    String desc = element.getElementsByTagName("Description").item(0).getTextContent();
    return new Constraint(id, desc);
  }


  public void addPredicates(Element root, String type, Map<String, ProfileElement> map) {
    NodeList rootChildList = root.getElementsByTagName(type);
    if (rootChildList != null && rootChildList.getLength() > 0) {
      Element rootChildElement = (Element) rootChildList.item(0);
      NodeList children = rootChildElement.getElementsByTagName("ByID");
      if (children != null && children.getLength() > 0) {
        for (int i = 0; i < children.getLength(); i++) {
          Element child = (Element) children.item(i);
          String id = child.getAttribute("ID");
          ProfileElement element = findElementById(id, map);
          if (element != null) {
            NodeList predicatesNodes = child.getElementsByTagName("Predicate");
            if (predicatesNodes != null && predicatesNodes.getLength() > 0) {
              for (int j = 0; j < predicatesNodes.getLength(); j++) {
                Element node = (Element) predicatesNodes.item(j);
                String target = node.getAttribute("Target");
                ProfileElement found = findElementByTarget(target, element);
                if (found != null) {
                  found.getPredicates().add(predicate(node));
                }
              }
            }
          }
        }
      }

      children = rootChildElement.getElementsByTagName("ByName");
      if (children != null && children.getLength() > 0) {
        for (int i = 0; i < children.getLength(); i++) {
          Element child = (Element) children.item(i);
          String name = child.getAttribute("Name");
          ProfileElement element = findElementByName(name, map);
          if (element != null) {
            NodeList predicatesNodes = child.getElementsByTagName("Predicate");
            if (predicatesNodes != null && predicatesNodes.getLength() > 0) {
              for (int j = 0; j < predicatesNodes.getLength(); j++) {
                Element node = (Element) predicatesNodes.item(j);
                String target = node.getAttribute("Target");
                ProfileElement found = findElementByTarget(target, element);
                if (found != null) {
                  found.getPredicates().add(predicate(node));
                }
              }
            }
          }
        }
      }
    }
  }

  public void addPredicates(Element root) {
    addPredicates(root, "Datatype", datatypesMap);
    addPredicates(root, "Segment", segmentsMap);
    addPredicates(root, "Group", groupsMap);
  }

  public void addConformanceStatements(Element root) {
    addConformanceStatements(root, "Datatype", datatypesMap);
    addConformanceStatements(root, "Segment", segmentsMap);
    addConformanceStatements(root, "Group", groupsMap);
  }



  public void addConformanceStatements(Element root, String type, Map<String, ProfileElement> map) {
    NodeList rootChildList = root.getElementsByTagName(type);
    if (rootChildList != null && rootChildList.getLength() > 0) {
      Element rootChildElement = (Element) rootChildList.item(0);
      NodeList children = rootChildElement.getElementsByTagName("ByID");
      if (children != null && children.getLength() > 0) {
        for (int i = 0; i < children.getLength(); i++) {
          Element child = (Element) children.item(i);
          String id = child.getAttribute("ID");
          ProfileElement element = findElementById(id, map);
          if (element != null) {
            NodeList predicatesNodes = child.getElementsByTagName("Constraint");
            if (predicatesNodes != null && predicatesNodes.getLength() > 0) {
              for (int j = 0; j < predicatesNodes.getLength(); j++) {
                Element node = (Element) predicatesNodes.item(j);
                String target = node.getAttribute("Target");
                ProfileElement found = findElementByTarget(target, element);
                if (found != null) {
                  found.getConformanceStatements().add(conformanceStatement(node));
                }
              }
            }
          }
        }
      }

      children = rootChildElement.getElementsByTagName("ByName");
      if (children != null && children.getLength() > 0) {
        for (int i = 0; i < children.getLength(); i++) {
          Element child = (Element) children.item(i);
          String name = child.getAttribute("Name");
          ProfileElement element = findElementByName(name, map);
          if (element != null) {
            NodeList predicatesNodes = child.getElementsByTagName("Constraint");
            if (predicatesNodes != null && predicatesNodes.getLength() > 0) {
              for (int j = 0; j < predicatesNodes.getLength(); j++) {
                Element node = (Element) predicatesNodes.item(j);
                String target = node.getAttribute("Target");
                ProfileElement found = findElementByTarget(target, element);
                if (found != null) {
                  found.getConformanceStatements().add(conformanceStatement(node));
                }
              }
            }
          }
        }
      }
    }

  }


  public void addConstraints(String constraintXml) {
    try {
      if (constraintXml != null && !"".equals(constraintXml)) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(IOUtils.toInputStream(constraintXml));
        Element context = (Element) doc.getElementsByTagName("ConformanceContext").item(0);
        NodeList predicatesList = context.getElementsByTagName("Predicates");
        if (predicatesList != null && predicatesList.getLength() > 0) {
          addPredicates((Element) predicatesList.item(0));
        }

        NodeList constraintsList = context.getElementsByTagName("Constraints");
        if (constraintsList != null && constraintsList.getLength() > 0) {
          addConformanceStatements((Element) constraintsList.item(0));
        }
      }
    } catch (ParserConfigurationException | SAXException | IOException e) {
      throw new RuntimeException(e);
    }
  }


  private ProfileElement findElement(java.util.List<Integer> positions, ProfileElement element) {
    if (!positions.isEmpty()) {
      int curPos = positions.get(0);
      ProfileElement child1 = null;
      if (element.getType().equals(TYPE_GROUP) || element.getType().equals(TYPE_SEGMENT)
          || element.getType().equals(TYPE_FIELD) || element.getType().equals(TYPE_COMPONENT)
          || element.getType().equals(TYPE_DATATYPE)) {
        child1 = element.getChildren().get(curPos - 1);
      } else if (element.getType().equals(TYPE_SEGMENT_REF)) {
        ProfileElement segmentElement = segmentsMap.get(element.getReference().getId());
        child1 = segmentElement.getChildren().get(curPos - 1);
      }
      positions.remove(0);
      if (!positions.isEmpty()) {
        return findElement(positions, child1);
      }
      return child1;
    }
    return null;
  }

  private java.util.List<Integer> positions(String target) {
    ArrayList<Integer> positions = new ArrayList<Integer>();
    String[] posis = target.split(Pattern.quote("."));
    for (int i = 0; i < posis.length; i++) {
      String t = posis[i].split(Pattern.quote("["))[0];
      positions.add(Integer.valueOf(t));
    }
    return positions;
  }



  private ProfileElement findElementByTarget(String target, ProfileElement element) {
    if (target != null && !"".equals(target)) {
      java.util.List<Integer> positions = positions(target);
      ProfileElement found = findElement(positions, element);
      return found;
    }
    return null;
  }

  private String shortTarget(ProfileElement element) {
    return element != null && element.getPosition() != null ? element.getPosition() + "[1]" : null;
  }

  private String mediumTarget(ProfileElement element) {
    String pTarget = shortTarget(element.getParent());
    return pTarget != null ? pTarget + "." + shortTarget(element) : shortTarget(element);
  }

  private String longTarget(ProfileElement element) {
    String pTarget = mediumTarget(element.getParent());
    return pTarget != null ? pTarget + "." + shortTarget(element) : shortTarget(element);
  }


  private ProfileElement findElementByName(String name, Map<String, ProfileElement> map) {
    for (ProfileElement child : map.values()) {
      if (name.equals(child.getName())) {
        return child;
      }
    }
    return null;
  }

  private ProfileElement findElementById(String id, Map<String, ProfileElement> map) {
    return map.get(id);
  }



}
