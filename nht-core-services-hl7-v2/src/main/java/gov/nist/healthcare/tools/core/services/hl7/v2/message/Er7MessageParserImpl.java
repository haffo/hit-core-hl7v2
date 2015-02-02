/**
 * This software was developed at the National Institute of Standards and
 * Technology by employees of the Federal Government in the course of their
 * official duties. Pursuant to title 17 Section 105 of the United States Code
 * this software is not subject to copyright protection and is in the public
 * domain. This is an experimental system. NIST assumes no responsibility
 * whatsoever for its use by other parties, and makes no guarantees, expressed
 * or implied, about its quality, reliability, or any other characteristic. We
 * would appreciate acknowledgement if the software is used. This software can
 * be redistributed and/or modified freely provided that any derivative works
 * bear some notice that they are derived from it, and any modified versions
 * bear some notice that they have been modified.
 */
package gov.nist.healthcare.tools.core.services.hl7.v2.message;

import gov.nist.healthcare.tools.core.models.MessageElement;
import gov.nist.healthcare.tools.core.models.MessageModel;
import gov.nist.healthcare.tools.core.models.hl7.v2.message.MessageElementData;
import gov.nist.healthcare.tools.core.models.hl7.v2.util.Util;
import gov.nist.healthcare.tools.core.services.exception.MessageParserException;
import hl7.v2.instance.ComplexComponent;
import hl7.v2.instance.ComplexField;
import hl7.v2.instance.Component;
import hl7.v2.instance.Field;
import hl7.v2.instance.Group;
import hl7.v2.instance.Location;
import hl7.v2.instance.Message;
import hl7.v2.instance.SegOrGroup;
import hl7.v2.instance.Segment;
import hl7.v2.instance.SimpleComponent;
import hl7.v2.instance.SimpleField;
import hl7.v2.profile.Profile;
import hl7.v2.profile.Range;
import hl7.v2.profile.Req;
import hl7.v2.profile.XMLDeserializer;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import scala.collection.Iterator;
import scala.collection.immutable.List;

public class Er7MessageParserImpl implements Er7MessageParser {

	private final static String SEGMENT = "SEGMENT";
	private final static String FIELD = "FIELD";
	private final static String COMPONENT = "COMPONENT";
	private final static String GROUP = "GROUP";
	private final static String SUB_COMPONENT = "SUB_COMPONENT";
	
	/**
	 * 
	 */
	@Override
	public MessageModel parse(String er7Message, Object... options)
			throws MessageParserException {
		try {
			String profileXml = (String) options[0];
			if (!"".equals(er7Message) && er7Message != null) {
				InputStream profileStream = IOUtils.toInputStream(profileXml);
				Profile profile = XMLDeserializer.deserialize(profileStream).get();
				scala.collection.Iterable<String> keys = profile.messages().keys();
				String key = keys.iterator().next();
 				JParser p = new JParser();
				Message message = p.jparse(er7Message, profile.messages()
						.apply(key));
				return toModel(message);
			}
		} catch (Exception e) {
			throw new MessageParserException(e.getMessage());
		}
		return new MessageModel();
	}

	/**
	 * 
	 * @param message
	 * @return
	 */
	private MessageModel toModel(Message message) {
 		MessageElement root = new MessageElement();
 		List<SegOrGroup> children = message.children();
 		if (children != null && !children.isEmpty()) {
 			scala.collection.Iterator<SegOrGroup> it = children.iterator();
 			while (it.hasNext()) {
 				process(it.next(), "", root);
 			}			
 		}
		return  new MessageModel(root);
	}
	
