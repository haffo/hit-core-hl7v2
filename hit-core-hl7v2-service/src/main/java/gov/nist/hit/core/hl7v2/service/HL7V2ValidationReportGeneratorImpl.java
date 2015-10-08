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

import gov.nist.healthcare.unified.converters.XMLConverter;
import gov.nist.healthcare.unified.model.EnhancedReport;
import gov.nist.hit.core.service.ValidationReportGenerator;
import gov.nist.hit.core.service.exception.ValidationReportException;

import java.io.IOException;

import org.apache.commons.io.IOUtils;

/**
 * @author Harold Affo (NIST)
 */

public class HL7V2ValidationReportGeneratorImpl extends ValidationReportGenerator {

  private static final String HTML_XSL = "/xslt/report-html.xsl";
  private static final String PDF_XSL = "/xslt/report-pdf.xsl";
  private static final String CSS = "/report.css";
  private String css = "";

  public HL7V2ValidationReportGeneratorImpl() {
    try {
      css = IOUtils.toString(HL7V2ValidationReportGeneratorImpl.class.getResourceAsStream(CSS));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }


  @Override
  public String toHTML(String xml) throws ValidationReportException {
    try {
      return addCss(super.toHTML(xml));
    } catch (IOException e) {
      throw new ValidationReportException(e);
    }
  }



  @Override
  public String toXHTML(String xml) throws ValidationReportException {
    try {
      return addCss(super.toXHTML(xml));
    } catch (IOException e) {
      throw new ValidationReportException(e);
    }
  }



  /**
   * @param htmlReport
   * @return
   * @throws IOException
   */
  private String addCss(String htmlReport) throws IOException {
    StringBuffer sb = new StringBuffer();
    sb.append("<html xmlns='http://www.w3.org/1999/xhtml'>");
    sb.append("<head>");
    sb.append("<title>HL7 V2 Message Validation Report</title>");
    sb.append("<meta http-equiv='Content-Type' content='text/html; charset=utf-8' />");
    sb.append("<style>");
    sb.append(css);
    sb.append("</style></head><body>");
    sb.append(htmlReport);
    sb.append("</body></html>");
    return sb.toString();
  }



  @Override
  public String getPdfConversionXslt() {
    try {
      return IOUtils.toString(ValidationReportGenerator.class.getResourceAsStream(PDF_XSL));
    } catch (IOException e) {
      throw new ValidationReportException(e.getMessage());
    }
  }

  @Override
  public String getHtmlConversionXslt() {
    try {
      return IOUtils.toString(HL7V2ValidationReportGeneratorImpl.class
          .getResourceAsStream(HTML_XSL));
    } catch (IOException e) {
      throw new ValidationReportException(e.getMessage());
    }
  }



  @Override
  public String toXML(String json) throws Exception {
    EnhancedReport report = EnhancedReport.from("json", json);
    String xml = new XMLConverter().convert(report);
    return xml;

  }

}
