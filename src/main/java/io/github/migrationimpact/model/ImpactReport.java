package io.github.migrationimpact.model;

import java.util.*;

public final class ImpactReport {
  public final String source, target, javaVersion;
  public final List<Finding> findings = new ArrayList<>();
  public final List<String> migrationOrder = new ArrayList<>();
  public ImpactReport(String source, String target, String javaVersion) { this.source=source; this.target=target; this.javaVersion=javaVersion; }
  public Risk risk() { return findings.stream().map(Finding::risk).max(Comparator.naturalOrder()).orElse(Risk.LOW); }
  public enum Risk { LOW, MEDIUM, HIGH, CRITICAL }
  public enum Status { SUPPORTED, COMPATIBLE, UPGRADE_RECOMMENDED, UPGRADE_REQUIRED, INCOMPATIBLE, UNKNOWN }
  public enum Confidence { HIGH, MEDIUM, LOW, UNKNOWN }
  public record Evidence(String type, String ruleId, String reference, String description) {}
  public record Finding(String category, String subject, String current, String target, Status status,
                        Risk risk, Confidence confidence, String recommendation, String reason,
                        List<Evidence> evidence) {}
}
