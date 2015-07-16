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
package gov.nist.hit.core.hl7v2.service.message;

import gov.nist.hit.core.service.MessageValidator;
import gov.nist.hit.core.service.exception.MessageValidationException;
import hl7.v2.profile.Profile;
import hl7.v2.profile.XMLDeserializer;
import hl7.v2.validation.SyncHL7Validator;
import hl7.v2.validation.content.ConformanceContext;
import hl7.v2.validation.content.DefaultConformanceContext;
import hl7.v2.validation.vs.ValueSetLibrary;
import hl7.v2.validation.vs.ValueSetLibraryImpl;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;


@Service("er7MessageValidator")
public class Er7MessageValidator implements MessageValidator {

  @Override
  public String validate(String title, String message, String... options)
      throws MessageValidationException {
    try {
      if (options == null || options.length < 3) {
        throw new MessageValidationException("Invalid Message Validation Arguments");
      }
      String conformanceProfielId = options[0];
      String integrationProfileXml = options[1];
      String valueSets = options[2];
      String constraintsXml = options[3];
      String constraintsXml2 = options.length >= 4 && options[4] != null ? options[4] : null;
      Profile profile = getProfile(IOUtils.toInputStream(integrationProfileXml));
      ConformanceContext c =
          constraintsXml2 != null ? getConformanceContext(IOUtils.toInputStream(constraintsXml),
              IOUtils.toInputStream(constraintsXml2)) : getConformanceContext(IOUtils
              .toInputStream(constraintsXml));
      ValueSetLibrary valueSetLibrary =
          valueSets != null ? getValueSetLibrary(IOUtils.toInputStream(valueSets)) : null;
      SyncHL7Validator validator = new SyncHL7Validator(profile, valueSetLibrary, c);
      gov.nist.validation.report.Report report = validator.check(message, conformanceProfielId);
      String res = report.toJson();
      return res;
    } catch (RuntimeException e) {
      throw new MessageValidationException(e);
    } catch (Exception e) {
      throw new MessageValidationException(e);
    }
  }

  private ConformanceContext getConformanceContext(InputStream... constraints) {
    List<InputStream> confContexts = Arrays.asList(constraints);
    ConformanceContext c = DefaultConformanceContext.apply(confContexts).get();
    return c;
  }

  private ValueSetLibrary getValueSetLibrary(InputStream vsLibXML) {
    ValueSetLibrary valueSetLibrary = ValueSetLibraryImpl.apply(vsLibXML).get();
    return valueSetLibrary;
  }

  private Profile getProfile(InputStream profileXML) {
    Profile profile = XMLDeserializer.deserialize(profileXML).get();
    return profile;
  }


}
