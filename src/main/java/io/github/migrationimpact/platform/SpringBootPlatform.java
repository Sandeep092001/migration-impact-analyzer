package io.github.migrationimpact.platform;

import io.github.migrationimpact.model.ImpactReport;
import io.github.migrationimpact.bom.TargetBom;
import io.github.migrationimpact.model.ImpactReport.*;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;
import java.util.stream.*;
import java.util.regex.Pattern;

public final class SpringBootPlatform implements MigrationPlatform {
  private final Path knowledgeDirectory;
  public SpringBootPlatform() { this(null); }
  public SpringBootPlatform(Path knowledgeDirectory) { this.knowledgeDirectory=knowledgeDirectory; }
  private static final String BOOT_3_GUIDE = "https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.0-Migration-Guide";
  private static final String CLOUD_MATRIX = "https://github.com/spring-cloud/spring-cloud-release/wiki/Supported-Versions";
  private static Evidence docs(String id, String url, String text) { return new Evidence("OFFICIAL_DOCUMENTATION", id, url, text); }
  @Override public boolean supports(String source, String target) { return knowledge(source)!=null && knowledge(target)!=null; }
  @Override public void analyze(MavenProject p, Path base, ImpactReport r, TargetBom targetBom, Map<String,String> dependencyPaths) {
    TargetKnowledge knowledge=knowledge(r.target);
    r.findings.add(new Finding("KNOWLEDGE","Knowledge package",knowledge.version(),knowledge.line(),Status.SUPPORTED,Risk.LOW,Confidence.HIGH,"Rules are bundled with this plugin; record this version with the report.","The analysis used a versioned knowledge package.",List.of(new Evidence("PROJECT_RULE","KNOWLEDGE_VERSION","classpath:/knowledge/spring-boot-targets.properties","Bundled migration knowledge"))));
    javaPrerequisite(p, r, knowledge); cloud(p, r, knowledge); dependencies(p, r, knowledge, targetBom, dependencyPaths); build(p, r, knowledge); sourceImports(base, r); order(r);
  }
  private TargetKnowledge knowledge(String version) { return TargetKnowledge.load(knowledgeDirectory, version); }
  private void javaPrerequisite(MavenProject p, ImpactReport r, TargetKnowledge knowledge) {
    String java = first(p.getProperties(), "maven.compiler.release", "java.version", "maven.compiler.source");
    if (java == null) java = "not declared";
    int major = parseJava(java);
    int required=Integer.parseInt(knowledge.javaMinimum());
    if (major < required) add(r,"PREREQUISITE","Java",java,required+"+",Status.UPGRADE_REQUIRED,Risk.HIGH,Confidence.HIGH,"Upgrade the build JDK to Java "+required+" or newer before upgrading Spring Boot.","Spring Boot "+knowledge.line()+" requires Java "+required+".",docs("BOOT_"+knowledge.line()+"_JAVA",knowledge.bootEvidence(),"Spring Boot system requirements"));
  }
  private void cloud(MavenProject p, ImpactReport r, TargetKnowledge knowledge) {
    Artifact cloud = artifacts(p).filter(a -> a.getGroupId().startsWith("org.springframework.cloud")).findFirst().orElse(null);
    if (cloud == null) return;
    if (knowledge.cloudTrain() == null) {
      add(r,"DEPENDENCY","Spring Cloud",cloud.getVersion(),"unknown",Status.UNKNOWN,Risk.MEDIUM,Confidence.UNKNOWN,"No verified Spring Cloud release train is bundled for this target yet; consult the official matrix.","The knowledge package has no evidence-backed compatibility mapping for this target.",docs("SPRING_CLOUD_BOOT_COMPATIBILITY",CLOUD_MATRIX,"Official Spring Cloud supported versions matrix"));
      return;
    }
    String currentTrain=p.getProperties().getProperty("spring-cloud.version", cloud.getVersion());
    boolean sameTrain=currentTrain.startsWith(knowledge.cloudTrain().substring(0, 6));
    add(r,"DEPENDENCY","Spring Cloud",currentTrain,knowledge.cloudTrain(),sameTrain ? Status.COMPATIBLE : Status.UPGRADE_REQUIRED,sameTrain ? Risk.LOW : Risk.HIGH,Confidence.HIGH,sameTrain ? "Keep the current release train and select its latest compatible patch." : "Align Spring Cloud with the "+knowledge.cloudTrain()+" release train for Spring Boot "+knowledge.line()+"; validate the exact patch against the official matrix.",sameTrain ? "The declared Spring Cloud release train matches the target Boot line." : "The detected Cloud dependency belongs to a pre-target release line.",docs("SPRING_CLOUD_BOOT_COMPATIBILITY",knowledge.cloudEvidence(),"Official Spring Cloud supported versions matrix"));
  }
  private void dependencies(MavenProject p, ImpactReport r, TargetKnowledge knowledge, TargetBom targetBom, Map<String,String> dependencyPaths) {
    Set<String> direct = p.getDependencies().stream().map(d -> d.getGroupId()+":"+d.getArtifactId()).collect(Collectors.toSet());
    List<LibraryKnowledge.Rule> libraryRules=LibraryKnowledge.load(knowledgeDirectory);
    Set<String> seenRules=new HashSet<>(), ruleMatched=new HashSet<>();
    for (Artifact a:p.getArtifacts()) {
      String ga=a.getGroupId()+":"+a.getArtifactId(); boolean isDirect=direct.contains(ga);
      String targetManaged=targetBom == null ? null : targetBom.managedVersion(a.getGroupId(),a.getArtifactId());
      if (isDirect && targetManaged != null && !targetManaged.equals(a.getVersion())) add(r,"TARGET_BOM",ga,a.getVersion(),targetManaged,Status.UPGRADE_RECOMMENDED,Risk.MEDIUM,Confidence.HIGH,"Adopt the target BOM-managed version unless the project has a documented compatibility override.","The exact target Spring Boot BOM manages a different version.",new Evidence("BOM_METADATA","TARGET_BOM",targetBom.coordinates()+":"+targetBom.resolvedVersion(),"Resolved through Maven repository metadata"));
      if (!isDirect && targetManaged != null && !targetManaged.equals(a.getVersion())) add(r,"TARGET_BOM_TRANSITIVE",ga,a.getVersion(),targetManaged,Status.UPGRADE_RECOMMENDED,Risk.MEDIUM,Confidence.HIGH,"Review the owning direct dependency before adopting the target BOM-managed version.","The exact target BOM manages a different transitive version. Path: "+dependencyPaths.getOrDefault(ga,"unavailable"),new Evidence("BOM_METADATA","TARGET_BOM",targetBom.coordinates()+":"+targetBom.resolvedVersion(),"Resolved through Maven repository metadata"));
      libraryRules.stream().filter(rule -> rule.matches(a.getGroupId(),a.getArtifactId(),a.getVersion(),knowledge.line())).findFirst().ifPresent(rule -> {
        ruleMatched.add(ga);
        if(seenRules.add(rule.id())) add(r,"LIBRARY_COMPATIBILITY",ga,a.getVersion(),rule.targetArtifact()+":"+rule.targetVersion(),rule.status(),Risk.HIGH,Confidence.HIGH,"Adopt the documented target artifact/version line and validate its latest stable patch.",rule.reason()+" Path: "+dependencyPaths.getOrDefault(ga,isDirect?ga+":"+a.getVersion():"unavailable"),docs(rule.id(),rule.evidence(),"Official library compatibility documentation"));
      });
      if (a.getGroupId().equals("org.springframework") && a.getArtifactId().startsWith("spring-") && a.getVersion().startsWith("5.")) add(r,"TRANSITIVE_DEPENDENCY",ga,a.getVersion(),"Spring Framework "+knowledge.framework(),Status.INCOMPATIBLE,Risk.HIGH,Confidence.MEDIUM,"Identify the owning dependency and upgrade or exclude it.","A resolved Spring Framework 5 artifact conflicts with the framework generation managed by Boot "+knowledge.line()+".",docs("BOOT_"+knowledge.line()+"_FRAMEWORK",knowledge.bootEvidence(),"Spring Boot system requirements"));
      if (a.getGroupId().equals("org.hibernate.orm") || a.getGroupId().equals("org.hibernate")) add(r,"DEPENDENCY",ga,a.getVersion(),"Hibernate "+knowledge.hibernateMajor(),Status.UPGRADE_RECOMMENDED,Risk.HIGH,Confidence.MEDIUM,"Review Hibernate "+knowledge.hibernateMajor()+" migration notes and application mappings.","The target Spring Boot line manages the Hibernate "+knowledge.hibernateMajor()+" generation; this may require application changes.",docs("BOOT_"+knowledge.line()+"_HIBERNATE",knowledge.bootEvidence(),"Spring Boot managed dependency documentation"));
      if (isDirect && managedOverride(p,a)) add(r,"DEPENDENCY_MANAGEMENT",ga,a.getVersion(),"target BOM managed version",Status.UPGRADE_RECOMMENDED,Risk.MEDIUM,Confidence.MEDIUM,"Review this explicit version override against the target Spring Boot BOM.","A direct dependency version overrides dependency management.",new Evidence("USER_CONFIGURATION","EXPLICIT_MANAGED_OVERRIDE","pom.xml","Maven project dependency model"));
      if(isDirect && targetManaged==null && !ruleMatched.contains(ga) && !a.getGroupId().startsWith("org.springframework")) add(r,"UNMANAGED_DEPENDENCY",ga,a.getVersion(),"not managed by target BOM",Status.UNKNOWN,Risk.MEDIUM,Confidence.UNKNOWN,"Check the library's official compatibility documentation for Spring Boot "+knowledge.line()+".","This direct third-party dependency is neither managed by the target Boot BOM nor covered by a verified bundled rule.",new Evidence("DEPENDENCY_GRAPH","UNMANAGED_DEPENDENCY",dependencyPaths.getOrDefault(ga,ga+":"+a.getVersion()),"Resolved Maven dependency graph"));
    }
  }
  private void build(MavenProject p, ImpactReport r, TargetKnowledge knowledge) {
    for (Plugin plugin:p.getBuildPlugins()) if (plugin.getArtifactId().equals("maven-compiler-plugin")) {
      add(r,"MAVEN_BUILD","maven-compiler-plugin",plugin.getVersion(),"Java "+knowledge.javaMinimum()+" compatible configuration",Status.UPGRADE_RECOMMENDED,Risk.MEDIUM,Confidence.MEDIUM,"Set release/source/target to "+knowledge.javaMinimum()+"+ and run the test suite.","Compiler settings must support the target JDK.",docs("BOOT_"+knowledge.line()+"_JAVA",knowledge.bootEvidence(),"Spring Boot system requirements"));
    }
  }
  private void sourceImports(Path base, ImpactReport r) {
    List<String> hits=new ArrayList<>();
    Pattern affectedImport=Pattern.compile("(?m)^\\s*import\\s+javax\\.(?:persistence|validation|servlet|annotation|transaction)(?:\\.|;)");
    try (Stream<Path> paths=Files.walk(base.resolve("src"))) { paths.filter(x->x.toString().endsWith(".java")).forEach(x->{ try { if(affectedImport.matcher(Files.readString(x)).find()) hits.add(base.relativize(x).toString()); } catch(IOException ignored){} }); } catch(IOException ignored) {}
    if(!hits.isEmpty()) add(r,"SOURCE_CODE","Jakarta namespace imports",hits.size()+" source file(s)","jakarta.* equivalents",Status.UPGRADE_REQUIRED,Risk.HIGH,Confidence.HIGH,"Replace only the listed affected Java EE imports; review each API's migration guidance.","Boot 3 uses Jakarta EE namespaces for these APIs. Files: "+String.join(", ",hits),docs("BOOT3_JAKARTA",BOOT_3_GUIDE,"Spring Boot 3 migration guide"));
  }
  private void order(ImpactReport r) { r.migrationOrder.addAll(r.findings.stream().filter(f->f.category().equals("PREREQUISITE")||f.status()==Status.UPGRADE_REQUIRED).map(f->"REQUIRED_BEFORE_MIGRATION: "+f.recommendation()).distinct().toList()); r.migrationOrder.add("REVIEW_DURING_MIGRATION: Upgrade Spring Boot and validate dependency convergence."); r.migrationOrder.add("POST_MIGRATION_VALIDATION: Run the complete test suite and review runtime configuration."); }
  private static Stream<Artifact> artifacts(MavenProject p) { return p.getArtifacts()==null?Stream.empty():p.getArtifacts().stream(); }
  private static boolean managedOverride(MavenProject p,Artifact a) { return p.getOriginalModel().getDependencies().stream().anyMatch(d->d.getGroupId().equals(a.getGroupId())&&d.getArtifactId().equals(a.getArtifactId())&&d.getVersion()!=null); }
  private static String first(Properties p,String... keys) { for(String k:keys)if(p.getProperty(k)!=null)return p.getProperty(k);return null; }
  private static int parseJava(String v) { try { return Integer.parseInt(v.replace("1.","").replaceAll("[^0-9].*","")); } catch(Exception e){return 0;} }
  private static void add(ImpactReport r,String c,String s,String cur,String target,Status st,Risk risk,Confidence conf,String rec,String why,Evidence e) { r.findings.add(new Finding(c,s,cur,target,st,risk,conf,rec,why,List.of(e))); }
}
