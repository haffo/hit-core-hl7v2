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

import gov.nist.healthcare.unified.enums.Context;
import gov.nist.healthcare.unified.model.EnhancedReport;
import gov.nist.healthcare.unified.proxy.ValidationProxy;
import gov.nist.hit.core.service.MessageValidator;
import gov.nist.hit.core.service.exception.MessageValidationException;
import hl7.v2.profile.Profile;
import hl7.v2.profile.XMLDeserializer;
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
  public String validate(String... args) throws MessageValidationException {
    try {
      if (args == null || args.length < 5) {
        throw new MessageValidationException("Invalid Message Validation Arguments");
      }
      String title = args[0];
      String contextType = args[1];

      String message = args[2];
      String conformanceProfielId = args[3];
      String integrationProfileXml = args[4];
      String valueSets = args[5];

      String c1 = args[6];
      String c2 = args.length >= 7 && args[7] != null ? args[7] : null;
      ConformanceContext c =
          c2 != null ? getConformanceContext(IOUtils.toInputStream(c1), IOUtils.toInputStream(c2))
              : getConformanceContext(IOUtils.toInputStream(c1));
      ValueSetLibrary vsLib =
          valueSets != null ? getValueSetLibrary(IOUtils.toInputStream(valueSets)) : null;
      ValidationProxy vp = new ValidationProxy(title, "NIST", "1.0");
      EnhancedReport report =
          vp.validate(message, integrationProfileXml, c, vsLib, conformanceProfielId,
              Context.valueOf(contextType));
      return report.to("json").toString();
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
