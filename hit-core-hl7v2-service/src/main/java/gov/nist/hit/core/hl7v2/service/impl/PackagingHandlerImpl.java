package gov.nist.hit.core.hl7v2.service.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import gov.nist.hit.core.domain.UploadedProfileModel;
import gov.nist.hit.core.hl7v2.service.PackagingHandler;
import gov.nist.hit.core.service.CachedRepository;

@Service
public class PackagingHandlerImpl implements PackagingHandler {
	
	@Autowired
	  protected CachedRepository cachedRepository;
	
	public List<UploadedProfileModel> getUploadedProfiles(String xml) {
	    Document doc = this.toDoc(xml);
	    NodeList nodes =  doc.getElementsByTagName("Message");
	    List<UploadedProfileModel> list = new ArrayList<UploadedProfileModel>();
	    for (int i = 0 ; i <nodes.getLength(); i++ ){
	    	Element elmIntegrationProfile =  (Element) nodes.item(i);	
	    	UploadedProfileModel upm = new UploadedProfileModel();
		    upm.setActivated(false);
		    upm.setId(elmIntegrationProfile.getAttribute("ID"));
		    upm.setName(elmIntegrationProfile.getAttribute("Name"));
		    upm.setType(elmIntegrationProfile.getAttribute("Type"));
		    upm.setEvent(elmIntegrationProfile.getAttribute("Event"));
		    upm.setStructID(elmIntegrationProfile.getAttribute("StructID"));
		    upm.setIdentifier(elmIntegrationProfile.getAttribute("Identifier"));
		    upm.setDescription(elmIntegrationProfile.getAttribute("Description"));
		    
		    if (cachedRepository.getCachedProfiles().containsKey(elmIntegrationProfile.getAttribute("ID"))) {
		    	//remove them from cache or it will trigger an error.
		    	cachedRepository.getCachedProfiles().remove(elmIntegrationProfile.getAttribute("ID"));
		    	upm.setUsed(false);
		    }else{
		    	upm.setUsed(false);
		    }
		    list.add(upm);
	    }
	    return list;
	  }

	
	public String removeUnusedAndDuplicateMessages(String content,List<UploadedProfileModel> presentMessages){
		Document doc = this.stringToDom(content);
		Element profileElement = (Element) doc.getElementsByTagName("ConformanceProfile").item(0);		
		//make ConformanceProfile id unique
		profileElement.setAttribute("ID",profileElement.getAttribute("ID")+Instant.now().toEpochMilli());
		Element conformanceProfilElementRoot = (Element) profileElement.getElementsByTagName("Messages").item(0);
		NodeList messages = conformanceProfilElementRoot.getElementsByTagName("Message");
		
		for (int j = messages.getLength() - 1; j >= 0; j--) {
			Element elmCode = (Element) messages.item(j);
			String id = elmCode.getAttribute("ID");
				
			boolean found = false;
			
			if (cachedRepository.getCachedProfiles().containsKey(id)) {
		        
		     }else{
		    	 for (UploadedProfileModel upm : presentMessages){
						if (upm.getId().equals(id)){
							found = true;
							break;
						}
		    	 }
		     }	
			if (!found){
				conformanceProfilElementRoot.removeChild(elmCode);
			}
		}

		return toString(doc);
	}
	

	@Override
	public File changeProfileId(File file) throws Exception {
		InputStream targetStream = new FileInputStream(file);
		  String content = IOUtils.toString(targetStream);
		  Document doc = this.stringToDom(content);
		  Element profileElement = (Element) doc.getElementsByTagName("ConformanceProfile").item(0);		
		  profileElement.setAttribute("ID",profileElement.getAttribute("ID")+Instant.now().toEpochMilli());
		  FileUtils.writeStringToFile(file, toString(doc));
		  return file;
	}


	@Override
	public File changeConstraintId(File file)  throws Exception{
		InputStream targetStream = new FileInputStream(file);
		  String content = IOUtils.toString(targetStream);
		  Document doc = this.stringToDom(content);
		  Element profileElement = (Element) doc.getElementsByTagName("ConformanceContext").item(0);		
		  profileElement.setAttribute("UUID",profileElement.getAttribute("UUID")+Instant.now().toEpochMilli());
		  FileUtils.writeStringToFile(file, toString(doc));
		  return file;
	}


	@Override
	public File changeVsId(File file)  throws Exception{
		InputStream targetStream = new FileInputStream(file);
		  String content = IOUtils.toString(targetStream);
		  Document doc = this.stringToDom(content);
		  Element profileElement = (Element) doc.getElementsByTagName("ValueSetLibrary").item(0);		
		  profileElement.setAttribute("ValueSetLibraryIdentifier",profileElement.getAttribute("ValueSetLibraryIdentifier")+Instant.now().toEpochMilli());
		  FileUtils.writeStringToFile(file, toString(doc));
		  return file;
	}
	
	public  File zip(List<File> files, String filename) throws Exception {
	    File zipfile = new File(filename);
	    // Create a buffer for reading the files
	    byte[] buf = new byte[1024];
	        // create the ZIP file
	        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipfile));
	        // compress the files
	        for(int i=0; i<files.size(); i++) {
	            FileInputStream in = new FileInputStream(files.get(i));
	            // add ZIP entry to output stream
	            out.putNextEntry(new ZipEntry(files.get(i).getName()));
	            // transfer bytes from the file to the ZIP file
	            int len;
	            while((len = in.read(buf)) > 0) {
	                out.write(buf, 0, len);
	            }
	            // complete the entry
	            out.closeEntry();
	            in.close();
	        }
	        // complete the ZIP file
	        out.close();
	        return zipfile;
	}
	
	protected Document stringToDom(String xmlSource) {
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
	
	protected String toString(Document doc) {
	    try {
	        StringWriter sw = new StringWriter();
	        TransformerFactory tf = TransformerFactory.newInstance();
	        Transformer transformer = tf.newTransformer();
	        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
	        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
	        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
	        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

	        transformer.transform(new DOMSource(doc), new StreamResult(sw));
	        return sw.toString();
	    } catch (Exception ex) {
	        throw new RuntimeException("Error converting to String", ex);
	    }
	}
	
	protected Document toDoc(String xmlSource) {
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
