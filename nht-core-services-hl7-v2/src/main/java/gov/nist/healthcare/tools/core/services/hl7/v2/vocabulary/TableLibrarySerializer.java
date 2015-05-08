package gov.nist.healthcare.tools.core.services.hl7.v2.vocabulary;

import gov.nist.healthcare.tools.core.models.ExtensibilityType;
import gov.nist.healthcare.tools.core.models.NoValidation;
import gov.nist.healthcare.tools.core.models.SourceType;
import gov.nist.healthcare.tools.core.models.StabilityType;
import gov.nist.healthcare.tools.core.models.StatusType;
import gov.nist.healthcare.tools.core.models.TableDefinition;
import gov.nist.healthcare.tools.core.models.TableElement;
import gov.nist.healthcare.tools.core.models.TableLibrary;
import gov.nist.healthcare.tools.core.models.TableSet;
import gov.nist.healthcare.tools.core.models.TableType;
import gov.nist.healthcare.tools.core.models.UsageType;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import nu.xom.Attribute;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


public class TableLibrarySerializer {
 
 	public String toString(TableLibrary tableLibrary) {
		nu.xom.Element elmTableLibrary = new nu.xom.Element("TableLibrary");
		elmTableLibrary.setNamespaceURI("http://www.nist.gov/healthcare/data");
		elmTableLibrary.addAttribute(new Attribute("TableLibraryIdentifier",
				tableLibrary.getTableLibraryIdentifier()));
		elmTableLibrary.addAttribute(new Attribute("Status", tableLibrary
				.getStatus().value()));
		elmTableLibrary.addAttribute(new Attribute("TableLibraryVersion",
				tableLibrary.getTableLibraryVersion()));
		elmTableLibrary.addAttribute(new Attribute("OrganizationName",
				tableLibrary.getOrganizationName()));
		elmTableLibrary.addAttribute(new Attribute("Name", tableLibrary
				.getName()));
		elmTableLibrary.addAttribute(new Attribute("Description", tableLibrary
				.getDescription()));
		
		
		if(tableLibrary.getNoValidation() != null){
			nu.xom.Element noValidation = new nu.xom.Element(
					"NoValidation");
			for(String id:tableLibrary.getNoValidation().getIds()){
				nu.xom.Element idElement = new nu.xom.Element(
						"id");
				idElement.appendChild(id);
				noValidation.appendChild(idElement);
			}
		}
		
		for (TableDefinition t : tableLibrary.getTableSet().getTableDefinitions()) {
			nu.xom.Element elmTableDefinition = new nu.xom.Element(
					"TableDefinition");
			elmTableDefinition.addAttribute(new Attribute("AlternateId", (t
					.getAlternateId() == null) ? "" : t
					.getAlternateId()));
			elmTableDefinition.addAttribute(new Attribute("Id", (t
					.getTdId() == null) ? "" : t.getTdId()));
			elmTableDefinition.addAttribute(new Attribute("Name",
					(t.getName() == null) ? "" : t.getName()));
			elmTableDefinition.addAttribute(new Attribute("Version", (t
					.getVersion() == null) ? "" : "" + t.getVersion()));
			elmTableDefinition.addAttribute(new Attribute("Codesys", (t
					.getCodesys() == null) ? "" : t.getCodesys()));
			elmTableDefinition.addAttribute(new Attribute("Oid",
					(t.getOid() == null) ? "" : t.getOid()));
			elmTableDefinition.addAttribute(new Attribute("Type", (t
					.getType() == null) ? "" : t.getType().value()));
			elmTableDefinition.addAttribute(new Attribute("Extensibility", (t
					.getExtensibility() == null) ? "" : t.getExtensibility().value()));
			elmTableDefinition.addAttribute(new Attribute("Stability", (t
					.getStability() == null) ? "" : t.getStability().value()));

			elmTableLibrary.appendChild(elmTableDefinition);

			if (t.getTableElements() != null) {
				for (TableElement c : t.getTableElements()) {
					nu.xom.Element elmTableElement = new nu.xom.Element(
							"TableElement");
					elmTableElement.addAttribute(new Attribute("Code", (c
							.getCode() == null) ? "" : c.getCode()));
					elmTableElement.addAttribute(new Attribute("DisplayName",
							(c.getDisplayName() == null) ? "" : c.getDisplayName()));
					elmTableElement.addAttribute(new Attribute("Codesys", (c
							.getCodesys() == null) ? "" : c.getCodesys()));
					elmTableElement.addAttribute(new Attribute("Source", (c
							.getSource() == null) ? "" : c.getSource().value()));
					elmTableElement.addAttribute(new Attribute("Usage", (c
							.getUsageType() == null) ? "" : c.getUsageType().value()));
					elmTableDefinition.appendChild(elmTableElement);
				}
			}

		}

		nu.xom.Document doc = new nu.xom.Document(elmTableLibrary);

		return doc.toXML();
	}

