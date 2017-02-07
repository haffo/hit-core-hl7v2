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

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import gov.nist.hit.core.domain.ProfileElement;
import gov.nist.hit.core.domain.ProfileModel;
import gov.nist.hit.core.domain.constraints.ByID;
import gov.nist.hit.core.domain.constraints.ByName;
import gov.nist.hit.core.domain.constraints.ByNameOrByID;
import gov.nist.hit.core.domain.constraints.ConformanceStatement;
import gov.nist.hit.core.domain.constraints.Constraints;
import gov.nist.hit.core.domain.constraints.Context;
import gov.nist.hit.core.domain.constraints.Predicate;
import gov.nist.hit.core.hl7v2.domain.util.Util;
import gov.nist.hit.core.service.ProfileParser;
import gov.nist.hit.core.service.exception.ProfileParserException;
import gov.nist.hit.core.service.impl.ConstraintsParserImpl;
import hl7.v2.profile.Component;
import hl7.v2.profile.Composite;
import hl7.v2.profile.Datatype;
import hl7.v2.profile.DynMapping;
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
import scala.collection.Iterator;
import scala.collection.immutable.List;

/**
 * 
 * @author Harold Affo
 * 
 */
public abstract class HL7V2ProfileParser extends ProfileParser {

	public HL7V2ProfileParser() {
	}

	private final Map<String, Profile> cachedIntegrationProfilesMap = new HashMap<String, Profile>();
	private Map<String, ProfileElement> segmentsMap;
	private Map<String, ProfileElement> datatypesMap;
	private Constraints conformanceStatements = null;
	private Constraints predicates = null;
	ConstraintsParserImpl constraintsParser = new ConstraintsParserImpl();

