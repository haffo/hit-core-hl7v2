/*
 * Meaningful Use Core DefaultTableLibraryUnmarshaller.java October 14, 2011
 * 
 * This code was produced by the National Institute of Standards and Technology (NIST). See the
 * 'nist.disclaimer' file given in the distribution for information on the use and redistribution of
 * this software.
 */
package gov.nist.healthcare.tools.core.services.hl7.v2.vocabulary;

import gov.nist.healthcare.tools.core.services.TableLibraryUnmarshaller;
 

/**
 * This class loads the table library definitions for different code systems
 * 
 * @author Harold Affo(NIST)
 */
public class TableLibraryUnmarshallerImpl extends TableLibraryUnmarshaller {

	static final String SCHEMA_LOCATION = "/schema/TableLibrary.xsd";

	public TableLibraryUnmarshallerImpl(String schemaLocation) {
		this.schemaLocation = schemaLocation;
	}

	public TableLibraryUnmarshallerImpl() {
		this.schemaLocation = SCHEMA_LOCATION;
	}

}
