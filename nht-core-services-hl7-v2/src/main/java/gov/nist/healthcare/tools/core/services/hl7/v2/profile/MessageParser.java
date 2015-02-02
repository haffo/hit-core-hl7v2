package gov.nist.healthcare.tools.core.services.hl7.v2.profile;

import gov.nist.healthcare.tools.core.models.ProfileElement;
import gov.nist.healthcare.tools.core.models.ProfileModel;
import gov.nist.healthcare.tools.core.models.hl7.v2.util.Util;
import hl7.v2.instance.SegOrGroup;
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
import hl7.v2.validation.content.ConstraintManager;

import java.util.HashSet;
import java.util.Set;

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

	private Message message;
	private ConstraintManager constraintManager;
	private ProfileModel model;
	private Set<String> tracker;

	public Message getMessage() {
		return message;
	}

	public void setMessage(Message message) {
		this.message = message;
	}

	public MessageParser() {
	}

	public ProfileModel parse(Message message,
			ConstraintManager constraintManager) {
		this.message = message;
		this.constraintManager = constraintManager;
		this.tracker = new HashSet<String>();
		model = new ProfileModel();
		ProfileElement full = new ProfileElement("FULL");
		model.getElements().add(full);
		scala.collection.immutable.List<SegRefOrGroup> children = this.message
				.structure();
		if (children != null && !children.isEmpty()) {
			Iterator<SegRefOrGroup> it = children.iterator();
			while (it.hasNext()) {
				process(it.next(), full);
			}
		}
		return model;
	}

	/**
	 * TODO: add predicate and conformance statement
	 * 
	 * @param ref
	 * @param parentElement
	 * @param model
	 * @param tracker
	 */
	private ProfileElement process(SegRefOrGroup ref,
			ProfileElement parentElement) {
		if (ref == null)
			return parentElement;
		if (ref instanceof SegmentRef) {
			return process(((SegmentRef) ref).ref(), ((SegmentRef) ref).req(),
					parentElement);
		} else if (ref instanceof Group) {
			return process((Group) ref, ((Group) ref).req(), parentElement);
		}
		return parentElement;
	}

	/**
	 * 
	 * @param s
	 * @param req
	 * @param parentElement
	 * @param model
	 * @param tracker
	 * @return
	 */
	private ProfileElement process(Segment s, Req req,
			ProfileElement parentElement) {
		ProfileElement element = process(req, new ProfileElement());		
		element.setTitle(s.name() + ":" + s.desc());
		element.setName(s.name());
		element.setType(SEGMENT);
		element.setLongName(s.desc());
		element.setIcon(ICON_SEGMENT);
		parentElement.getChildren().add(element);
		scala.collection.immutable.List<Field> children = s.fields();
		if (children != null && !children.isEmpty()) {
			Iterator<Field> it = children.iterator();
			while (it.hasNext()) {
				process(it.next(), element);
			}
		}
		if (!tracker.contains(element.getName())) {
			model.getElements().add(element);
			tracker.add(element.getName());
		}
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
	 */
	private ProfileElement process(Group g, Req req,
			ProfileElement parentElement) {
		ProfileElement element = process(req, new ProfileElement());
		element.setType(GROUP);
		element.setIcon(ICON_GROUP);
		element.setName(g.name());
		element.setLongName(g.name());
		element.setTitle(element.getName());
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
	 * @param ref
	 * @param parentElement
	 * @param model
	 * @param tracker
	 * @return
	 */
	private ProfileElement process(SegOrGroup ref, ProfileElement parentElement) {
		if (ref == null)
			return parentElement;
		if (ref instanceof Segment) {
			return process((Segment) ref, ref.req(), parentElement);
		} else if (ref instanceof Group) {
			return process((Group) ref, ref.req(), parentElement);
		}
		return parentElement;
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
		if(usage != null){
			element.setUsage(req.usage().toString());
		}
		if (card != null) {
			element.setMinOccurs(card.min());
			element.setMaxOccurs(card.max());
			element.setCardinality("[" + card.min() + "," + card.max() + "]");
		}
		Range length = Util.getOption(req.length());
		if (length != null) {
			int minLength = length.min();
			String maxLength = length.max();
			element.setMinLength(minLength + "");
			element.setMaxLength(maxLength);
			if (minLength != 0 && maxLength != null && !maxLength.equals("")) {
				if (!maxLength.equals("65K")) {
					element.setLength("[" + minLength + "," + maxLength + "]");
				} else {
					element.setLength("[" + minLength + ",*]");
				}
			} else {
				element.setLength("");
			}
		}
		return element;
	}

	/**
	 * 
	 * @param f
	 * @param parentElement
	 */
	private void process(Field f, ProfileElement parentElement) {
		if (f == null)
			return;
		ProfileElement element = process(f.req(), new ProfileElement());
		element.setTitle(f.name());
		element.setName(f.name());
		element.setType(FIELD);
		element.setDataTypeUsage("O".equals(element.getUsage()) ? "-" : element
				.getUsage());
		element.setIcon(ICON_FIELD);
		String table = Util.getOption(f.req().table());
		if (table != null)
			element.setTable(table);
		element.setDataType(f.datatype().name());
		element.setPosition(f.req().position() + "");
		element.setPath(parentElement.getName() + "." + f.req().position());
		element.setTitle(element.getPath() + " : " + element.getName());
		element.setDataTypeUsage("O".equals(element.getUsage()) ? "-" : element
				.getUsage());
		parentElement.getChildren().add(element);
		process(f.datatype(), element);
	}

	/**
	 * 
	 * @param d
	 * @param parentElement
	 */
	private void process(Datatype d, ProfileElement parentElement) {
		if (d instanceof Composite) {
			Composite c = (Composite) d;
			scala.collection.immutable.List<Component> children = c
					.components();
			if (children != null) {
				Iterator<Component> it = children.iterator();
				while (it.hasNext()) {
					process(it.next(), parentElement);
				}
			}
		}
	}

	/**
	 * 
	 * @param c
	 * @param parentElement
	 */
	private void process(Component c, ProfileElement parentElement) {
		if (c == null)
			return;
		ProfileElement element = new ProfileElement();
		process(c.req(), element);
		element.setName(c.name());
		element.setType(COMPONENT);
		element.setDataTypeUsage(element.getUsage());
		element.setIcon(ICON_COMPONENT);
 		String table = Util.getOption(c.req().table());
		if (table != null)
			element.setTable(table);
		
		element.setDataType(c.datatype().name());
		element.setPosition(c.req().position() + "");
		element.setPath(parentElement.getPath() + "." + c.req().position());
		element.setTitle(element.getPath() + " : " + element.getName());
		parentElement.getChildren().add(element);
		process(c.datatype(), element);
	}

}
