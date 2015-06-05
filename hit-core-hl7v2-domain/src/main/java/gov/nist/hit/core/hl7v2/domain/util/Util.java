package gov.nist.hit.core.hl7v2.domain.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import scala.Option;

public class Util {
	
	/**
	 * Returns the content of the Scala option or null  
	 * @param o - The scala option
	 * @return The content of the scala option or null
	 */
	public static <T> T getOption(Option<T> o) {
		try { 
			return o.get(); 
		} catch( Exception e) {
			return null;
		}
	}
	
	/**
	 * Returns the content of the file as stream
	 * @param fileName
	 * @return
	 * @throws Exception
	 */
	public static String streamAsString(String fileName) throws Exception {
		try {     
			InputStream in = Util.class.getResourceAsStream(fileName);
			BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
			String str = null;
			StringBuilder sb = new StringBuilder();
			while ((str = r.readLine()) != null) {
				sb.append(str); sb.append("\n");
			}
			return sb.toString();
		} catch (Exception e) {
			throw e;
		}
	}

}
