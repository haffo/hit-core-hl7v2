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
package gov.nist.healthcare.tools.core.services.hl7.v2.soap;

import gov.nist.healthcare.tools.core.models.message.MessageElement;
import gov.nist.healthcare.tools.core.models.message.MessageModel;
import gov.nist.healthcare.tools.core.models.soap.SoapMessageElementData;
import gov.nist.healthcare.tools.core.services.soap.SoapMessageParser;
import gov.nist.healthcare.tools.core.services.soap.SoapMessageParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.located.LocatedJDOMFactory;
import org.jdom2.output.XMLOutputter;

public class SoapMessageParserImpl implements SoapMessageParser {

	@Override
	public MessageModel parse(String soapXml, Object...options) throws SoapMessageParserException {
 			return parse(IOUtils.toInputStream(soapXml));
	}

 	private MessageModel parse(InputStream soapXml)
			throws SoapMessageParserException {
		try {
			SAXBuilder builder = new SAXBuilder();
			builder.setJDOMFactory(new LocatedJDOMFactory());
			builder.setExpandEntities(false);
			Document document = builder.build(soapXml);
			XMLOutputter xml = new XMLOutputter();
			System.out.println(xml.outputString(document));
			Element element = document.getRootElement();
			MessageModel model = new MessageModel();
			SoapMessageElementData data = new SoapMessageElementData(element);
			MessageElement parentNode = getSoapMessageElement(data);
			processChildren(element.getChildren(), parentNode);
			model.getElements().add(parentNode);
			return model;
		} catch (JDOMException | IOException e) {
			throw new SoapMessageParserException(e);
		}
	}

 	private void processChildren(List<Element> childElements,
			MessageElement parent) {
		for (int i = 0; i < childElements.size(); i++) {
			Element element = childElements.get(i);
			MessageElement childNode = getSoapMessageElement(
					new SoapMessageElementData(element), parent);
			if (!element.getChildren().isEmpty()) {
				processChildren(element.getChildren(), childNode);
			} else {
				getSoapMessageElement(new SoapMessageElementData(element),
						element.getValue(), childNode);
			}
		}
	}
	
	
	private MessageElement getSoapMessageElement(SoapMessageElementData data) {
		return getSoapMessageElement(data, null);
	}

	private MessageElement getSoapMessageElement(SoapMessageElementData data,
			MessageElement parent) {
		MessageElement element = new MessageElement();
		element.setData(data);
		List<MessageElement> children = new ArrayList<MessageElement>();
		if (parent != null) {
			if (parent.getChildren() == null) {
				parent.setChildren(new ArrayList<MessageElement>());
			}
			parent.getChildren().add(element);
		}
		element.setChildren(children);
		element.setLabel(data.getName());
		return element;
	}

	
	private MessageElement getSoapMessageElement(SoapMessageElementData data,String label,
			MessageElement parent) {
		MessageElement element = getSoapMessageElement(data, parent);
		element.setLabel(label);
		return element;
	}

	 

}
