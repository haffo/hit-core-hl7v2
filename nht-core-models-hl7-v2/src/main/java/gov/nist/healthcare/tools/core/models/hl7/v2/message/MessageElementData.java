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
package gov.nist.healthcare.tools.core.models.hl7.v2.message;

import gov.nist.healthcare.core.hl7.v2.enumeration.ElementType;
import gov.nist.healthcare.core.hl7.v2.instance.Element;
import gov.nist.healthcare.core.hl7.v2.instance.impl.Segment;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 
 * @author Harold Affo (NIST)
 *  
 */

public class MessageElementData extends gov.nist.healthcare.tools.core.models.MessageElementData implements Serializable {

	private static final long serialVersionUID = 1L;

	 

	public MessageElementData(Element element, String path, String name,
			String usage, Integer minOccurs, Integer maxOccurs) {
		this(element);
		this.path = path;
		this.name = name;
		this.value = null;
		this.usage = usage;
		this.minOccurs = minOccurs;
		this.maxOccurs = maxOccurs; 
	}

	public MessageElementData(Element element, String value) {
		this(element);
		this.value = value;
	}

	public MessageElementData(Element element) {
		this.path = element.getPath();
		if (element instanceof Segment) {
			this.name = ((Segment) element).getLongName();
		} else {
			this.name = element.getName();
		}
		this.usage = element.getUsage().getCode();
		this.minOccurs = element.getMinOccurs();
		this.maxOccurs = element.getMaxOccurs();
		this.value = null;
		this.type = element.getElementType().toString();
		this.lineNumber = element.getLineNumber();
		this.position = element.getPosition();
		this.instanceNumber = element.getInstanceNumber();
		this.description = toString();
		this.stringRepresentation = element.getStringRepresentation();
		this.startIndex = element.getColumn();
		this.endIndex = this.startIndex
				+ (this.stringRepresentation != null ? this.stringRepresentation
						.length() : 0);
	}

	public String getPath() {

		return this.path;
	}

	public void setPath(String path) {

		this.path = path;
	}

	public String getName() {

		return this.name;
	}

	public void setName(String name) {

		this.name = name;
	}

	public String getUsage() {

		return this.usage;
	}

	public void setUsage(String usage) {

		this.usage = usage;
	}

	public Integer getMinOccurs() {

		return this.minOccurs;
	}

	public void setMinOccurs(Integer minOccurs) {

		this.minOccurs = minOccurs;
	}

	public Integer getMaxOccurs() {

		return this.maxOccurs;
	}

	public void setMaxOccurs(Integer maxOccurs) {

		this.maxOccurs = maxOccurs;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	/*
	 * public Element getElement() { return element; } public void
	 * setElement(Element element) { this.element = element; }
	 */
	@Override
	public String toString() {

		if (this.value != null) {
			return this.value;
		} else {
			StringBuffer buffer1 = new StringBuffer();
			buffer1.append(this.path).append(":".charAt(0)).append(this.name)
					.append(" ".charAt(0)).append(this.usage).append("")
					.append("[".charAt(0)).append(printMinOccurs())
					.append(",".charAt(0)).append(printMaxOccurs())
					.append("]".charAt(0));
			return buffer1.toString();
		}
	}

 
	 
}
