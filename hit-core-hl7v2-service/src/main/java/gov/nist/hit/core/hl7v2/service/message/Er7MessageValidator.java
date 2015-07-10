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


@Service
public class Er7MessageValidator implements MessageValidator {

  @Override
  public String validate(String title, String message, String... options)
      throws MessageValidationException {
    try {
      String profileXml = options[0];
      String valueSets = options[1];
      String constraintsXml = options[2];
      String constraintsXml2 = null;
      if (options.length == 3) {
        constraintsXml2 = options[3];
      }
      ConformanceContext c = null;
      Profile profile = getProfile(IOUtils.toInputStream(profileXml));
      if (constraintsXml2 != null) {
        c =
            getConformanceContext(IOUtils.toInputStream(constraintsXml),
                IOUtils.toInputStream(constraintsXml2));
      } else {
        c = getConformanceContext(IOUtils.toInputStream(constraintsXml));
      }

      // The plugin map. This should be empty if no plugin is used
      ValueSetLibrary valueSetLibrary =
          valueSets != null ? getValueSetLibrary(IOUtils.toInputStream(valueSets)) : null;
      SyncHL7Validator validator = new SyncHL7Validator(profile, valueSetLibrary, c);
      scala.collection.Iterable<String> keys = profile.messages().keys();
      String key = keys.iterator().next();
      gov.nist.validation.report.Report report = validator.check(message, key);
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
    // The get() at the end will throw an exception if something goes wrong
    ConformanceContext c = DefaultConformanceContext.apply(confContexts).get();
    return c;
  }

  private ValueSetLibrary getValueSetLibrary(InputStream vsLibXML) {
    ValueSetLibrary valueSetLibrary = ValueSetLibraryImpl.apply(vsLibXML).get();
    return valueSetLibrary;
  }

  private Profile getProfile(InputStream profileXML) {
    // The get() at the end will throw an exception if something goes wrong
    Profile profile = XMLDeserializer.deserialize(profileXML).get();

    return profile;
  }


}