	public TableLibrary toTableLibrary(String xml) {
		Document tableLibraryDoc = this.toDoc(xml);
		TableLibrary tableLibrary = new TableLibrary();
		Element elmTableLibrary = (Element) tableLibraryDoc
				.getElementsByTagName("TableLibrary").item(0);
		tableLibrary
				.setDescription(elmTableLibrary.getAttribute("Description"));
		tableLibrary.setName(elmTableLibrary.getAttribute("Name"));
		tableLibrary.setOrganizationName(elmTableLibrary
				.getAttribute("OrganizationName"));
 		tableLibrary.setStatus(StatusType.fromValue(elmTableLibrary.getAttribute("Status")));
		tableLibrary.setTableLibraryIdentifier(elmTableLibrary
				.getAttribute("TableLibraryIdentifier"));
		tableLibrary.setTableLibraryVersion(elmTableLibrary
				.getAttribute("TableLibraryVersion"));
		
		NodeList noValidationElements = elmTableLibrary.getElementsByTagName("NoValidation");
		if(noValidationElements != null && noValidationElements.getLength() > 0){
			Element noValidationElement = (Element) noValidationElements.item(0);
			NoValidation noVal = new NoValidation();
			NodeList idElements = noValidationElement.getElementsByTagName("id");
			for (int i = 0; i < idElements.getLength(); i++) {
				Element idEl = (Element) idElements.item(i);
				String id = idEl.getTextContent();
				noVal.getIds().add(id);
			}
			tableLibrary.setNoValidation(noVal);
		}
		
		TableSet tableSet = new TableSet();
		tableLibrary.setTableSet(tableSet);
		NodeList nodes = elmTableLibrary
				.getElementsByTagName("TableDefinition");
		for (int i = 0; i < nodes.getLength(); i++) {
			Element elmTable = (Element) nodes.item(i);
			TableDefinition tableObj = new TableDefinition();
			tableObj.setCodesys(elmTable.getAttribute("Codesys"));
 			tableObj.setAlternateId(elmTable.getAttribute("AlternateId"));
			tableObj.setTdId(elmTable.getAttribute("Id"));
			tableObj.setName(elmTable.getAttribute("Name"));
			tableObj.setOid(elmTable.getAttribute("Oid"));
			tableObj.setType(TableType.fromValue(elmTable.getAttribute("Type")));
			if (StringUtils.isNotEmpty(elmTable.getAttribute("Version"))){
				tableObj.setVersion(elmTable.getAttribute("Version"));
			}
			if (StringUtils.isNotEmpty(elmTable.getAttribute("Extensibility"))) {
				tableObj.setExtensibility(ExtensibilityType.fromValue(elmTable
						.getAttribute("Extensibility")));
			}

			if (StringUtils.isNotEmpty(elmTable.getAttribute("Stability"))) {
				tableObj.setStability(StabilityType.fromValue(elmTable.getAttribute("Stability")));
			} 
			
			NodeList tableElements = elmTable.getElementsByTagName("TableElement");

			for (int j = 0; j < tableElements.getLength(); j++) {
				Element elmCode = (Element) tableElements.item(j);
				TableElement codeObj = new TableElement();
				codeObj.setCode(elmCode.getAttribute("Code"));
				codeObj.setCodesys(elmCode.getAttribute("Codesys"));
				codeObj.setDisplayName(elmCode.getAttribute("DisplayName"));
				codeObj.setSource(SourceType.fromValue(elmCode.getAttribute("Source")));
				if (StringUtils.isNotEmpty(elmCode.getAttribute("Usage"))) {
					codeObj.setUsageType(UsageType.fromValue(elmTable.getAttribute("Usage")));
				}  
				tableObj.addTableElement(codeObj);
			}
			
			tableSet.addTableDefinition(tableObj);
		}
		
		return tableLibrary;
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
}
