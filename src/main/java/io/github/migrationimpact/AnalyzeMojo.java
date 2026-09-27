package io.github.migrationimpact;

import io.github.migrationimpact.model.ImpactReport;
import io.github.migrationimpact.platform.*;
import io.github.migrationimpact.bom.*;
import io.github.migrationimpact.graph.DependencyPathFinder;
import io.github.migrationimpact.report.ReportWriter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectDependenciesResolver;
import java.nio.file.Path;
import java.util.List;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

/** Produces a read-only pre-migration impact report. */
@Mojo(name="analyze", requiresDependencyResolution=ResolutionScope.TEST, threadSafe=true)
public final class AnalyzeMojo extends AbstractMojo {
  @Parameter(property="source", required=true) private String source;
  @Parameter(property="target", required=true) private String target;
  @Parameter(defaultValue="${project}", readonly=true, required=true) private MavenProject project;
  @Parameter(defaultValue="${project.build.directory}/migration-impact") private String outputDirectory;
  @Parameter(property="reportFormat", defaultValue="all") private String reportFormat;
  /** Optional directory containing a compatible spring-boot-targets.properties knowledge package. */
  @Parameter(property="knowledgeDirectory") private String knowledgeDirectory;
  @Component private RepositorySystem repositorySystem;
  @Component private ProjectDependenciesResolver projectDependenciesResolver;
  @Parameter(defaultValue="${repositorySystemSession}", readonly=true) private RepositorySystemSession repositorySystemSession;
  @Parameter(defaultValue="${project.remoteProjectRepositories}", readonly=true) private List<RemoteRepository> remoteRepositories;
  public void execute() throws MojoExecutionException, MojoFailureException {
    if(!reportFormat.matches("console|json|html|all")) throw new MojoFailureException("reportFormat must be console, json, html, or all");
    MigrationPlatform platform=new SpringBootPlatform(knowledgeDirectory == null || knowledgeDirectory.isBlank() ? null : Path.of(knowledgeDirectory)); if(!platform.supports(source,target)) throw new MojoFailureException("The source or target Spring Boot line is absent from the selected knowledge package. Add verified knowledge rather than guessing compatibility.");
    ImpactReport report=new ImpactReport(source,target,project.getProperties().getProperty("java.version","not declared"));
    TargetBom targetBom=null;
    try { targetBom=new TargetBomResolver(repositorySystem,repositorySystemSession,remoteRepositories).resolveSpringBoot(target); report.targetBomCoordinates=targetBom.coordinates(); report.targetBomVersion=targetBom.resolvedVersion(); }
    catch(Exception e) { getLog().warn("Target BOM metadata was unavailable: "+e.getMessage()); }
    java.util.Map<String,String> paths=java.util.Map.of();
    try { paths=new DependencyPathFinder(projectDependenciesResolver,repositorySystemSession).paths(project); }
    catch(Exception e) { getLog().warn("Dependency paths were unavailable: "+e.getMessage()); }
    platform.analyze(project,project.getBasedir().toPath(),report,targetBom,paths);
    if(reportFormat.equals("console")||reportFormat.equals("all")) getLog().info(ReportWriter.console(report));
    try { if(!reportFormat.equals("console")) ReportWriter.write(report,Path.of(outputDirectory),reportFormat); } catch(Exception e) { throw new MojoExecutionException("Could not write migration report",e); }
  }
}
