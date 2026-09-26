package io.github.migrationimpact.report;

import io.github.migrationimpact.model.ImpactReport;
import io.github.migrationimpact.model.ImpactReport.*;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ReportWriterTest {
  @Test void rendersEscapedMachineReadableReports() throws Exception {
    ImpactReport report=new ImpactReport("spring-boot:2.7","spring-boot:3.5","11");
    report.findings.add(new Finding("PREREQUISITE","Java","11","17+",Status.UPGRADE_REQUIRED,Risk.HIGH,Confidence.HIGH,"Upgrade \"Java\"","Reason",List.of(new Evidence("OFFICIAL_DOCUMENTATION","JAVA","https://example.org","docs"))));
    report.migrationOrder.add("Upgrade Java");
    assertTrue(ReportWriter.json(report).contains("Upgrade \\\"Java\\\""));
    var dir=Files.createTempDirectory("impact-report"); ReportWriter.write(report,dir,"all");
    assertTrue(Files.exists(dir.resolve("report.json"))); assertTrue(Files.readString(dir.resolve("report.html")).contains("Migration Impact"));
  }
}