	@Override
	/**
	 * integrationProfileXml: integration profile xml content
	 * conformanceProfileId: conformance profile id Options: constraints xml
	 * content
	 */
	public ProfileModel parse(String integrationProfileXml, String conformanceProfileId, String... constraints)
			throws ProfileParserException {
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
	public ProfileModel parse(Object conformanceProfile, String... constraints) throws ProfileParserException {
		try {
			if (!(conformanceProfile instanceof Message)) {
				throw new IllegalArgumentException(
						"Conformance Profile is not a valid instanceof " + Message.class.getCanonicalName());
			}
			String c1Xml = constraints != null && constraints.length > 0 ? constraints[0] : null;
			String c2Xml = constraints != null && constraints.length > 1 ? constraints[1] : null;
			this.segmentsMap = new LinkedHashMap<String, ProfileElement>();
			this.datatypesMap = new LinkedHashMap<String, ProfileElement>();
			this.conformanceStatements = constraintsParser.confStatements(c1Xml);
			this.predicates = constraintsParser.predicates(c1Xml);
			if (c2Xml != null) {
				Constraints conformanceStatements2 = constraintsParser.confStatements(c2Xml);
				if (conformanceStatements2 != null) {
					this.conformanceStatements = merge(this.conformanceStatements, conformanceStatements2);
				}
				Constraints predicates2 = constraintsParser.predicates(c2Xml);
				if (predicates2 != null) {
					this.predicates = merge(this.predicates, predicates2);
				}
			}
			process((Message) conformanceProfile);
			return model;
		} catch (XPathExpressionException e) {
			throw new ProfileParserException(e.getLocalizedMessage());
		} catch (CloneNotSupportedException e) {
			throw new ProfileParserException(e.getLocalizedMessage());
		}
	}

	private Constraints merge(Constraints c1, Constraints c2) {
		if (c2 == null)
			return c1;

		if (c1 == null)
			return c2;

		if (c1.getDatatypes() == null) {
			c1.setDatatypes(new Context());
		}

		if (c2.getDatatypes() != null) {
			c1.getDatatypes().getByNameOrByIDs().addAll(c2.getDatatypes().getByNameOrByIDs());
		}

		if (c1.getSegments() == null) {
			c1.setSegments(new Context());
		}

		if (c2.getSegments() != null) {
			c1.getSegments().getByNameOrByIDs().addAll(c2.getSegments().getByNameOrByIDs());
		}

		if (c1.getGroups() == null) {
			c1.setGroups(new Context());
		}

		if (c2.getGroups() != null) {
			c1.getGroups().getByNameOrByIDs().addAll(c2.getGroups().getByNameOrByIDs());
		}

		if (c1.getMessages() == null) {
			c1.setMessages(new Context());
		}

		if (c2.getMessages() != null) {
			c1.getMessages().getByNameOrByIDs().addAll(c2.getMessages().getByNameOrByIDs());
		}

		return c1;
	}

	private ProfileElement process(Message m) throws XPathExpressionException, CloneNotSupportedException {
		model = new ProfileModel();
		ProfileElement message = new ProfileElement("FULL");
		message.setType(TYPE_MESSAGE);
		message.setRelevent(true);
		message.setId(m.id());
		model.setMessage(message);
		message.setConformanceStatements(this.findConformanceStatements(this.conformanceStatements.getMessages(),
				model.getMessage().getId(), model.getMessage().getName()));
		message.setPredicates(this.findPredicates(this.predicates.getMessages(), model.getMessage().getId(),
				model.getMessage().getName()));

		scala.collection.immutable.List<SegRefOrGroup> children = m.structure();
		if (children != null && !children.isEmpty()) {
			Iterator<SegRefOrGroup> it = children.iterator();
			while (it.hasNext()) {
				process(it.next(), message);
			}
		}
		model.setDatatypes(this.datatypesMap);
		model.setSegments(this.segmentsMap);
		return message;
	}

	/**
	 * 
	 * @param ref
	 * @param parentElement
	 * @param model
	 * @param segmentsMap
	 * @throws XPathExpressionException
	 * @throws CloneNotSupportedException
	 */
	private ProfileElement process(SegRefOrGroup ref, ProfileElement parentElement)
			throws XPathExpressionException, CloneNotSupportedException {
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
			throws XPathExpressionException, CloneNotSupportedException {
		ProfileElement element = process(req, new ProfileElement(), parentElement);
		Segment s = ref.ref();
		element.setName(s.name());
		element.setType(TYPE_SEGMENT_REF);
		element.setDescription(s.desc());
		// element.setIcon(ICON_SEGMENT);
		element.setParent(parentElement);
		element.setPosition(req.position() + "");
		element.setId(UUID.randomUUID().toString());
		ProfileElement segmentElement = null;
		if (segmentsMap.containsKey(s.id())) {
			segmentElement = segmentsMap.get(s.id());
		} else {
			segmentElement = process(ref.ref(), ref.req());
			segmentsMap.put(segmentElement.getId(), segmentElement);
		}
		segmentElement.setHide(req.hide());
		segmentElement.setRelevent(segmentElement.isRelevent() || element.isRelevent());
		element.setRef(segmentElement.getId());
		// element.setChildren(segmentElement.getChildren());
		element.setPath(segmentElement.getName());

		element.setPredicates(new ArrayList<Predicate>());
		element.setConformanceStatements(new ArrayList<ConformanceStatement>());

		// addMessageConstraints(element);
		parentElement.getChildren().add(element);
		return element;
	}

	// private void addMessageConstraints(ProfileElement element) {
	// String targetPath = getTargetPath(element);
	// if (!targetPath.equals("")) {
	// for (ConformanceStatement cs :
	// this.model.getMessage().getConformanceStatements()) {
	// if (cs.getConstraintTarget().equals(targetPath)) {
	// element.getConformanceStatements().add(cs);
	// }
	// }
	//
	// for (Predicate p : this.model.getMessage().getPredicates()) {
	// if (p.getConstraintTarget().equals(targetPath)) {
	// element.getPredicates().add(p);
	// }
	// }
	// }
	// }

	private boolean relevent(ProfileElement child, ProfileElement parent) {
		boolean isUsageRelevent = relevent(child.getUsage(), child.isHide());
		if (parent != null) {
			return isUsageRelevent && relevent(parent, parent.getParent());
		} else {
			return isUsageRelevent;
		}
	}

	private boolean relevent(String usage, boolean hide) {
		return (usage == null || usage.equals("R") || usage.equals("RE") || usage.equals("C")
				|| (usage.startsWith("C") && (usage.contains("R") || usage.contains("RE")))) && !hide;
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
	 * @throws CloneNotSupportedException
	 */
	private ProfileElement process(Segment s, Req req) throws XPathExpressionException, CloneNotSupportedException {
		ProfileElement element = new ProfileElement();
		element.setName(s.name());
		element.setType(TYPE_SEGMENT);
		element.setDescription(s.desc());
		// element.setIcon(ICON_SEGMENT);
		element.setId(s.id());
		element.setDynamicMaps(dynaMap(s));
		element.setPredicates(this.findPredicates(this.predicates.getSegments(), s.id(), s.name()));
		element.setConformanceStatements(
				this.findConformanceStatements(this.conformanceStatements.getSegments(), s.id(), s.name()));

		scala.collection.immutable.List<Field> children = s.fields();
		if (children != null && !children.isEmpty()) {
			Iterator<Field> it = children.iterator();
			while (it.hasNext()) {
				process(it.next(), element);
			}
		}
		return element;
	}

	private Map<Integer, Set<String>> dynaMap(Segment s) throws XPathExpressionException, CloneNotSupportedException {
		List<DynMapping> dynamicMappings = s.mappings();
		if (!dynamicMappings.isEmpty()) {
			Map<Integer, Set<String>> maps = new HashMap<Integer, Set<String>>();
			Iterator<DynMapping> dynIt = dynamicMappings.iterator();
			while (dynIt.hasNext()) {
				DynMapping d = dynIt.next();
				Set<String> ids = new HashSet<String>();
				Iterator<Datatype> mapIt = d.map().valuesIterator();
				while (mapIt.hasNext()) {
					Datatype da = mapIt.next();
					ids.add(da.id());
					process(da);
				}
				maps.put(d.position(), ids);
			}
			return maps;
		}
		return null;
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
	 * @throws CloneNotSupportedException
	 */
	private ProfileElement process(Group g, Req req, ProfileElement parentElement)
			throws XPathExpressionException, CloneNotSupportedException {
		ProfileElement element = process(req, new ProfileElement(), parentElement);
		element.setType(TYPE_GROUP);
		// element.setIcon(ICON_GROUP);
		element.setName(g.name());
		element.setDescription(g.name());
		element.setParent(parentElement);
		element.setPosition(req.position() + "");
		element.setId(g.id());
		element.setPredicates(this.findPredicates(this.predicates.getGroups(), g.id(), g.name()));
		element.setConformanceStatements(
				this.findConformanceStatements(this.conformanceStatements.getGroups(), g.id(), g.name()));

		// String targetPath = getTargetPath(element);
		// if (!targetPath.equals("")) {
		// for (ConformanceStatement cs :
		// this.model.getMessage().getConformanceStatements()) {
		// if (cs.getConstraintTarget().equals(targetPath)) {
		// element.getConformanceStatements().add(cs);
		// }
		// }
		//
		// for (Predicate p : this.model.getMessage().getPredicates()) {
		// if (p.getConstraintTarget().equals(targetPath)) {
		// element.getPredicates().add(p);
		// }
		// }
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

	private String getTargetPath(ProfileElement element) {
		if (element == null || element.getType().equals(TYPE_MESSAGE))
			return "";
		String pTarget = getTargetPath(element.getParent());
		return pTarget.equals("") ? element.getPosition() + "[1]" : pTarget + "." + element.getPosition() + "[1]";
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
			element.setMin(card.min() + "");
			element.setMax(card.max());
		}
		Range length = Util.getOption(req.length());
		if (length != null) {
			element.setMinLength(length.min() + "");
			element.setMaxLength(length.max());
		}
		element.setHide(req.hide());
		boolean relevent = relevent(element, parent);
		element.setRelevent(relevent);

		return element;
	}

	/**
	 * 
	 * @param f
	 * @param parentElement
	 * @throws XPathExpressionException
	 * @throws CloneNotSupportedException
	 */
	private void process(Field f, ProfileElement parent) throws XPathExpressionException, CloneNotSupportedException {
		if (f == null)
			return;
		ProfileElement element = process(f.req(), new ProfileElement(), parent);
		element.setName(f.name());
		element.setType(TYPE_FIELD);
		element.setId(UUID.randomUUID().toString());
		element.setParent(parent);
		String table = table(f.req());
		if (table != null) {
			element.setTable(table);
		}
		element.setPosition(f.req().position() + "");
		element.setPath(parent.getName() + "-" + f.req().position());
		parent.getChildren().add(element);
		ProfileElement datatypeElement = process(f.datatype());
		element.setDatatype(datatypeElement.getId()); // use id for flavors
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

	private ProfileElement process(Datatype d) throws XPathExpressionException, CloneNotSupportedException {
		if (!datatypesMap.containsKey(d.id())) {
			ProfileElement element = new ProfileElement();
			element.setId(d.id());
			element.setName(d.name());
			element.setDescription(d.desc());
			element.setType(TYPE_DATATYPE);
			element.setRelevent(true);
			element.setPredicates(this.findPredicates(this.predicates.getDatatypes(), d.id(), d.name()));
			element.setConformanceStatements(
					this.findConformanceStatements(this.conformanceStatements.getDatatypes(), d.id(), d.name()));
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
			throws XPathExpressionException, CloneNotSupportedException {
		if (c == null)
			return parent;
		ProfileElement element = new ProfileElement();
		process(c.req(), element, parent);
		element.setName(c.name());
		element.setId(UUID.randomUUID().toString());
		element.setType(TYPE_COMPONENT);
		String table = table(c.req());
		if (table != null)
			element.setTable(table);
		element.setDatatype(c.datatype().id());
		element.setPosition(c.req().position() + "");
		element.setParent(parent);
		element.setPath(parent.getPath() + "." + c.req().position());
		parent.getChildren().add(element);
		ProfileElement datatypeElement = process(c.datatype());
		element.setChildren(ProfileElement.clone(datatypeElement.getChildren()));
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
		Element elmIntegrationProfile = (Element) doc.getElementsByTagName("ConformanceProfile").item(0);
		return elmIntegrationProfile.getAttribute("ID");
	}

	private ProfileElement findElement(java.util.List<Integer> positions, ProfileElement element) {
		if (!positions.isEmpty()) {
			int curPos = positions.get(0);
			ProfileElement child1 = null;
			if (element.getType().equals(TYPE_MESSAGE) || element.getType().equals(TYPE_GROUP)
					|| element.getType().equals(TYPE_SEGMENT) || element.getType().equals(TYPE_FIELD)
					|| element.getType().equals(TYPE_COMPONENT) || element.getType().equals(TYPE_DATATYPE)) {
				child1 = element.getChildren().get(curPos - 1);
			} else if (element.getType().equals(TYPE_SEGMENT_REF)) {
				ProfileElement segmentElement = segmentsMap.get(element.getRef());
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

	private ProfileElement findElementByName(String name, Map<String, ProfileElement>... maps) {
		for (Map<String, ProfileElement> map : maps) {
			for (ProfileElement child : map.values()) {
				if (name.equals(child.getName())) {
					return child;
				}
			}
		}
		return null;
	}

	private ProfileElement findElementById(String id, Map<String, ProfileElement>... maps) {
		for (Map<String, ProfileElement> map : maps) {
			if (map.get(id) != null) {
				return map.get(id);
			}
		}
		return null;
	}

	private ArrayList<ConformanceStatement> findConformanceStatements(Context context, String id, String name) {
		Set<ByNameOrByID> byNameOrByIDs = context.getByNameOrByIDs();
		ArrayList<ConformanceStatement> result = new ArrayList<ConformanceStatement>();
		for (ByNameOrByID byNameOrByID : byNameOrByIDs) {
			if (byNameOrByID instanceof ByID) {
				ByID byID = (ByID) byNameOrByID;
				if (byID.getByID().equals(id)) {
					for (ConformanceStatement c : byID.getConformanceStatements()) {
						result.add(c);
					}
				}
			} else if (byNameOrByID instanceof ByName) {
				ByName byName = (ByName) byNameOrByID;
				if (byName.getByName().equals(name)) {
					for (ConformanceStatement c : byName.getConformanceStatements()) {
						result.add(c);
					}
				}
			}
		}
		return result;
	}

	private ArrayList<Predicate> findPredicates(Context context, String id, String name) {
		Set<ByNameOrByID> byNameOrByIDs = context.getByNameOrByIDs();
		ArrayList<Predicate> result = new ArrayList<Predicate>();
		for (ByNameOrByID byNameOrByID : byNameOrByIDs) {
			if (byNameOrByID instanceof ByID) {
				ByID byID = (ByID) byNameOrByID;
				if (byID.getByID().equals(id)) {
					for (Predicate p : byID.getPredicates()) {
						result.add(p);
					}
				}
			} else if (byNameOrByID instanceof ByName) {
				ByName byName = (ByName) byNameOrByID;
				if (byName.getByName().equals(name)) {
					for (Predicate p : byName.getPredicates()) {
						result.add(p);
					}
				}
			}
		}
		return result;
	}

}
