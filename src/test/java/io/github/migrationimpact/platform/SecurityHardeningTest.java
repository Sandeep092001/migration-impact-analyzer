package io.github.migrationimpact.platform;

import io.github.migrationimpact.model.ImpactReport;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class SecurityHardeningTest {
  @TempDir Path directory;

  @Test void doesNotFollowSourceSymlinksOrReadOversizedFiles() throws Exception {
    Path source=Files.createDirectories(directory.resolve("src/main/java"));
    Path outside=directory.resolve("outside.java");
    Files.writeString(outside,"import javax.persistence.Entity;\n");
    try {
      Files.createSymbolicLink(source.resolve("Linked.java"),outside);
    } catch(UnsupportedOperationException | FileSystemException unavailable) {
      assumeTrue(false,"Symbolic links unavailable: "+unavailable.getClass().getSimpleName());
    }
    Files.writeString(source.resolve("Oversized.java"),"import javax.persistence.Entity;\n"+" ".repeat(2*1024*1024));

    ImpactReport report=analyze(new SpringBootPlatform(),directory);
    assertTrue(report.affectedSourceFiles.isEmpty());
    assertTrue(report.findings.stream().anyMatch(f->f.subject().equals("Source scan coverage") && f.status()==ImpactReport.Status.UNKNOWN));
  }

  @Test void externalKnowledgeReportDoesNotExposeAbsoluteLocalPath() throws Exception {
    Path knowledge=Files.createDirectories(directory.resolve("private-user-directory"));
    copyResource("/knowledge/spring-boot-targets.properties",knowledge.resolve("spring-boot-targets.properties"));
    copyResource("/knowledge/library-rules.properties",knowledge.resolve("library-rules.properties"));

    ImpactReport report=analyze(new SpringBootPlatform(knowledge),directory);
    assertEquals("external-directory",report.knowledgeSource);
    assertFalse(report.knowledgeSource.contains(directory.toString()));
  }

  private static ImpactReport analyze(SpringBootPlatform platform,Path base) {
    Model model=new Model();
    model.setModelVersion("4.0.0");
    model.getProperties().setProperty("java.version","17");
    MavenProject project=new MavenProject(model);
    project.setOriginalModel(model);
    project.setArtifacts(Collections.emptySet());
    ImpactReport report=new ImpactReport("spring-boot:3.5","spring-boot:4.0","17");
    platform.analyze(project,base,report,null,Map.of());
    return report;
  }

  private static void copyResource(String resource,Path destination) throws Exception {
    try(InputStream input=SecurityHardeningTest.class.getResourceAsStream(resource)) {
      assertNotNull(input);
      Files.copy(input,destination);
    }
  }
}
