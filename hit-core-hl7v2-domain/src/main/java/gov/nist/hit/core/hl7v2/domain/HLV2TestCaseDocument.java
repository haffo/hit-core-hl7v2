package gov.nist.hit.core.hl7v2.domain;

import gov.nist.hit.core.domain.TestCaseDocument;

import java.io.Serializable;

public class HLV2TestCaseDocument extends TestCaseDocument implements Serializable {

  private static final long serialVersionUID = 1L;
  protected boolean exMsgPresent;
  protected boolean xmlConfProfilePresent;
  protected boolean xmlValueSetLibraryPresent;



  public HLV2TestCaseDocument() {
    super();
    this.format = "hl7v2";
  }

  public boolean isExMsgPresent() {
    return exMsgPresent;
  }

  public void setExMsgPresent(boolean exMsgPresent) {
    this.exMsgPresent = exMsgPresent;
  }

  public boolean isXmlConfProfilePresent() {
    return xmlConfProfilePresent;
  }

  public void setXmlConfProfilePresent(boolean xmlConfProfilePresent) {
    this.xmlConfProfilePresent = xmlConfProfilePresent;
  }

  public boolean isXmlValueSetLibraryPresent() {
    return xmlValueSetLibraryPresent;
  }

  public void setXmlValueSetLibraryPresent(boolean xmlValueSetLibraryPresent) {
    this.xmlValueSetLibraryPresent = xmlValueSetLibraryPresent;
  }



}
