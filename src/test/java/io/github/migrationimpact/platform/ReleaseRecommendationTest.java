package io.github.migrationimpact.platform;

import io.github.migrationimpact.bom.LibraryVersionResolver;
import io.github.migrationimpact.bom.TargetBom;
import io.github.migrationimpact.model.ImpactReport;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.model.*;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ReleaseRecommendationTest {
  @TempDir Path directory;
  private ImpactReport analyze(String version,SpringBootPlatform.ReleaseLookup lookup) {
    return analyze(version,lookup,null);
  }
  private ImpactReport analyze(String version,SpringBootPlatform.ReleaseLookup lookup,TargetBom bom) {
    Model model=new Model(); model.setModelVersion("4.0.0");
    model.getProperties().setProperty("java.version","17");
    Dependency dependency=new Dependency(); dependency.setGroupId("org.jsoup"); dependency.setArtifactId("jsoup"); dependency.setVersion(version);
    model.addDependency(dependency);
    MavenProject project=new MavenProject(model); project.setOriginalModel(model);
    project.setArtifacts(new LinkedHashSet<>(List.of(new DefaultArtifact("org.jsoup","jsoup",version,"compile","jar",null,new DefaultArtifactHandler("jar")))));
    ImpactReport report=new ImpactReport("spring-boot:3.5","spring-boot:4.0","17");
    new SpringBootPlatform(null,lookup).analyze(project,directory,report,bom,Map.of());
    return report;
  }
  @Test void returnsExactReleaseAndRetainsEvidence() {
    var report=analyze("1.21.1",(ga,range) -> { assertEquals("org.jsoup:jsoup",ga); assertEquals("[1.23,1.24)",range); return "1.23.2"; });
    var finding=report.findings.get(0);
    assertEquals("org.jsoup:jsoup:1.23.2",finding.target());
    assertEquals(ImpactReport.Status.UPGRADE_RECOMMENDED,finding.status());
    assertEquals(2,finding.evidence().size());
    assertEquals(1,report.dependencyTargets.size());
  }
  @Test void unavailableMetadataDoesNotPretendCompatibility() {
    var report=analyze("1.21.1",(ga,range) -> {throw new Exception("offline");});
    assertEquals(ImpactReport.Status.UNKNOWN,report.findings.get(0).status());
    assertTrue(report.findings.get(0).target().contains("unresolved"));
  }
  @Test void neverRecommendsDowngradeFromNewerUnreviewedVersion() {
    var report=analyze("1.24.0",(ga,range) -> "1.23.2");
    assertEquals(ImpactReport.Status.UNKNOWN,report.findings.get(0).status());
    assertTrue(report.findings.get(0).target().startsWith("Keep current"));
  }
  @Test void excludesUnstableAndUnrecognizedReleaseQualifiers() {
    for(String v:List.of("1.0-SNAPSHOT","1.0-RC1","1.0-beta","1.0-M1","latest","v1.0")) assertFalse(LibraryVersionResolver.isStable(v),v);
    for(String v:List.of("1.2.3","2.0.37","5.6.15.Final","2.0.RELEASE")) assertTrue(LibraryVersionResolver.isStable(v),v);
  }
  @Test void platformBomTakesPrecedenceOverGeneralLibraryRule() {
    var bom=new TargetBom("org.springframework.boot:spring-boot-dependencies","4.0.8",Map.of("org.jsoup:jsoup","1.21.2"));
    var report=analyze("1.21.1",(ga,range) -> {fail("BOM-managed library must not query general release rule");return null;},bom);
    assertEquals("1.21.2",report.dependencyTargets.get(0).target());
    assertEquals("TARGET_BOM",report.dependencyTargets.get(0).basis());
    assertEquals(1,report.findings.size());
  }
}
