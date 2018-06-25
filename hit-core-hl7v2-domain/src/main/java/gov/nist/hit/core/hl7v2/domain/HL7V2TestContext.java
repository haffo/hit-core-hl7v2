package gov.nist.hit.core.hl7v2.domain;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import gov.nist.hit.core.domain.ConformanceProfile;
import gov.nist.hit.core.domain.Constraints;
import gov.nist.hit.core.domain.TestContext;
import gov.nist.hit.core.domain.VocabularyLibrary;

@Entity
public class HL7V2TestContext extends TestContext {


  private static final long serialVersionUID = 1L;

  private boolean dqa;

  @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(unique = true, nullable = false, insertable = true, updatable = true)
  @JsonProperty(value = "profile")
  protected ConformanceProfile conformanceProfile;

  @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  protected VocabularyLibrary vocabularyLibrary;

  @JsonIgnore
  @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @JoinColumn(nullable = true, insertable = true, updatable = true)
  protected Constraints constraints;

  @JsonIgnore
  @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(nullable = true, insertable = true, updatable = true)
  protected Constraints addditionalConstraints;


  public HL7V2TestContext() {
    this.format = "hl7v2";
  }

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

  public boolean isDqa() {
    return dqa;
  }

  public void setDqa(boolean dqa) {
    this.dqa = dqa;
  }



}
