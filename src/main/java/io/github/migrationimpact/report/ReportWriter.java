package io.github.migrationimpact.report;

import io.github.migrationimpact.model.ImpactReport;
import io.github.migrationimpact.model.ImpactReport.Finding;
import io.github.migrationimpact.model.ImpactReport.Status;
import io.github.migrationimpact.model.ImpactReport.Summary;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class ReportWriter {
  private ReportWriter() {}

  public static String console(ImpactReport report) {
    StringBuilder out=new StringBuilder();
    out.append("\n============================================================\n")
        .append("MIGRATION IMPACT ANALYZER\n")
        .append(report.source).append(" -> ").append(report.target).append('\n')
        .append("============================================================\n")
        .append("Overall risk : ").append(report.risk()).append('\n')
        .append("Knowledge    : ").append(report.knowledgeVersion).append('\n');
    if(report.targetBomVersion!=null) out.append("Target BOM   : ").append(report.targetBomCoordinates).append(':').append(report.targetBomVersion).append('\n');

    List<Finding> findings=report.uniqueFindings();
    appendSection(out,"REQUIRED CHANGES",findings,f -> f.status()==Status.UPGRADE_REQUIRED || f.status()==Status.INCOMPATIBLE);
    appendSection(out,"RECOMMENDED CHANGES",findings,f -> f.status()==Status.UPGRADE_RECOMMENDED);
    appendSection(out,"MANUAL REVIEW",findings,f -> f.status()==Status.UNKNOWN);
    appendSection(out,"CONFIRMED COMPATIBILITY",findings,f -> f.status()==Status.SUPPORTED || f.status()==Status.COMPATIBLE);

    if(!report.migrationOrder.isEmpty()) {
      out.append("\nMIGRATION ORDER\n---------------\n");
      for(int i=0;i<report.migrationOrder.size();i++) out.append(i+1).append(". ").append(report.migrationOrder.get(i)).append('\n');
    }
    appendSummary(out,report.summary());
    return out.toString();
  }

  private static void appendSection(StringBuilder out, String title, List<Finding> all, Predicate<Finding> matches) {
    List<Finding> findings=all.stream().filter(matches).toList();
    if(findings.isEmpty()) return;
    out.append("\n").append(title).append(" (").append(findings.size()).append(")\n").append("-".repeat(title.length()+4)).append('\n');
    for(int i=0;i<findings.size();i++) {
      Finding finding=findings.get(i);
      out.append(i+1).append(". ").append(finding.subject()).append(" [").append(finding.risk()).append("]\n")
          .append("   Current : ").append(finding.current()).append('\n')
          .append("   Target  : ").append(finding.target()).append('\n')
          .append("   Action  : ").append(finding.recommendation()).append('\n')
          .append("   Why     : ").append(finding.reason()).append('\n');
      for(int e=0;e<finding.evidence().size();e++) { var evidence=finding.evidence().get(e); out.append(e==0?"   Evidence: ":"             ").append(evidence.ruleId()).append(" — ").append(evidence.reference()).append('\n'); }
    }
  }

  private static void appendSummary(StringBuilder out, Summary summary) {
    out.append("\nIMPACT SUMMARY\n--------------\n")
        .append("Detected affected source files        : ").append(summary.affectedSourceFiles()).append('\n')
        .append("Required library updates              : ").append(summary.requiredLibraryUpdates()).append('\n')
        .append("Recommended library updates           : ").append(summary.recommendedLibraryUpdates()).append('\n')
        .append("Libraries requiring manual review     : ").append(summary.librariesToReview()).append('\n')
        .append("Direct changes selected by target BOM  : ").append(summary.managedDirectChanges()).append('\n')
        .append("Transitive BOM changes (not repeated) : ").append(summary.managedTransitiveChanges()).append('\n')
        .append("Unique findings                       : ").append(summary.totalFindings()).append('\n');
  }

  public static void write(ImpactReport report, Path output, String format) throws IOException {
    Files.createDirectories(output);
    if(format.equals("json") || format.equals("all")) Files.writeString(output.resolve("report.json"),json(report),StandardCharsets.UTF_8);
    if(format.equals("html") || format.equals("all")) Files.writeString(output.resolve("report.html"),html(report),StandardCharsets.UTF_8);
  }

  static String json(ImpactReport report) {
    Summary summary=report.summary();
    String findings=report.uniqueFindings().stream().map(ReportWriter::jsonFinding).collect(Collectors.joining(",\n"));
    return "{\n"+
        "  \"source\":\""+j(report.source)+"\",\n"+
        "  \"target\":\""+j(report.target)+"\",\n"+
        "  \"overallRisk\":\""+report.risk()+"\",\n"+
        "  \"metadata\":{\"knowledgeVersion\":\""+j(report.knowledgeVersion)+"\",\"knowledgeSource\":\""+j(report.knowledgeSource)+"\",\"targetBom\":"+nullableJson(joinBom(report))+"},\n"+
        "  \"summary\":{\"affectedSourceFiles\":"+summary.affectedSourceFiles()+",\"requiredLibraryUpdates\":"+summary.requiredLibraryUpdates()+",\"recommendedLibraryUpdates\":"+summary.recommendedLibraryUpdates()+",\"librariesToReview\":"+summary.librariesToReview()+",\"managedDirectChanges\":"+summary.managedDirectChanges()+",\"managedTransitiveChanges\":"+summary.managedTransitiveChanges()+",\"totalFindings\":"+summary.totalFindings()+"},\n"+
        "  \"affectedFiles\":["+report.affectedSourceFiles.stream().map(x -> "\""+j(x)+"\"").collect(Collectors.joining(","))+"],\n"+
        "  \"findings\":[\n"+findings+"\n  ],\n"+
        "  \"migrationOrder\":["+report.migrationOrder.stream().map(x -> "\""+j(x)+"\"").collect(Collectors.joining(","))+ "]\n"+
        "}\n";
  }

  private static String jsonFinding(Finding finding) {
    return "    {\"category\":\""+j(finding.category())+"\",\"subject\":\""+j(finding.subject())+"\",\"current\":\""+j(finding.current())+"\",\"target\":\""+j(finding.target())+"\",\"status\":\""+finding.status()+"\",\"risk\":\""+finding.risk()+"\",\"confidence\":\""+finding.confidence()+"\",\"recommendation\":\""+j(finding.recommendation())+"\",\"reason\":\""+j(finding.reason())+"\",\"evidence\":["+finding.evidence().stream().map(e -> "{\"type\":\""+j(e.type())+"\",\"ruleId\":\""+j(e.ruleId())+"\",\"reference\":\""+j(e.reference())+"\"}").collect(Collectors.joining(","))+"]}";
  }

  static String html(ImpactReport report) {
    Summary summary=report.summary();
    String rows=report.uniqueFindings().stream().map(f -> "<tr><td>"+h(f.status().toString())+"</td><td>"+h(f.subject())+"</td><td>"+h(f.current())+"</td><td>"+h(f.target())+"</td><td>"+h(f.recommendation())+"</td><td>"+evidenceHtml(f)+"</td></tr>").collect(Collectors.joining());
    return "<!doctype html><html><head><meta charset=\"utf-8\"><title>Migration Impact Report</title><style>body{font-family:system-ui;margin:2rem;color:#172033}.meta,.summary{display:grid;grid-template-columns:repeat(auto-fit,minmax(180px,1fr));gap:.75rem;margin:1rem 0}.card{border:1px solid #d9dfeb;border-radius:8px;padding:.8rem}.value{font-size:1.4rem;font-weight:700}table{border-collapse:collapse;width:100%;margin-top:1.5rem}td,th{border:1px solid #d9dfeb;padding:.65rem;text-align:left;vertical-align:top}th{background:#f3f5f9}</style></head><body><h1>Migration Impact Analyzer</h1><p>"+h(report.source)+" &rarr; "+h(report.target)+"</p><div class=\"meta\"><div class=\"card\"><div>Overall risk</div><div class=\"value\">"+report.risk()+"</div></div><div class=\"card\"><div>Knowledge</div><div class=\"value\">"+h(report.knowledgeVersion)+"</div></div><div class=\"card\"><div>Target BOM</div><div>"+h(joinBom(report)==null?"Unavailable":joinBom(report))+"</div></div></div><h2>Impact summary</h2><div class=\"summary\">"+card("Affected files",summary.affectedSourceFiles())+card("Required library updates",summary.requiredLibraryUpdates())+card("Recommended updates",summary.recommendedLibraryUpdates())+card("Manual reviews",summary.librariesToReview())+card("Managed direct changes",summary.managedDirectChanges())+card("Hidden transitive changes",summary.managedTransitiveChanges())+"</div><h2>Findings</h2><table><thead><tr><th>Status</th><th>Subject</th><th>Current</th><th>Target</th><th>Action</th><th>Evidence</th></tr></thead><tbody>"+rows+"</tbody></table></body></html>";
  }

  private static String card(String label,int value) { return "<div class=\"card\"><div>"+h(label)+"</div><div class=\"value\">"+value+"</div></div>"; }
  private static String evidenceHtml(Finding finding) {
    if(finding.evidence().isEmpty()) return "None";
    return finding.evidence().stream().map(evidence -> { String reference=evidence.reference(); if(reference!=null && reference.startsWith("https://")) return "<a rel=\"noopener noreferrer\" href=\""+h(reference)+"\">"+h(evidence.ruleId())+"</a>"; return h(evidence.ruleId()+" — "+reference); }).collect(Collectors.joining("<br>"));
  }
  private static String joinBom(ImpactReport report) { return report.targetBomVersion==null?null:report.targetBomCoordinates+":"+report.targetBomVersion; }
  private static String nullableJson(String value) { return value==null?"null":"\""+j(value)+"\""; }
  private static String j(String value) {
    if(value==null) return ""; StringBuilder out=new StringBuilder();
    for(char c:value.toCharArray()) switch(c) { case '\\' -> out.append("\\\\"); case '"' -> out.append("\\\""); case '\b' -> out.append("\\b"); case '\f' -> out.append("\\f"); case '\n' -> out.append("\\n"); case '\r' -> out.append("\\r"); case '\t' -> out.append("\\t"); default -> { if(c<0x20) out.append(String.format("\\u%04x",(int)c)); else out.append(c); } }
    return out.toString();
  }
  private static String h(String value) { return value==null?"":value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;"); }
}
