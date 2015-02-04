package gov.nist.healthcare.tools.core.services.hl7.v2.profile;

import gov.nist.healthcare.tools.core.models.ProfileElement;
import gov.nist.healthcare.tools.core.models.ProfileModel;
import gov.nist.healthcare.tools.core.models.ProfileRef;
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

import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.xpath.XPathExpressionException;

import scala.collection.Iterator;

public class MessageParser {

	private final static String GROUP = "GROUP";
	private final static String SEGMENT = "SEGMENT";
	private final static String FIELD = "FIELD";
	private final static String COMPONENT = "COMPONENT";

	private final static String ICON_GROUP = "group.png";
	private final static String ICON_SEGMENT = "segment.png";
	private final static String ICON_FIELD = "field.png";
	private final static String ICON_COMPONENT = "component.png";

 	private ConstraintManager predicateManager;
	private ConstraintManager confStatementManager;

	private ProfileModel model;
	private Map<String, ProfileElement> tracker;

 
	public MessageParser() {
	}

	public ProfileModel parse(Message message, String confStatementXml,
			String predicatesXml) throws XPathExpressionException {
 		this.confStatementManager = new ConstraintManager(confStatementXml);
		this.predicateManager = new ConstraintManager(predicatesXml);
		this.tracker = new LinkedHashMap<String, ProfileElement>();
		model = new ProfileModel();
		ProfileElement structure = new ProfileElement("Message Structure");
		structure.setType("MESSAGE");
		scala.collection.immutable.List<SegRefOrGroup> children =  message
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
	 * TODO: add predicate and conformance statement
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

	private ProfileElement process(SegmentRef ref, Req req,
			ProfileElement parentElement) throws XPathExpressionException {
		ProfileElement element = process(req, new ProfileElement());
		Segment s = ref.ref();
//		element.setTitle(s.name() + ":" + s.desc());
		element.setName(s.name());
		element.setType(SEGMENT);
		element.setLongName(s.desc());
		element.setIcon(ICON_SEGMENT);

		// element.getConformanceStatements().addAll(confStatementManager.findById("Segment",s.id()));
		// element.getConformanceStatements().addAll(confStatementManager.findByName("Segment",
		// s.name()));
		// element.getPredicates().addAll(predicateManager.findById("Segment",
		// s.id()));
		// element.getPredicates().addAll(predicateManager.findByName("Segment",
		// s.name()));
		//
		ProfileElement pe = null;
		if (tracker.containsKey(s.id())) {
			pe = tracker.get(s.id());
		} else {
			pe = process(ref.ref(), ref.req());
			tracker.put(pe.getId(), pe);
		}
		element.setReference(new ProfileRef(pe.getId(), "Segment", pe.getName()));
		parentElement.getChildren().add(element);
		
		return element;
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
//		element.setTitle(s.name() + ":" + s.desc());
		element.setName(s.name());
		element.setType(SEGMENT);
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

		element.getConformanceStatements().addAll(
				confStatementManager.findById("Segment", s.id()));
		element.getConformanceStatements().addAll(
				confStatementManager.findByName("Segment", s.name()));
		element.getPredicates().addAll(
				predicateManager.findById("Segment", s.id()));
		element.getPredicates().addAll(
				predicateManager.findByName("Segment", s.name()));

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
		ProfileElement element = process(req, new ProfileElement());
		element.setType(GROUP);
		element.setIcon(ICON_GROUP);
		element.setName(g.name());
		element.setLongName(g.name());
//		element.setTitle(element.getName());

		element.getConformanceStatements().addAll(
				confStatementManager.findByName("Group", g.name()));
		element.getPredicates().addAll(
				predicateManager.findByName("Group", g.name()));

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
	private ProfileElement process(Req req, ProfileElement element) {
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
			element.setMaxLength( length.max());
		}
		return element;
	}

	/**
	 * 
	 * @param f
	 * @param parentElement
	 * @throws XPathExpressionException
	 */
	private void process(Field f, ProfileElement parentElement)
			throws XPathExpressionException {
		if (f == null)
			return;
		ProfileElement element = process(f.req(), new ProfileElement());
 		element.setName(f.name());
		element.setType(FIELD);
//		element.setDataTypeUsage("O".equals(element.getUsage()) ? "-" : element
//				.getUsage());
		element.setIcon(ICON_FIELD);
		String table = Util.getOption(f.req().table());
		if (table != null)
			element.setTable(table);
		element.setDataType(f.datatype().name());
		element.setPosition(f.req().position() + "");
 		element.setPath(parentElement.getName() + "." + f.req().position());
//		element.setTitle(element.getPath() + " : " + element.getName());
//		element.setDataTypeUsage("O".equals(element.getUsage()) ? "-" : element
//				.getUsage());

		parentElement.getChildren().add(element);
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

		belongTo.getConformanceStatements().addAll(
				confStatementManager.findById("Datatype", d.id()));
		belongTo.getConformanceStatements().addAll(
				confStatementManager.findByName("Datatype", d.name()));

		belongTo.getPredicates().addAll(
				predicateManager.findById("Datatype", d.id()));
		belongTo.getPredicates().addAll(
				predicateManager.findByName("Datatype", d.name()));

		if (d instanceof Composite) {
			Composite c = (Composite) d;
			scala.collection.immutable.List<Component> children = c
					.components();
			if (children != null) {
				Iterator<Component> it = children.iterator();
				while (it.hasNext()) {
					process(it.next(), belongTo);
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
	private ProfileElement process(Component c, ProfileElement parentElement)
			throws XPathExpressionException {
		if (c == null)
			return parentElement;
		ProfileElement element = new ProfileElement();
		process(c.req(), element);
		element.setName(c.name());
		element.setType(COMPONENT);
//		element.setDataTypeUsage(element.getUsage());
		element.setIcon(ICON_COMPONENT);
		String table = Util.getOption(c.req().table());
		if (table != null)
			element.setTable(table);

		element.setDataType(c.datatype().name());
		element.setPosition(c.req().position() + "");
		element.setPath(parentElement.getPath() + "." + c.req().position());
//		element.setTitle(element.getPath() + " : " + element.getName());
		parentElement.getChildren().add(element);
		process(c.datatype(), element);
		
		return element;
	}

}
