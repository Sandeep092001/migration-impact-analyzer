package io.github.migrationimpact.platform;

import io.github.migrationimpact.model.ImpactReport;
import io.github.migrationimpact.bom.TargetBom;
import org.apache.maven.project.MavenProject;
import java.nio.file.Path;
import java.util.Map;

/** Extension point for framework-specific analysis. */
public interface MigrationPlatform {
  boolean supports(String source, String target);
  void analyze(MavenProject project, Path baseDirectory, ImpactReport report, TargetBom targetBom, Map<String,String> dependencyPaths);
}
