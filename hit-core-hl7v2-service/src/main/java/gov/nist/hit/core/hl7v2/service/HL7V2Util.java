package gov.nist.hit.core.hl7v2.service;

import java.util.regex.Pattern;

public class HL7V2Util {


  public static int getPosition(String path, String type) {
    switch (type) {
      case "SEGMENT": {
        return 1;
      }
      case "FIELD": {
        // MSH[1].7[1]
        return Integer.parseInt(path.split(Pattern.quote("."))[1].split(Pattern.quote("["))[0]);
      }
      case "COMPONENT": {
        // MSH[1].7[1].3[1]
        return Integer.parseInt(path.split(Pattern.quote("."))[2].split(Pattern.quote("["))[0]);
      }
      case "SUB_COMPONENT": {
        // MSH[1].7[1].3[1].4[1]
        return Integer.parseInt(path.split(Pattern.quote("."))[3].split(Pattern.quote("["))[0]);
      }
    }
    return -1;
  }

  /**
   * Use at your own risk
   * 
   * @param path
   * @return
   */
  public static String getType(String path) {
    if (path.split(Pattern.quote(".")).length == 2) {
      return "FIELD";
    } else if (path.split(Pattern.quote(".")).length == 3) {
      return "COMPONENT";
    } else if (path.split(Pattern.quote(".")).length == 4) {
      return "SUB_COMPONENT";
    } else if (path.split(Pattern.quote(".")).length == 1) {
      return "SEGMENT";
    }
    throw new IllegalArgumentException("Invalid Path " + path);
  }

  /**
   * 
   * @param path
   * @return
   */
  public static int getPosition(String path) {
    return getPosition(path, getType(path));
  }


}
