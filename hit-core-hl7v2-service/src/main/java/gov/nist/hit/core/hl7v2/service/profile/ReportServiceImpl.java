package gov.nist.hit.core.hl7v2.service.profile;


import gov.nist.healthcare.unified.model.EnhancedReport;
import gov.nist.hit.core.service.ReportService;

import java.util.HashMap;

import org.springframework.stereotype.Service;

@Service
public class ReportServiceImpl implements ReportService {

  @Override
  public HashMap<String, String> getReports(String jsonReport) throws Exception {
    HashMap<String, String> resultMap = new HashMap<String, String>();
    EnhancedReport report = EnhancedReport.from("json", jsonReport);
    resultMap.put("json", report.to("json").toString());
    resultMap.put("html", report.render("iz-report", null));
    return resultMap;
  }

}
