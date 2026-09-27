package io.github.migrationimpact.model;

import java.util.*;

public final class ImpactReport {
  public final String source, target, javaVersion;
  public final List<Finding> findings = new ArrayList<>();
  public final List<DependencyTarget> dependencyTargets = new ArrayList<>();
  public record DependencyTarget(String artifact,String current,String target,String basis) {}
  public final List<String> migrationOrder = new ArrayList<>();
  public final Set<String> affectedSourceFiles = new TreeSet<>();
  public String knowledgeVersion = "unknown";
  public String knowledgeSource = "classpath:/knowledge/spring-boot-targets.properties";
  public String targetBomCoordinates;
  public String targetBomVersion;
  public int managedDirectChanges;
  public int managedTransitiveChanges;
  public ImpactReport(String source, String target, String javaVersion) { this.source=source; this.target=target; this.javaVersion=javaVersion; }
  public List<Finding> uniqueFindings() {
    Map<String,Finding> unique=new LinkedHashMap<>();
    for(Finding finding:findings) unique.putIfAbsent(finding.category()+"\u0000"+finding.subject()+"\u0000"+finding.target()+"\u0000"+finding.status(),finding);
    return List.copyOf(unique.values());
  }
  public Risk risk() { return uniqueFindings().stream().map(Finding::risk).max(Comparator.naturalOrder()).orElse(Risk.LOW); }
  public Summary summary() {
    Set<String> requiredLibraries=new TreeSet<>(), recommendedLibraries=new TreeSet<>(), reviewLibraries=new TreeSet<>();
    for(Finding finding:uniqueFindings()) if(isLibraryFinding(finding)) {
      if(finding.status()==Status.UPGRADE_REQUIRED || finding.status()==Status.INCOMPATIBLE) requiredLibraries.add(finding.subject());
      else if(finding.status()==Status.UPGRADE_RECOMMENDED) recommendedLibraries.add(finding.subject());
      else if(finding.status()==Status.UNKNOWN) reviewLibraries.add(finding.subject());
    }
    return new Summary(affectedSourceFiles.size(),requiredLibraries.size(),recommendedLibraries.size(),reviewLibraries.size(),managedDirectChanges,managedTransitiveChanges,uniqueFindings().size());
  }
  private static boolean isLibraryFinding(Finding finding) {
    return Set.of("DEPENDENCY","LIBRARY_COMPATIBILITY","TRANSITIVE_DEPENDENCY","DEPENDENCY_MANAGEMENT","UNMANAGED_DEPENDENCY").contains(finding.category());
  }
  public enum Risk { LOW, MEDIUM, HIGH, CRITICAL }
  public enum Status { SUPPORTED, COMPATIBLE, UPGRADE_RECOMMENDED, UPGRADE_REQUIRED, INCOMPATIBLE, UNKNOWN }
  public enum Confidence { HIGH, MEDIUM, LOW, UNKNOWN }
  public record Evidence(String type, String ruleId, String reference, String description) {}
  public record Finding(String category, String subject, String current, String target, Status status,
                        Risk risk, Confidence confidence, String recommendation, String reason,
                        List<Evidence> evidence) {}
  public record Summary(int affectedSourceFiles, int requiredLibraryUpdates, int recommendedLibraryUpdates,
                        int librariesToReview, int managedDirectChanges, int managedTransitiveChanges,
                        int totalFindings) {}
}
