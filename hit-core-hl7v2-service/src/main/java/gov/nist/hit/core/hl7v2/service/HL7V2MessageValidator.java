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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import gov.nist.healthcare.unified.enums.Context;
import gov.nist.healthcare.unified.model.EnhancedReport;
import gov.nist.healthcare.unified.proxy.ValidationProxy;
import gov.nist.hit.core.domain.MessageValidationCommand;
import gov.nist.hit.core.domain.MessageValidationResult;
import gov.nist.hit.core.domain.TestContext;
import gov.nist.hit.core.hl7v2.domain.HL7V2TestContext;
import gov.nist.hit.core.service.MessageValidator;
import gov.nist.hit.core.service.exception.MessageException;
import gov.nist.hit.core.service.exception.MessageValidationException;
import hl7.v2.validation.content.ConformanceContext;
import hl7.v2.validation.content.DefaultConformanceContext;
import hl7.v2.validation.vs.ValueSetLibrary;
import hl7.v2.validation.vs.ValueSetLibraryImpl;

public abstract class HL7V2MessageValidator implements MessageValidator {

	@Override
	public MessageValidationResult validate(TestContext testContext, MessageValidationCommand command)
			throws MessageValidationException {
		try {
			EnhancedReport report = generateReport(testContext, command);
			if (report != null) {
				Map<String, String> nav = command.getNav();
				if (nav != null && !nav.isEmpty()) {
					report.setTestCase(nav.get("testPlan"), nav.get("testGroup"), nav.get("testCase"),
							nav.get("testStep"));
				}
				return new MessageValidationResult(report.to("json").toString(), report.render("report", null));
			}
			throw new MessageValidationException();
		} catch (MessageException e) {
			throw new MessageValidationException(e.getLocalizedMessage());
		} catch (RuntimeException e) {
			throw new MessageValidationException(e.getLocalizedMessage());
		} catch (Exception e) {
			throw new MessageValidationException(e.getLocalizedMessage());
		}
	}

	public EnhancedReport generateReport(TestContext testContext, MessageValidationCommand command)
			throws MessageValidationException {
		try {
			if (testContext instanceof HL7V2TestContext) {
				HL7V2TestContext v2TestContext = (HL7V2TestContext) testContext;
				String contextType = command.getContextType();
				String message = getMessageContent(command);
				String conformanceProfielId = v2TestContext.getConformanceProfile().getSourceId();
				String integrationProfileXml = v2TestContext.getConformanceProfile().getIntegrationProfile().getXml();
				String valueSets = v2TestContext.getVocabularyLibrary().getXml();
				String c1 = v2TestContext.getConstraints() != null ? v2TestContext.getConstraints().getXml() : null;
				String c2 = v2TestContext.getAddditionalConstraints() != null
						? v2TestContext.getAddditionalConstraints().getXml() : null;
				InputStream c1Stream = c1 != null ? IOUtils.toInputStream(c1) : null;
				InputStream c2Stream = c2 != null ? IOUtils.toInputStream(c2) : null;
				List<InputStream> cStreams = new ArrayList<InputStream>();
				if (c1Stream != null)
					cStreams.add(c1Stream);
				if (c2Stream != null)
					cStreams.add(c2Stream);
				ConformanceContext c = getConformanceContext(cStreams);
				ValueSetLibrary vsLib = valueSets != null ? getValueSetLibrary(IOUtils.toInputStream(valueSets)) : null;
				ValidationProxy vp = new ValidationProxy(getValidationServiceName(), getProviderName());
				EnhancedReport report = vp.validate(message, integrationProfileXml, c, vsLib, conformanceProfielId,
						Context.valueOf(contextType));
				if (report != null) {
					Map<String, String> nav = command.getNav();
					if (nav != null && !nav.isEmpty()) {
						report.setTestCase(nav.get("testPlan"), nav.get("testGroup"), nav.get("testCase"),
								nav.get("testStep"));
					}
				}
				return report;
			}
			throw new MessageValidationException();
		} catch (MessageException e) {
			throw new MessageValidationException(e.getLocalizedMessage());
		} catch (RuntimeException e) {
			throw new MessageValidationException(e.getLocalizedMessage());
		} catch (Exception e) {
			throw new MessageValidationException(e.getLocalizedMessage());
		}
	}

	protected ConformanceContext getConformanceContext(List<InputStream> confContexts) {
		ConformanceContext c = DefaultConformanceContext.apply(confContexts).get();
		return c;
	}

	protected ValueSetLibrary getValueSetLibrary(InputStream vsLibXML) {
		ValueSetLibrary valueSetLibrary = ValueSetLibraryImpl.apply(vsLibXML).get();
		return valueSetLibrary;
	}

	public static String getMessageContent(MessageValidationCommand command) throws MessageException {
		String message = command.getContent();
		if (message == null) {
			throw new MessageException("No message provided");
		}
		return message;
	}

	private String organizationName;

	@Override
	public String getProviderName() {
		// TODO Auto-generated method stub
		return organizationName != null ? organizationName : "NIST";
	}

	@Override
	public String getValidationServiceName() {
		// TODO Auto-generated method stub
		return getProviderName() + " Validation Tool";
	}

	public String getOrganizationName() {
		return organizationName;
	}

	public void setOrganizationName(String organizationName) {
		this.organizationName = organizationName;
	}

}
