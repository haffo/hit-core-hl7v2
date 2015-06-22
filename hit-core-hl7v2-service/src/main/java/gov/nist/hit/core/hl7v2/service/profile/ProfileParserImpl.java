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
package gov.nist.hit.core.hl7v2.service.profile;

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

import java.io.InputStream;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import scala.collection.Iterator;
import scala.collection.immutable.List;

/**
 * 
 * @author Harold Affo
 * 
 */
@Service
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ProfileParserImpl implements ProfileParser {

  private final static String TYPE_GROUP = "GROUP";
  private final static String TYPE_DT = "DATATYPE";
  private final static String TYPE_SEGMENT = "SEGMENT";
  private final static String TYPE_FIELD = "FIELD";
  private final static String TYPE_COMPONENT = "COMPONENT";
  private final static String NODE_SEGMENT = "Segment";
  private final static String NODE_DATATYPE = "Datatype";
  private final static String NODE_GROUP = "Group";
  private final static String TYPE_SUBCOMPONENT = "SUBCOMPONENT";

  private final static String ICON_GROUP = "group.png";
  private final static String ICON_SEGMENT = "segment.png";
  private final static String ICON_FIELD = "field.png";
  private final static String ICON_DATATYPE = ICON_FIELD;
  private final static String ICON_COMPONENT = "component.png";
  private final static String ICON_SUBCOMPONENT = "subcomponent.png";

  private ConstraintManager constraintManager;

  private ProfileModel model;
  private Map<String, ProfileElement> segmentTracker;
  private Map<String, ProfileElement> datatypeTracker;

  public ProfileParserImpl() {}

  @Override
  /**
   * TODO: we are only parsing one message. 
   * Determine if we should parse all messages in a profile.
   * 
   */
  public ProfileModel parse(String content, Object... options) throws ProfileParserException {
    try {
      String constraintsXml = options != null && options.length > 0 ? (String) options[0] : null;
      InputStream profileStream = IOUtils.toInputStream(content);
      Profile p = XMLDeserializer.deserialize(profileStream).get();
      scala.collection.Iterable<String> keys = p.messages().keys();
      String key = keys.iterator().next();
      hl7.v2.profile.Message m = p.messages().apply(key);
      return parse(m, constraintsXml);
    } catch (Exception e) {
      e.printStackTrace();
      throw new ProfileParserException(e.getMessage());
    }
  }

  public ProfileModel parse(Message message, String constraintsXml) throws XPathExpressionException {
    this.constraintManager = new ConstraintManager(constraintsXml);
    this.segmentTracker = new LinkedHashMap<String, ProfileElement>();
    this.datatypeTracker = new LinkedHashMap<String, ProfileElement>();
    model = new ProfileModel();
    ProfileElement structure = new ProfileElement("Message Structure");
    structure.setType("MESSAGE");
    structure.setRelevent(true);
    structure.setConstraintPath(null);
    scala.collection.immutable.List<SegRefOrGroup> children = message.structure();
    if (children != null && !children.isEmpty()) {
      Iterator<SegRefOrGroup> it = children.iterator();
      while (it.hasNext()) {
        process(it.next(), structure);
      }
    }
    model.getElements().add(structure);
    model.getElements().addAll(this.segmentTracker.values());

    ProfileElement datatypes = new ProfileElement("Datatypes");
    datatypes.setType("DATATYPE");
    datatypes.setRelevent(true);
    datatypes.setConstraintPath(null);
    datatypes.getChildren().addAll(this.datatypeTracker.values());
    model.getElements().add(datatypes);
    return model;
  }

  /**
   * 
   * @param ref
   * @param parentElement
   * @param model
   * @param segmentTracker
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

  private Set<Constraint> findConfStatements(String type, String id, String name,
      String constraintPath) throws XPathExpressionException {
    Set<Constraint> constraints = new HashSet<Constraint>();
    if (id != null)
      constraints.addAll(constraintManager.findConfStatementsByIdAndPath(type, id, constraintPath));
    if (name != null)
      constraints.addAll(constraintManager.findConfStatementsByNameAndPath(type, name,
          constraintPath));
    return constraints;
  }

  private Set<Predicate> findPredicates(String type, String id, String name, String constraintPath)
      throws XPathExpressionException {
    Set<Predicate> constraints = new HashSet<Predicate>();
    if (id != null)
      constraints.addAll(constraintManager.findPredicatesByIdAndTarget(type, id, constraintPath));
    if (name != null)
      constraints.addAll(constraintManager
          .findPredicatesByNameAndTarget(type, name, constraintPath));
    return constraints;
  }

  private ProfileElement process(SegmentRef ref, Req req, ProfileElement parentElement)
      throws XPathExpressionException {
    ProfileElement element = process(req, new ProfileElement(), parentElement);
    Segment s = ref.ref();
    element.setName(s.name());
    element.setType(TYPE_SEGMENT);
    element.setLongName(s.desc());
    element.setIcon(ICON_SEGMENT);
    element.setParent(parentElement);
    element.setConstraintPath(ref.req().position() + "[1]");

    // relative to itself
    String constraintPath = element.getConstraintPath();
    element.getConformanceStatements().addAll(
        findConfStatements(NODE_GROUP, parentElement.getId(), parentElement.getName(),
            constraintPath));
    element.getPredicates().addAll(
        findPredicates(NODE_GROUP, parentElement.getId(), parentElement.getName(), constraintPath));
    // relative to its parent
    if (TYPE_GROUP.equals(parentElement.getType())) {
      constraintPath = parentElement.getConstraintPath() + "." + constraintPath;
      element.getConformanceStatements().addAll(
          findConfStatements(NODE_GROUP, parentElement.getId(), parentElement.getName(),
              constraintPath));
      element.getPredicates()
          .addAll(
              findPredicates(NODE_GROUP, parentElement.getId(), parentElement.getName(),
                  constraintPath));
    }

    ProfileElement pe = null;
    if (segmentTracker.containsKey(s.id())) {
      pe = segmentTracker.get(s.id());
    } else {
      pe = process(ref.ref(), ref.req());
      segmentTracker.put(pe.getId(), pe);
    }
    pe.setRelevent(pe.isRelevent() || element.isRelevent());
    element.setReference(new gov.nist.hit.core.domain.SegmentRef(pe.getId(), "Segment", pe
        .getName()));
    parentElement.getChildren().add(element);

    return element;
  }

  private boolean relevent(ProfileElement child, ProfileElement parent) {
    if (parent != null) {
      return relevent(child.getUsage()) && relevent(parent, parent.getParent());
    } else {
      return relevent(child.getUsage());
    }
  }

  private boolean relevent(String usage) {
    return usage == null || usage.equals("R") || usage.equals("RE") || usage.equals("C")
        || usage.startsWith("C");
  }

  /**
   * 
   * @param s
   * @param req
   * @param parentElement
   * @param model
   * @param segmentTracker
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
   * @param segmentTracker
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
    element.setConstraintPath(req.position() + "[1]");

    // relative to parent
    String constraintPath = element.getConstraintPath();
    element.getConformanceStatements().addAll(
        findConfStatements(NODE_GROUP, parentElement.getId(), parentElement.getName(),
            constraintPath));
    element.getPredicates().addAll(
        findPredicates(NODE_GROUP, parentElement.getId(), parentElement.getName(), constraintPath));

    parentElement.getChildren().add(element);
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
    if (table != null)
      element.setTable(table);
    element.setDataType(f.datatype().id()); // use id for flavors
    element.setPosition(f.req().position() + "");
    element.setPath(parent.getName() + "." + f.req().position());
    String constraintPath = f.req().position() + "[1]";
    element.setConstraintPath(constraintPath);

    element.getConformanceStatements().addAll(
        findConfStatements(NODE_SEGMENT, parent.getId(), parent.getName(), constraintPath));
    element.getPredicates().addAll(
        findPredicates(NODE_SEGMENT, parent.getId(), parent.getName(), constraintPath));

    parent.getChildren().add(element);
    process(f.datatype(), element);
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

  /**
   * 
   * @param d
   * @param parentElement
   * @throws XPathExpressionException
   */
  @SuppressWarnings("unchecked")
  private ProfileElement process(Datatype d, ProfileElement fieldOrComponent)
      throws XPathExpressionException {
    if (d instanceof Composite) {
      Composite c = (Composite) d;
      scala.collection.immutable.List<Component> children = c.components();
      if (children != null) {
        Iterator<Component> it = children.iterator();
        while (it.hasNext()) {
          ProfileElement componentElement = process(it.next(), d, fieldOrComponent);
          String segmentId = fieldOrComponent.getParent().getId();
          String segmentName = fieldOrComponent.getParent().getName();
          String constraintPath =
              fieldOrComponent.getConstraintPath() + "." + componentElement.getConstraintPath();
          componentElement.getConformanceStatements().addAll(
              findConfStatements(NODE_SEGMENT, segmentId, null, constraintPath));
          componentElement.getPredicates().addAll(
              findPredicates(NODE_SEGMENT, segmentId, null, constraintPath));

          if (fieldOrComponent.getType().equals(TYPE_COMPONENT)) {
            segmentId = fieldOrComponent.getParent().getParent().getId();
            segmentName = fieldOrComponent.getParent().getParent().getName();
            constraintPath =
                fieldOrComponent.getParent().getConstraintPath() + "."
                    + fieldOrComponent.getConstraintPath() + "." + componentElement.getPosition()
                    + "[1]";
            componentElement.getConformanceStatements().addAll(
                findConfStatements(NODE_SEGMENT, segmentId, null, constraintPath));
            componentElement.getPredicates().addAll(
                findPredicates(NODE_SEGMENT, segmentId, null, constraintPath));
          }
        }
      }
    }

    if (!datatypeTracker.containsKey(d.id())) {
      ProfileElement element = new ProfileElement();
      element.setId(d.id());
      element.setName(d.name());
      element.setLongName(d.desc());
      element.setType(TYPE_DT);
      element.setIcon(ICON_DATATYPE);
      element.setRelevent(true);
      element.getChildren().addAll(fieldOrComponent.getChildren());
      datatypeTracker.put(d.id(), element);
    }

    return fieldOrComponent;
  }

  private void setSubComponentTypes(ProfileElement subComponent) {
    subComponent.setType(TYPE_SUBCOMPONENT);
    subComponent.setIcon(ICON_SUBCOMPONENT);
    if (subComponent.getChildren() != null && !subComponent.getChildren().isEmpty()) {
      for (ProfileElement child : subComponent.getChildren()) {
        setSubComponentTypes(child);
      }
    }
  }

  /**
   * 
   * @param c
   * @param parentElement
   * @throws XPathExpressionException
   */
  private ProfileElement process(Component c, Datatype d, ProfileElement parent)
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
    String constraintPath = c.req().position() + "[1]";
    element.setConstraintPath(constraintPath);
    element.getConformanceStatements().addAll(
        findConfStatements(NODE_DATATYPE, d.id(), d.name(), constraintPath));
    element.getPredicates().addAll(findPredicates(NODE_DATATYPE, d.id(), d.name(), constraintPath));
    parent.getChildren().add(element);
    process(c.datatype(), element);

    return element;
  }

}
