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
    report.knowledgeVersion="2026.09.27";
    report.targetBomCoordinates="org.springframework.boot:spring-boot-dependencies";
    report.targetBomVersion="3.5.7";
    report.affectedSourceFiles.add("src/main/java/Legacy.java");
    report.findings.add(new Finding("PREREQUISITE","Java","11","17+",Status.UPGRADE_REQUIRED,Risk.HIGH,Confidence.HIGH,"Upgrade \"Java\"","Reason",List.of(new Evidence("OFFICIAL_DOCUMENTATION","JAVA","https://example.org/?q=\"unsafe\"","docs"))));
    Finding library=new Finding("LIBRARY_COMPATIBILITY","org.example:legacy","1.0","2.x",Status.UPGRADE_REQUIRED,Risk.HIGH,Confidence.HIGH,"Upgrade once","Reason",List.of(new Evidence("OFFICIAL_DOCUMENTATION","LIBRARY","https://example.org/library","docs")));
    report.findings.add(library);
    report.findings.add(library);
    report.migrationOrder.add("Upgrade Java");
    String json=ReportWriter.json(report);
    assertTrue(json.contains("Upgrade \\\"Java\\\""));
    assertTrue(json.contains("\"affectedSourceFiles\":1"));
    assertTrue(json.contains("\"requiredLibraryUpdates\":1"));
    assertEquals(1,count(json,"org.example:legacy"));
    String console=ReportWriter.console(report);
    assertTrue(console.contains("REQUIRED CHANGES"));
    assertTrue(console.contains("Detected affected source files        : 1"));
    assertTrue(console.contains("Transitive BOM changes (not repeated) : 0"));
    assertEquals(1,count(console,"org.example:legacy"));
    var dir=Files.createTempDirectory("impact-report"); ReportWriter.write(report,dir,"all");
    assertTrue(Files.exists(dir.resolve("report.json")));
    String html=Files.readString(dir.resolve("report.html"));
    assertTrue(html.contains("Content-Security-Policy"));
    assertTrue(html.contains("name=\"referrer\" content=\"no-referrer\""));
    assertTrue(html.contains("Migration Impact"));
    assertTrue(html.contains("&quot;unsafe&quot;"));
  }
  private static int count(String value,String needle) { return value.split(java.util.regex.Pattern.quote(needle),-1).length-1; }
}
