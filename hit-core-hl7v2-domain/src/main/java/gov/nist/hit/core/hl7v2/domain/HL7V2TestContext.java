package gov.nist.hit.core.hl7v2.domain;

import gov.nist.hit.core.domain.ConformanceProfile;
import gov.nist.hit.core.domain.Constraints;
import gov.nist.hit.core.domain.TestContext;
import gov.nist.hit.core.domain.VocabularyLibrary;

import java.io.Serializable;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
public class HL7V2TestContext extends TestContext implements Serializable {


  private static final long serialVersionUID = 1L;


  @OneToOne(cascade = CascadeType.ALL)
  @JoinColumn(unique = true, nullable = false, insertable = true, updatable = true)
  @JsonProperty(value = "profile")
  protected ConformanceProfile conformanceProfile;

  @ManyToOne
  protected VocabularyLibrary vocabularyLibrary;

  @JsonIgnore
  @ManyToOne
  protected Constraints constraints;

  @JsonIgnore
  @OneToOne(cascade = CascadeType.ALL)
  @JoinColumn(unique = true, nullable = true, insertable = true, updatable = true)
  protected Constraints addditionalConstraints;


  public HL7V2TestContext() {}

  public ConformanceProfile getConformanceProfile() {
    return conformanceProfile;
  }

  public void setConformanceProfile(ConformanceProfile conformanceProfile) {
    this.conformanceProfile = conformanceProfile;
  }


  public Constraints getConstraints() {
    return constraints;
  }

  public void setConstraints(Constraints constraints) {
    this.constraints = constraints;
  }

  public VocabularyLibrary getVocabularyLibrary() {
    return vocabularyLibrary;
  }

  public void setVocabularyLibrary(VocabularyLibrary vocabularyLibrary) {
    this.vocabularyLibrary = vocabularyLibrary;
  }

  public Constraints getAddditionalConstraints() {
    return addditionalConstraints;
  }

  public void setAddditionalConstraints(Constraints addditionalConstraints) {
    this.addditionalConstraints = addditionalConstraints;
  }



}
