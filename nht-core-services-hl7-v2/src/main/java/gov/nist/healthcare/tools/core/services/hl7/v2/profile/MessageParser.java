package gov.nist.healthcare.tools.core.services.hl7.v2.profile;

import gov.nist.healthcare.tools.core.models.Constraint;
import gov.nist.healthcare.tools.core.models.Predicate;
import gov.nist.healthcare.tools.core.models.ProfileElement;
import gov.nist.healthcare.tools.core.models.ProfileModel;
import gov.nist.healthcare.tools.core.models.hl7.v2.util.Util;
import hl7.v2.profile.Component;
import hl7.v2.profile.Composite;
import hl7.v2.profile.Datatype;
import hl7.v2.profile.Field;
import hl7.v2.profile.Group;
import hl7.v2.profile.Message;
import hl7.v2.profile.Range;
import hl7.v2.profile.Req;
import hl7.v2.profile.SegRefOrGroup;
import hl7.v2.profile.Segment;
import hl7.v2.profile.SegmentRef;
import hl7.v2.profile.Usage;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.xpath.XPathExpressionException;

import scala.collection.Iterator;

public class MessageParser {

	private final static String TYPE_GROUP = "GROUP";
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
	private final static String ICON_COMPONENT = "component.png";
	private final static String ICON_SUBCOMPONENT = "subcomponent.png";

 	private ConstraintManager constraintManager;

	private ProfileModel model;
	private Map<String, ProfileElement> tracker;

	public MessageParser() {
	}

	public ProfileModel parse(Message message, String constraintsXml) throws XPathExpressionException {
		this.constraintManager = new ConstraintManager(constraintsXml);
 		this.tracker = new LinkedHashMap<String, ProfileElement>();
		model = new ProfileModel();
		ProfileElement structure = new ProfileElement("Message Structure");
		structure.setType("MESSAGE");
		structure.setRelevent(true);
		structure.setConstraintPath(null);
		scala.collection.immutable.List<SegRefOrGroup> children = message
				.structure();
		if (children != null && !children.isEmpty()) {
			Iterator<SegRefOrGroup> it = children.iterator();
			while (it.hasNext()) {
				process(it.next(), structure);
			}
		}
		model.getElements().add(structure);
		model.getElements().addAll(this.tracker.values());
		return model;
	}

	/**
	 * 
	 * @param ref
	 * @param parentElement
	 * @param model
	 * @param tracker
	 * @throws XPathExpressionException
	 */
	private ProfileElement process(SegRefOrGroup ref,
			ProfileElement parentElement) throws XPathExpressionException {
		if (ref == null)
			return parentElement;
		if (ref instanceof SegmentRef) {
			return process((SegmentRef) ref, ((SegmentRef) ref).req(),
					parentElement);
		} else if (ref instanceof Group) {
			return process((Group) ref, ((Group) ref).req(), parentElement);
		} else {
			throw new IllegalArgumentException("Unknown type of SegRefOrGroup");
		}
	}

	private Set<Constraint> findConfStatements(String type, String id,
			String name, String constraintPath) throws XPathExpressionException {
		Set<Constraint> constraints = new HashSet<Constraint>();
		if (id != null)
			constraints.addAll(constraintManager.findConfStatementsByIdAndPath(type, id,
					constraintPath));
		if (name != null)
			constraints.addAll(constraintManager.findConfStatementsByNameAndPath(type,
					name, constraintPath));
		return constraints;
	}
	
 
	

	private Set<Predicate> findPredicates(String type, String id, String name,
			String constraintPath) throws XPathExpressionException {
		Set<Predicate> constraints = new HashSet<Predicate>();
		if (id != null)
			constraints.addAll(constraintManager.findPredicatesByIdAndTarget(type, id,
					constraintPath));
		if (name != null)
			constraints.addAll(constraintManager.findPredicatesByNameAndTarget(type, name,
					constraintPath));
		return constraints;
	}

	private ProfileElement process(SegmentRef ref, Req req,
			ProfileElement parentElement) throws XPathExpressionException {
		ProfileElement element = process(req, new ProfileElement(),parentElement);
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
				findConfStatements(NODE_GROUP, parentElement.getId(),
						parentElement.getName(), constraintPath));
		element.getPredicates().addAll(
				findPredicates(NODE_GROUP, parentElement.getId(),
						parentElement.getName(), constraintPath));
		// relative to its parent
		if (TYPE_GROUP.equals(parentElement.getType())) {
			constraintPath = parentElement.getConstraintPath() + "."
					+ constraintPath;
			element.getConformanceStatements().addAll(
					findConfStatements(NODE_GROUP, parentElement.getId(),
							parentElement.getName(), constraintPath));
			element.getPredicates().addAll(
					findPredicates(NODE_GROUP, parentElement.getId(),
							parentElement.getName(), constraintPath));
		}

