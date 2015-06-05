package gov.nist.hit.core.hl7v2.service.message;

import hl7.v2.instance.Message;
import hl7.v2.parser.impl.DefaultParser;
import hl7.v2.parser.impl.DefaultParser$class;
import scala.util.Try;

public class JParser implements DefaultParser {

	/**
	  * A java friendly way to call the `parse` method of the default parser implementation
	  * @param message - The message as a string
	  * @param model   - The message model
	  * @return The message instance model encapsulated in a scala `scala.util.Try`
	  */
	@Override
	@SuppressWarnings("unchecked")
	public Try<Message> parse(String message, hl7.v2.profile.Message model) {
		return DefaultParser$class.parse( this, message, model);
	}
	
	
	/**
	  * Call JParser.parse method and decapsulate the result
	  */
	public Message jparse(String message, hl7.v2.profile.Message model) throws Exception {
		return parse(message, model).get();
	}
 
}
