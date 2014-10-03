/**
 * This software was developed at the National Institute of Standards and Technology by employees
 * of the Federal Government in the course of their official duties. Pursuant to title 17 Section 105 of the
 * United States Code this software is not subject to copyright protection and is in the public domain.
 * This is an experimental system. NIST assumes no responsibility whatsoever for its use by other parties,
 * and makes no guarantees, expressed or implied, about its quality, reliability, or any other characteristic.
 * We would appreciate acknowledgement if the software is used. This software can be redistributed and/or
 * modified freely provided that any derivative works bear some notice that they are derived from it, and any
 * modified versions bear some notice that they have been modified.
 */
package gov.nist.healthcare.tools.core.services.hl7.v2.message;

import gov.nist.healthcare.tools.core.services.validation.ValidationReportException;
import gov.nist.healthcare.tools.core.services.validation.ValidationReportGenerator;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

/**
 * @author Harold Affo (NIST)
 */

public abstract class Er7ValidationReportGenerator extends ValidationReportGenerator {
	private final static Logger logger = Logger
			.getLogger(Er7ValidationReportGenerator.class);
	
	private static final String HTML_XSL = "/xslt/HL7V2HTML.xsl";

	private static final String PDF_XSL = "/xslt/HL7V2PDF.xsl";

	public Er7ValidationReportGenerator() {

	}

	/**
	 * @param htmlReport
	 * @return
	 */
	@Override
	public String addStyleSheet(String htmlReport) {
		StringBuffer sb = new StringBuffer();
		sb.append("<html xmlns='http://www.w3.org/1999/xhtml'>");
		sb.append("<head>");
		sb.append("<title>Message Validation Report</title>");
		sb.append("<meta http-equiv='Content-Type' content='text/html; charset=utf-8' />");
		sb.append("<style>.row4 a, .row3 a {color: #003399; text-decoration: underline;}");
		sb.append(".row4 a:hover, .row3 a:hover { color: #000000; text-decoration: underline;}");
		sb.append(".headerReport {width: 250px;}");
		sb.append(".row1 {vertical-align: top;background-color: #EFEFEF;width: 100px;}");
		sb.append(".row2 {background-color: #DEE3E7;width: 100px;}");
		sb.append(".row3 {background-color: #D1D7DC;vertical-align: top;}");
		sb.append(".row4 { background-color: #EFEFEF;vertical-align: top;}");
		sb.append(".row5 { background-color: #FFEC9D;vertical-align: top;}");
		sb.append(".forumline { background-color:#FFFFFF;border: 2px #006699 solid;width: 700px;}");
		sb.append(".maintitle {font-weight: bold;font-size: 22px;"
				+ "font-family: Georgia, Verdana;text-decoration: none;line-height : 120%;color : #000000;}");
		sb.append("</style></head><body>");
		sb.append(htmlReport);
		sb.append("</body></html>");
		return sb.toString();
	}

	@Override
	public String getPdfConversionXslt() {
		try {
			return IOUtils.toString(Er7ValidationReportGenerator.class
					.getResourceAsStream(PDF_XSL));
		} catch (IOException e) {
			throw new ValidationReportException(e.getMessage());
		}
	}

	@Override
	public String getHtmlConversionXslt() {
		try {
			return IOUtils.toString(Er7ValidationReportGenerator.class
					.getResourceAsStream(HTML_XSL));
		} catch (IOException e) {
			throw new ValidationReportException(e.getMessage());
		}
	}

	private String getFileName(String title, String extension) {
		String fileName = null;
		if (title != null) {
			fileName = title + "-ValidationReport";
		} else {
			fileName = "MessageValidationReport";
		}
		return fileName + "." + extension;
	}

}
