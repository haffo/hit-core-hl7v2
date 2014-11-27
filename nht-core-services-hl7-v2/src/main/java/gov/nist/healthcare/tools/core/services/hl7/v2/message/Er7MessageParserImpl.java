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

import gov.nist.healthcare.core.hl7.v2.instance.DataElement;
import gov.nist.healthcare.core.hl7.v2.instance.Element;
import gov.nist.healthcare.core.hl7.v2.parser.ParserException;
import gov.nist.healthcare.core.hl7.v2.parser.ParserImpl;
import gov.nist.healthcare.tools.core.models.MessageElement;
import gov.nist.healthcare.tools.core.models.MessageElementData;
import gov.nist.healthcare.tools.core.models.MessageModel;
import gov.nist.healthcare.tools.core.services.exception.MessageParserException;

import java.util.List;
import java.util.TreeMap;

public class Er7MessageParserImpl implements Er7MessageParser {

	@Override
	public MessageModel parse(String er7Message, Object... options)
			throws MessageParserException {
		MessageModel model = new MessageModel();
		MessageElement element = new MessageElement();
		try {
			String xmlProfile = (String) options[0];
			if (er7Message != null && !"".equals(er7Message)) {
				gov.nist.healthcare.core.hl7.v2.instance.Message instance = new ParserImpl()
						.parse(er7Message, xmlProfile);
				processChildren(instance.getChildren(), element);
			}
		} catch (ParserException e) {
			throw new MessageParserException(e.getMessage());
		}

		model.setElements(element.getChildren());
		return model;
	}

	private void processChildren(TreeMap<Integer, List<Element>> children,
			MessageElement parentNode) {
		if (children == null || children.isEmpty()) {
			return;
		}
		for (List<Element> el : children.values()) {
			for (Element e : el) {
				processElement(e, "", parentNode);
			}
		}
	}

	private void processElement(Element element, String parentName,
			MessageElement parentNode) {
		if (element == null) {
			return;
		}
		gov.nist.healthcare.core.hl7.v2.enumeration.ElementType type = element
				.getElementType();
		switch (type) {
		case GROUP:
			processChildren(element.getChildren(), parentNode);
			break;
		case SEGMENT:
			MessageElement segmentNode = new MessageElement(
					"segment",
					new gov.nist.healthcare.tools.core.models.hl7.v2.message.MessageElementData(
							element), parentNode);
			processChildren(element.getChildren(), segmentNode);
			break;
		case FIELD:
			processField(element, parentNode);
			break;
		case COMPONENT:
			processDataElement("component", element, parentNode);
			break;
		case SUB_COMPONENT:
			processDataElement("subcomponent", element, parentNode);
			break;
		}
	}

	private void processField(Element e, MessageElement parentNode) {
		gov.nist.healthcare.tools.core.models.hl7.v2.message.MessageElementData data = new gov.nist.healthcare.tools.core.models.hl7.v2.message.MessageElementData(
				e);
		MessageElement fieldNode = new MessageElement("field", data, parentNode);
		if (((DataElement) e).isPrimitive()) { 
			MessageElement el = new MessageElement(
					"value",
					new gov.nist.healthcare.tools.core.models.hl7.v2.message.MessageElementData(
							e, ((DataElement) e).getValue()), fieldNode);
		} else {
			processChildren(e.getChildren(), fieldNode);
		}
	}
 
	private void processDataElement(String type, Element e,
			MessageElement parentNode) {
		MessageElementData data = new gov.nist.healthcare.tools.core.models.hl7.v2.message.MessageElementData(
				e);
		MessageElement node1 = new MessageElement(type, data, parentNode);
		if (((DataElement) e).isPrimitive()) {
			MessageElement el = new MessageElement(
					"value",
					new gov.nist.healthcare.tools.core.models.hl7.v2.message.MessageElementData(
							e, ((DataElement) e).getValue()), node1);
		} else {
			processChildren(e.getChildren(), node1);
		}
	}

}