	/**
	 * 
	 * @param c
	 * @param parent
	 */
	private void process(Component c, MessageElement parent) {
		Location loc = c.location();
		Req req = c.req();
 		String rep = c.toString();
		MessageElementData data = new MessageElementData(loc.path(),
				loc.desc(), req.usage().toString(), null, null,
				loc.line(), loc.column(), loc.column()
						+ (rep != null ? rep.length() : 0), loc.column(), c.instance(),
				 null, COMPONENT, rep);
		MessageElement el = new MessageElement("component", data, parent);
		if (c instanceof SimpleComponent) {
			SimpleComponent s = (SimpleComponent) c;
			MessageElementData value = new MessageElementData(loc.path(),
					loc.desc(), req.usage().toString(),null,null,
					loc.line(), loc.column(), loc.column()
							+ (rep != null ? rep.length() : 0), loc.column(),
					c.instance(), s.value().raw(), COMPONENT, rep);
			new MessageElement("value", value, el);
		} else {
			ComplexComponent cc = (ComplexComponent) c;
			List<SimpleComponent> children = cc.children();
			if (children != null && !children.isEmpty()) {
				Iterator<SimpleComponent> it = children.iterator();
				while (it.hasNext()) {
 					process(it.next(), el);
				}
			}
		}
	}
	
	/**
	 * 
	 * @param s
	 * @param parent
	 */
	private void process(SimpleComponent s, MessageElement parent) {
		Location loc = s.location();
		Req req = s.req();
		String rep = s.toString();
		MessageElementData data = new MessageElementData(loc.path(),
				loc.desc(), req.usage().toString(), null,null,
				loc.line(), loc.column(), loc.column()
						+ (rep != null ? rep.length() : 0), loc.column(), s.instance() , null, FIELD, rep);
		MessageElement el = new MessageElement("subcomponent", data, parent);
		MessageElementData value = new MessageElementData(loc.path(),
				loc.desc(), req.usage().toString(), null,null,
				loc.line(), loc.column(), loc.column()
						+ (rep != null ? rep.length() : 0), loc.column(), s.instance(), s.value().raw(), SUB_COMPONENT, rep);
		new MessageElement("value", value, el);
	}

	/**
	 * 
	 * @param f: field 
	 * @param parent: parent
	 */
	private void process(Field f, MessageElement parent) {
		Location loc = f.location();
		Req req = f.req();
 		Range card = Util.getOption(req.cardinality());
		String rep = f.toString();
		MessageElementData data = new MessageElementData(loc.path(),
				loc.desc(), req.usage().toString(), card.min(), card.max(),
				loc.line(), loc.column(), loc.column()
						+ (rep != null ? rep.length() : 0), loc.column(), f.instance(), null, FIELD, rep);
		MessageElement el = new MessageElement("field", data, parent);
		if (f instanceof SimpleField) {
			SimpleField s = (SimpleField) f;
			MessageElementData value = new MessageElementData(loc.path(),
					loc.desc(), req.usage().toString(), card.min(), card.max(),
					loc.line(), loc.column(), loc.column()
							+ (rep != null ? rep.length() : 0), loc.column(),
					f.instance(), s.value().raw(), FIELD, rep);
			new MessageElement("value", value, el);
		} else {
			ComplexField c = (ComplexField) f;
			List<Component> children = c.children();
			if (children != null && !children.isEmpty()) {
				Iterator<Component> it = children.iterator();
				while (it.hasNext()) {
 					process(it.next(), el);
				}
			}
			
		}
	}

	/**
	 * 
	 * @param e
	 * @param parentName
	 * @param parent
	 */
	private void process(SegOrGroup e, String parentName,
			MessageElement parent) {
		if (e == null) {
			return;
		}
		if (e instanceof Segment) {
			Segment s = (Segment) e;
 			Location loc = s.location();
			Req req = s.req();
 			Range card = Util.getOption(req.cardinality());
			String rep = s.toString();
			MessageElementData data = new MessageElementData(loc.path(),
					loc.desc(), req.usage().toString(), card.min(), card.max(),
					loc.line(), loc.column(), loc.column()
							+ (rep != null ? rep.length() : 0), loc.column(),
					s.instance(), null, SEGMENT, rep);
			MessageElement el = new MessageElement("segment", data,
					parent);
			List<Field> children = s.children();
			if (children != null && !children.isEmpty()) {
				Iterator<Field> it = children.iterator();
				while (it.hasNext()) {
 					process(it.next(), el);
				}
			}
			
		} else if (e instanceof Group) {
			Group g = (Group) e;
			List<SegOrGroup>  children = g.children();
			if (children != null && !children.isEmpty()) {
				scala.collection.Iterator<SegOrGroup> it = children.iterator();
				while (it.hasNext()) {
					process(it.next(), "", parent);
				}
			}
		}
	} 
	

}
