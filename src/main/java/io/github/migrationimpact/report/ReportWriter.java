package io.github.migrationimpact.report;

import io.github.migrationimpact.model.ImpactReport;
import io.github.migrationimpact.model.ImpactReport.Finding;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.stream.Collectors;

public final class ReportWriter {
  private ReportWriter() {}
  public static String console(ImpactReport r) {
    StringBuilder b=new StringBuilder("\n=============================================\nMIGRATION IMPACT ANALYZER\n"+r.source+" → "+r.target+"\n=============================================\nOverall Risk: "+r.risk()+"\n");
    for(Finding f:r.findings) b.append("\n[").append(f.status()).append("] ").append(f.subject()).append(" — ").append(f.recommendation()).append("\n  Evidence: ").append(f.evidence().get(0).reference()).append('\n');
    b.append("\nMIGRATION ORDER\n---------------------------------------------\n"); for(int i=0;i<r.migrationOrder.size();i++)b.append(i+1).append(". ").append(r.migrationOrder.get(i)).append('\n'); return b.toString();
  }
  public static void write(ImpactReport r, Path output, String format) throws IOException {
    Files.createDirectories(output); if (format.equals("json")||format.equals("all")) Files.writeString(output.resolve("report.json"),json(r),StandardCharsets.UTF_8); if(format.equals("html")||format.equals("all")) Files.writeString(output.resolve("report.html"),html(r),StandardCharsets.UTF_8);
  }
  static String json(ImpactReport r) { return "{\n  \"source\":\""+q(r.source)+"\",\n  \"target\":\""+q(r.target)+"\",\n  \"overallRisk\":\""+r.risk()+"\",\n  \"findings\":[\n"+r.findings.stream().map(ReportWriter::jsonFinding).collect(Collectors.joining(",\n"))+"\n  ],\n  \"migrationOrder\":["+r.migrationOrder.stream().map(x->"\""+q(x)+"\"").collect(Collectors.joining(","))+ "]\n}\n"; }
  private static String jsonFinding(Finding f) { return "    {\"category\":\""+q(f.category())+"\",\"subject\":\""+q(f.subject())+"\",\"current\":\""+q(f.current())+"\",\"target\":\""+q(f.target())+"\",\"status\":\""+f.status()+"\",\"risk\":\""+f.risk()+"\",\"confidence\":\""+f.confidence()+"\",\"recommendation\":\""+q(f.recommendation())+"\",\"reason\":\""+q(f.reason())+"\",\"evidence\":["+f.evidence().stream().map(e->"{\"type\":\""+q(e.type())+"\",\"ruleId\":\""+q(e.ruleId())+"\",\"reference\":\""+q(e.reference())+"\"}").collect(Collectors.joining(","))+"]}"; }
  static String html(ImpactReport r) { String rows=r.findings.stream().map(f->"<tr><td>"+e(f.category())+"</td><td>"+e(f.subject())+"</td><td>"+f.status()+"</td><td>"+f.risk()+"</td><td>"+e(f.recommendation())+"</td><td><a href=\""+e(f.evidence().get(0).reference())+"\">"+e(f.evidence().get(0).ruleId())+"</a></td></tr>").collect(Collectors.joining()); return "<!doctype html><html><head><meta charset=\"utf-8\"><title>Migration Impact Report</title><style>body{font-family:system-ui;margin:2rem}table{border-collapse:collapse;width:100%}td,th{border:1px solid #ddd;padding:.6rem;text-align:left}th{background:#f3f4f6}</style></head><body><h1>Migration Impact Analyzer</h1><p>"+e(r.source)+" → "+e(r.target)+" · Overall risk: <strong>"+r.risk()+"</strong></p><table><tr><th>Category</th><th>Subject</th><th>Status</th><th>Risk</th><th>Action</th><th>Evidence</th></tr>"+rows+"</table><h2>Migration order</h2><ol>"+r.migrationOrder.stream().map(x->"<li>"+e(x)+"</li>").collect(Collectors.joining())+"</ol></body></html>"; }
  private static String q(String x){return x==null?"":x.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n");} private static String e(String x){return q(x).replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");}
}
