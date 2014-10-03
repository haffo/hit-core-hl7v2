<schema xmlns="http://www.ascc.net/xml/schematron" >
    <title>IHE SOAP/WSA Schematron</title>
    <ns prefix="soap" uri="http://www.w3.org/2003/05/soap-envelope"/>
    <ns prefix="wsa" uri="http://www.w3.org/2005/08/addressing"/>
    <phase id='errors'>
        <active pattern='Soap-errors'/>
    </phase>
     <pattern name="Soap" id='Soap-errors'>
          <rule context='soap:Envelope'>
   			<assert test='soap:Header'>
      			HL7-WSA100: HL7 WS Messages MUST use the WS-Addressing framework for SOAP-based messaging
   			</assert>
   			<assert test='count(soap:Header[1]/wsa:Action[1])!=1 or (normalize-space(soap:Header[1]/wsa:Action[1]/text()) = "urn:hl7-org:v3:QUQI_IN000003UV01_Continue" and normalize-space(concat(namespace-uri(soap:Body[1]/*[1]),":",local-name(soap:Body[1]/*[1])))= "urn:hl7-org:v3:QUQI_IN000003UV01")  or normalize-space(concat(namespace-uri(soap:Body[1]/*[1]),":",local-name(soap:Body[1]/*[1]))) = normalize-space(soap:Header[1]/wsa:Action[1]/text())'>  				
   				HL7-WSP208: WSDL messages for Interactions SHOULD use wsdl:operation/wsdl:input/@wsa:Action = "urn:hl7-org:v3:{Interaction_Artifact_ID}
   				See HL7 V3 TRANS WS R2 section 3.3.2.3
   				
   				HL7-WSP100: The top-level element of the HL7 message MUST be embedded as the only child of the soap:Body element. The name of the top-level element (the one directly under the soap:Body element) MUST be {Interaction Artifact Id}.
   				See HL7 V3 TRANS WS R2 section 3.2.1
   				
   				The value <value-of select="normalize-space(soap:Header[1]/wsa:Action[1]/text())"/> provided in wsa:Action does not match the name of the top element <value-of select="normalize-space(concat(namespace-uri(soap:Body[1]/*[1]),':',local-name(soap:Body[1]/*[1])))"/>
   			</assert>
          </rule>    
              
          <rule context='soap:Header'>
   			<assert test='count(wsa:Action)=1'>
					HL7-WSP207 For each input and output message defined in the WSDL portType an attribute wsa:Action MUST be defined.
					See HL7 V3 TRANS WS R2 section 3.3.2.3  
					
					The element wsa:Action is required. 			
			</assert>
          </rule>
          
          <rule context='wsa:Action'>
          	<!-- wsa:Action starts with urn:hl7-org:v3: -->
   			<assert test='starts-with(text(),"urn:hl7-org:v3:")'>
   				HL7-WSP208: WSDL messages for Interactions SHOULD use wsdl:operation/wsdl:input/@wsa:Action = "urn:hl7-org:v3:{Interaction_Artifact_ID}.
   				See HL7 V3 TRANS WS R2 section 3.3.2.3
   				
   				The value provided in wsa:Action is not fully qualified with "urn:hl7-org:v3" : <value-of select="normalize-space(text())"/>
			</assert>
			<!-- wsa:Action ends with {Interaction Artifact_ID} -->
			<assert test='ends-with(normalize-space(text()),"PRPA_IN201301UV02") or ends-with(normalize-space(text()), "PRPA_IN201302UV02") or ends-with(normalize-space(text()),"PRPA_IN201304UV02") or ends-with(normalize-space(text()),"PRPA_IN201305UV02") or ends-with(normalize-space(text()),"PRPA_IN201306UV02") or ends-with(normalize-space(text()),"PRPA_IN201309UV02") or ends-with(normalize-space(text()),"PRPA_IN201310UV02") or ends-with(normalize-space(text()),"QUQI_IN000003UV01_Continue") or ends-with(normalize-space(text()),"QUQI_IN000003UV01") or ends-with(normalize-space(text()),"QUQI_IN000003UV01_Cancel") or ends-with(normalize-space(text()),"MCCI_IN000002UV01")'>
			 	HL7-WSP208: WSDL messages for Interactions SHOULD use wsdl:operation/wsdl:input/@wsa:Action = "urn:hl7-org:v3:{Interaction_Artifact_ID}
			 	See HL7 V3 TRANS WS R2 section 3.3.2.3
			 	
			 	The value <value-of select="normalize-space(text())"/> provided for wsa:Action is not valid.
			 	Valid values are : PRPA_IN201301UV02, PRPA_IN201302UV02, PRPA_IN201304UV02, PRPA_IN201305UV02, PRPA_IN201306UV02, PRPA_IN201309UV02, PRPA_IN201310UV02, QUQI_IN000003UV01, QUQI_IN000003UV01_Cancel or MCCI_IN000002UV01
			</assert>	
          </rule>
          
          <rule context='soap:Body'>
   			<assert test='count(*)=1'>
      			HL7-WSP100: The top-level element of the HL7 message MUST be embedded as the only child of the soap:Body element. The name of the top-level element (the one directly under the soap:Body element) MUST be {Interaction Artifact Id}.
   				See HL7 V3 TRANS WS R2 section 3.2.1
   				
   				The soap:Body element in the message has <value-of select="count(*)"/>. Only one child is allowed.
   			</assert>
          </rule>
    
          <rule context='soap:Body[1]/*[1]'>          	
            <assert test='normalize-space(local-name()) = "PRPA_IN201301UV02" or normalize-space(local-name()) = "PRPA_IN201302UV02" or normalize-space(local-name()) = "PRPA_IN201304UV02" or normalize-space(local-name()) = "PRPA_IN201305UV02" or normalize-space(local-name()) = "PRPA_IN201306UV02" or normalize-space(local-name()) = "PRPA_IN201309UV02" or normalize-space(local-name()) = "PRPA_IN201310UV02" or normalize-space(local-name()) = "QUQI_IN000003UV01" or normalize-space(local-name()) = "QUQI_IN000003UV01_Cancel" or normalize-space(local-name()) = "MCCI_IN000002UV01"'>
			 	HL7-WSP100: The top-level element of the HL7 message MUST be embedded as the only child of the soap:Body element. The name of the top-level element (the one directly under the soap:Body element) MUST be {Interaction Artifact Id}.
   				See HL7 V3 TRANS WS R2 section 3.2.1
   				
   				The value <value-of select="normalize-space(local-name())"/> provided is not valid.
			 	Valid values are : PRPA_IN201301UV02, PRPA_IN201302UV02, PRPA_IN201304UV02, PRPA_IN201305UV02, PRPA_IN201306UV02, PRPA_IN201309UV02, PRPA_IN201310UV02, QUQI_IN000003UV01, QUQI_IN000003UV01_Cancel or MCCI_IN000002UV01
			</assert>
          </rule>
     </pattern>
     
</schema>