		ProfileElement pe = null;
		if (tracker.containsKey(s.id())) {
			pe = tracker.get(s.id());
		} else {
			pe = process(ref.ref(), ref.req());
			tracker.put(pe.getId(), pe);
		}
		pe.setRelevent(pe.isRelevent() || element.isRelevent());
 		element.setReference(new gov.nist.healthcare.tools.core.models.SegmentRef(
				pe.getId(), "Segment", pe.getName()));
		parentElement.getChildren().add(element);

		return element;
	} 
	
	
	private boolean relevent(ProfileElement child, ProfileElement parent){
		if(parent != null) {
		return  relevent(child.getUsage()) && relevent(parent, parent.getParent()); 
		}else{
			return relevent(child.getUsage());
		}
    } 
	
	private boolean relevent(String usage){
 		return usage == null || usage.equals("R") || usage.equals("RE") || usage.equals("C") || usage.startsWith("C");
	}
	

	/**
	 * 
	 * @param s
	 * @param req
	 * @param parentElement
	 * @param model
	 * @param tracker
	 * @return
	 * @throws XPathExpressionException
	 */
	private ProfileElement process(Segment s, Req req)
			throws XPathExpressionException {
		ProfileElement element = new ProfileElement();
		// element.setTitle(s.name() + ":" + s.desc());
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

		// element.getConformanceStatements().addAll(
		// confStatementManager.findById("Segment", s.id()));
		// element.getConformanceStatements().addAll(
		// confStatementManager.findByName("Segment", s.name()));
		// element.getPredicates().addAll(
		// predicateManager.findById("Segment", s.id()));
		// element.getPredicates().addAll(
		// predicateManager.findByName("Segment", s.name()));

		return element;
	}

	/**
	 * 
	 * @param g
	 * @param req
	 * @param parentElement
	 * @param model
	 * @param tracker
	 * @return
	 * @throws XPathExpressionException
	 */
	private ProfileElement process(Group g, Req req,
			ProfileElement parentElement) throws XPathExpressionException {
		ProfileElement element = process(req, new ProfileElement(),parentElement);
		element.setType(TYPE_GROUP);
		element.setIcon(ICON_GROUP);
		element.setName(g.name());
		element.setLongName(g.name());
		element.setParent(parentElement);
		element.setConstraintPath(req.position() + "[1]");

		// relative to parent
		String constraintPath = element.getConstraintPath();
		element.getConformanceStatements().addAll(
				findConfStatements(NODE_GROUP, parentElement.getId(),
						parentElement.getName(), constraintPath));
		element.getPredicates().addAll(
				findPredicates(NODE_GROUP, parentElement.getId(),
						parentElement.getName(), constraintPath));

		// TODO: a Group child of another Group
		// if (parentElement.getConstraintPath() != null) {
		// constraintPath = parentElement.getConstraintPath() + "."
		// + element.getConstraintPath();
		// element.getConformanceStatements().addAll(
		// findConfStatements(NODE_GROUP, parentElement.getId(),
		// parentElement.getName(), constraintPath));
		// element.getPredicates().addAll(
		// findPredicates(NODE_GROUP, parentElement.getId(),
		// parentElement.getName(), constraintPath));
		// }
		

		
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
	private ProfileElement process(Req req, ProfileElement element, ProfileElement parent ) {
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
		
		boolean relevent = relevent(  element, parent);
		element.setRelevent(relevent);
		
		return element;
	}

	/**
	 * 
	 * @param f
	 * @param parentElement
	 * @throws XPathExpressionException
	 */
	private void process(Field f, ProfileElement parent)
			throws XPathExpressionException {
		if (f == null)
			return;
		ProfileElement element = process(f.req(), new ProfileElement(),parent);
		element.setName(f.name());
		element.setType(TYPE_FIELD);
		// element.setDataTypeUsage("O".equals(element.getUsage()) ? "-" :
		// element
		// .getUsage());
		element.setIcon(ICON_FIELD);
		element.setParent(parent);
		String table = Util.getOption(f.req().table());
		if (table != null)
			element.setTable(table);
		element.setDataType(f.datatype().id()); // use id for flavors
		element.setPosition(f.req().position() + "");
		element.setPath(parent.getName() + "." + f.req().position());
		String constraintPath = f.req().position() + "[1]";
		element.setConstraintPath(constraintPath);

		element.getConformanceStatements().addAll(
				findConfStatements(NODE_SEGMENT, parent.getId(),
						parent.getName(), constraintPath));
		element.getPredicates().addAll(
				findPredicates(NODE_SEGMENT, parent.getId(), parent.getName(),
						constraintPath));

		parent.getChildren().add(element);
		process(f.datatype(), element);
	}

	/**
	 * 
	 * @param d
	 * @param parentElement
	 * @throws XPathExpressionException
	 */
	private ProfileElement process(Datatype d, ProfileElement belongTo)
			throws XPathExpressionException {

		// belongTo.getConformanceStatements().addAll(
		// confStatementManager.findById("Datatype", d.id()));
		// belongTo.getConformanceStatements().addAll(
		// confStatementManager.findByName("Datatype", d.name()));
		//
		// belongTo.getPredicates().addAll(
		// predicateManager.findById("Datatype", d.id()));
		// belongTo.getPredicates().addAll(
		// predicateManager.findByName("Datatype", d.name()));

		if (d instanceof Composite) {
			Composite c = (Composite) d;
			scala.collection.immutable.List<Component> children = c
					.components();
			if (children != null) {
				Iterator<Component> it = children.iterator();
				while (it.hasNext()) {
					ProfileElement element = process(it.next(), d, belongTo);
					// get conformance statements and predicates from a segment
					// context
					// context. ex location: 7[1].2[1] or 7[1].2[1].2[1]
					// handles 7[1].2[1]
					String segmentId = belongTo.getParent().getId();
					String segmentName = belongTo.getParent().getName();
					String constraintPath = belongTo.getConstraintPath() + "."
							+ element.getConstraintPath();
					// handle 7[1].2[1]
					element.getConformanceStatements().addAll(
							findConfStatements(NODE_SEGMENT, segmentId,
									null, constraintPath));
					element.getPredicates().addAll(
							findPredicates(NODE_SEGMENT, segmentId,
									null, constraintPath));

					if (belongTo.getType().equals(TYPE_COMPONENT)) {
						segmentId = belongTo.getParent().getParent().getId();
						segmentName = belongTo.getParent().getParent()
								.getName();
						// handle 7[1].2[1].2[1]
						constraintPath = belongTo.getParent()
								.getConstraintPath()
								+ "."
								+ belongTo.getConstraintPath()
								+ "."
								+ element.getPosition() + "[1]";
						element.getConformanceStatements().addAll(
								findConfStatements(NODE_SEGMENT, segmentId,
										null, constraintPath));
						element.getPredicates().addAll(
								findPredicates(NODE_SEGMENT, segmentId,
										null, constraintPath));
					}

					// element.getConformanceStatements().addAll(
					// confStatementManager.findByIdAndPath(NODE_SEGMENT,
					// segmentId, constraintPath));
					// element.getConformanceStatements().addAll(
					// confStatementManager.findByNameAndPath(
					// NODE_SEGMENT, segmentName, constraintPath));
					// element.getPredicates().addAll(
					// predicateManager.findByIdAndPath(NODE_SEGMENT,
					// segmentId, constraintPath));
					// element.getPredicates().addAll(
					// predicateManager.findByNameAndPath(NODE_SEGMENT,
					// segmentName, constraintPath));
				}
			}
		}

		return belongTo;
	}

	/**
	 * 
	 * @param c
	 * @param parentElement
	 * @throws XPathExpressionException
	 */
	private ProfileElement process(Component c, Datatype d,
			ProfileElement parent) throws XPathExpressionException {
		if (c == null)
			return parent;
		ProfileElement element = new ProfileElement();
		process(c.req(), element,parent);
		element.setName(c.name());
		element.setType(c.datatype() instanceof Composite ? TYPE_COMPONENT:TYPE_SUBCOMPONENT);
		element.setIcon(c.datatype() instanceof Composite ? ICON_COMPONENT:ICON_SUBCOMPONENT);
		String table = Util.getOption(c.req().table());
		if (table != null)
			element.setTable(table);
		element.setDataType(c.datatype().name());
		element.setPosition(c.req().position() + "");
		element.setParent(parent);
		element.setPath(parent.getPath() + "." + c.req().position());
		// get conformance statements and predicates from a datatype context
		String constraintPath = c.req().position() + "[1]";
		element.setConstraintPath(constraintPath);
		element.getConformanceStatements().addAll(
				findConfStatements(NODE_DATATYPE, d.id(), d.name(),
						constraintPath));
		element.getPredicates().addAll(
				findPredicates(NODE_DATATYPE, d.id(), d.name(),
						constraintPath));
		parent.getChildren().add(element);
		process(c.datatype(), element);

		return element;
	}

}
