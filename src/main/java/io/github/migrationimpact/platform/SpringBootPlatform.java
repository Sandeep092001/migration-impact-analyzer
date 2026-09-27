package io.github.migrationimpact.platform;

import io.github.migrationimpact.model.ImpactReport;
import io.github.migrationimpact.bom.TargetBom;
import io.github.migrationimpact.model.ImpactReport.*;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;
import java.util.stream.*;
import java.util.regex.Pattern;

public final class SpringBootPlatform implements MigrationPlatform {
  @FunctionalInterface public interface ReleaseLookup { String resolve(String coordinates,String range) throws Exception; }
  private final ReleaseLookup releases;
  private final Path knowledgeDirectory;
  public SpringBootPlatform() { this(null); }
  public SpringBootPlatform(Path knowledgeDirectory) { this(knowledgeDirectory,null); }
  public SpringBootPlatform(Path knowledgeDirectory,ReleaseLookup releases) { this.knowledgeDirectory=knowledgeDirectory; this.releases=releases; }
  private static final String BOOT_3_GUIDE = "https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.0-Migration-Guide";
  private static final String CLOUD_MATRIX = "https://github.com/spring-cloud/spring-cloud-release/wiki/Supported-Versions";
  private static Evidence docs(String id, String url, String text) { return new Evidence("OFFICIAL_DOCUMENTATION", id, url, text); }
  @Override public boolean supports(String source, String target) { return knowledge(source)!=null && knowledge(target)!=null; }
  @Override public void analyze(MavenProject p, Path base, ImpactReport r, TargetBom targetBom, Map<String,String> dependencyPaths) {
    TargetKnowledge knowledge=knowledge(r.target);
    r.knowledgeVersion=knowledge.version();
    if(knowledgeDirectory!=null) r.knowledgeSource=knowledgeDirectory.toAbsolutePath().normalize().toString();
    javaPrerequisite(p, r, knowledge); cloud(p, r, knowledge); dependencies(p, r, knowledge, targetBom, dependencyPaths); sourceImports(base, r); order(r);
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
    List<Artifact> resolved=artifacts(p).sorted(Comparator.comparing(a -> a.getGroupId()+":"+a.getArtifactId())).toList();
    Set<String> ruleMatched=new HashSet<>();
    for(LibraryKnowledge.Rule rule:libraryRules) {
      List<Artifact> matches=resolved.stream().filter(a -> direct.contains(a.getGroupId()+":"+a.getArtifactId()) && rule.matches(a.getGroupId(),a.getArtifactId(),a.getVersion(),knowledge.line()))
          .filter(a -> rule.status()!=Status.COMPATIBLE || targetBom==null || targetBom.managedVersion(a.getGroupId(),a.getArtifactId())==null).toList();
      if(matches.isEmpty()) continue;
      matches.forEach(a -> ruleMatched.add(a.getGroupId()+":"+a.getArtifactId()));
      String subject=rule.displayName()==null?matches.get(0).getGroupId()+":"+matches.get(0).getArtifactId():rule.displayName();
      String versions=matches.stream().map(Artifact::getVersion).distinct().collect(Collectors.joining(", "));
      String current=matches.size()==1?versions:versions+" ("+matches.size()+" modules)";
      String targetDisplay=rule.targetDisplay()==null?rule.targetArtifact()+":"+rule.targetVersion():rule.targetDisplay();
      String recommendation=rule.recommendation()==null?"Adopt the documented target artifact/version line and validate its latest stable patch.":rule.recommendation();
      boolean misaligned=rule.alignVersions() && matches.stream().map(Artifact::getVersion).distinct().count()>1;
      List<String> wrongScopes=matches.stream().filter(a -> rule.runtimeArtifacts().contains(a.getArtifactId()) && !"runtime".equals(a.getScope())).map(a -> a.getArtifactId()+"="+a.getScope()).toList();
      Status status=misaligned?Status.UPGRADE_REQUIRED:!wrongScopes.isEmpty()?Status.UPGRADE_RECOMMENDED:rule.status();
      if(misaligned) recommendation="Align every "+subject+" artifact to one version. "+recommendation;
      if(!wrongScopes.isEmpty()) recommendation="Use runtime scope for "+String.join(", ",wrongScopes)+". "+recommendation;
      String javaEvidence=rule.javaMinimum()>0?" The library baseline is Java "+rule.javaMinimum()+"+; Spring Boot "+knowledge.line()+" requires Java "+knowledge.javaMinimum()+"+.":"";
      List<Evidence> evidence=new ArrayList<>(); for(int i=0;i<rule.evidence().size();i++) evidence.add(docs(rule.id()+(i==0?"":"_"+(i+1)),rule.evidence().get(i),"Official library compatibility documentation"));
      String selectedVersion=null;
      if(rule.releaseRange()!=null) {
        try {
          if(releases==null) throw new IllegalStateException("Release lookup unavailable");
          String exact=releases.resolve(rule.releaseArtifact(),rule.releaseRange());
          if(rule.alignVersions()) for(Artifact matched:matches) {
            if(rule.releaseArtifact().equals(matched.getGroupId()+":"+matched.getArtifactId())) continue;
            if(!exact.equals(releases.resolve(matched.getGroupId()+":"+matched.getArtifactId(),"["+exact+"]"))) throw new IllegalStateException("Family release unavailable");
          }
          selectedVersion=exact;
          targetDisplay=rule.releaseArtifact()+":"+exact;
          evidence.add(new Evidence("MAVEN_METADATA",rule.id()+"_RELEASE",targetDisplay,"Latest stable release inside reviewed range "+rule.releaseRange()));
          if(matches.stream().anyMatch(a -> new org.apache.maven.artifact.versioning.ComparableVersion(a.getVersion()).compareTo(new org.apache.maven.artifact.versioning.ComparableVersion(exact))>0)) {
            selectedVersion=null;
            targetDisplay="Keep current version pending review; reviewed candidate "+targetDisplay;
            status=Status.UNKNOWN;
          }
          if(status==Status.COMPATIBLE && matches.stream().anyMatch(a -> new org.apache.maven.artifact.versioning.ComparableVersion(a.getVersion()).compareTo(new org.apache.maven.artifact.versioning.ComparableVersion(exact))<0)) status=Status.UPGRADE_RECOMMENDED;
          recommendation="Reviewed release candidate: "+targetDisplay+". "+recommendation;
        } catch(Exception unavailable) {
          selectedVersion=null;
          targetDisplay="Exact release unresolved; reviewed range "+rule.releaseRange();
          if(status==Status.COMPATIBLE) status=Status.UNKNOWN;
          recommendation="Release metadata was unavailable or contained no stable version in the reviewed range. Retry with repository access. "+recommendation;
        }
      }
      if(rule.javaMinimum()>Integer.parseInt(knowledge.javaMinimum())) {
        status=Status.UNKNOWN;
        recommendation="This library needs Java "+rule.javaMinimum()+"+, above the target Boot minimum; explicitly validate the target JDK. "+recommendation;
      }
      for(Artifact matched:matches) {
        String artifact=matched.getGroupId()+":"+matched.getArtifactId();
        String mappedTarget=selectedVersion==null?targetDisplay:(rule.alignVersions()?artifact:rule.releaseArtifact())+":"+selectedVersion;
        r.dependencyTargets.add(new ImpactReport.DependencyTarget(artifact,matched.getVersion(),mappedTarget,"RULE:"+rule.id()));
      }
      add(r,"LIBRARY_COMPATIBILITY",subject,current,targetDisplay,status,rule.risk(),rule.confidence(),recommendation,rule.reason()+javaEvidence,evidence);
    }
    Set<String> oldSpringArtifacts=new TreeSet<>(), hibernateArtifacts=new TreeSet<>(), hibernateVersions=new TreeSet<>();
    for (Artifact a:resolved) {
      String ga=a.getGroupId()+":"+a.getArtifactId(); boolean isDirect=direct.contains(ga);
      String targetManaged=targetBom == null ? null : targetBom.managedVersion(a.getGroupId(),a.getArtifactId());
      if(isDirect && !ruleMatched.contains(ga)) r.dependencyTargets.add(new ImpactReport.DependencyTarget(ga,a.getVersion(),targetManaged,targetManaged==null?"UNKNOWN":"TARGET_BOM"));
      if (targetManaged != null && !targetManaged.equals(a.getVersion())) {
        if(isDirect) r.managedDirectChanges++; else r.managedTransitiveChanges++;
      }
      if (isDirect && targetManaged != null && !targetManaged.equals(a.getVersion()) && managedOverride(p,a)) add(r,"DEPENDENCY_MANAGEMENT",ga,a.getVersion(),targetManaged,Status.UPGRADE_RECOMMENDED,Risk.MEDIUM,Confidence.HIGH,"Remove or update the explicit version so the Spring Boot "+knowledge.line()+" BOM can manage this dependency.","The project explicitly overrides the version selected by the resolved target BOM.",bomEvidence(targetBom));
      if (knowledge.framework()!=null && !knowledge.framework().startsWith("5.") && a.getGroupId().equals("org.springframework") && a.getArtifactId().startsWith("spring-") && a.getVersion().startsWith("5.")) oldSpringArtifacts.add(ga+":"+a.getVersion());
      if (a.getGroupId().equals("org.hibernate.orm") || a.getGroupId().equals("org.hibernate")) { hibernateArtifacts.add(ga); hibernateVersions.add(a.getVersion()); }
      if(isDirect && targetManaged==null && !ruleMatched.contains(ga) && !a.getGroupId().startsWith("org.springframework")) add(r,"UNMANAGED_DEPENDENCY",ga,a.getVersion(),"not managed by target BOM",Status.UNKNOWN,Risk.MEDIUM,Confidence.UNKNOWN,"Check the library's official compatibility documentation for Spring Boot "+knowledge.line()+".","This direct third-party dependency is neither managed by the target Boot BOM nor covered by a verified bundled rule.",new Evidence("DEPENDENCY_GRAPH","UNMANAGED_DEPENDENCY",dependencyPaths.getOrDefault(ga,ga+":"+a.getVersion()),"Resolved Maven dependency graph"));
    }
    if(!oldSpringArtifacts.isEmpty()) add(r,"TRANSITIVE_DEPENDENCY","Spring Framework 5 transitive dependencies",oldSpringArtifacts.size()+" artifact(s)","Spring Framework "+knowledge.framework(),Status.INCOMPATIBLE,Risk.HIGH,Confidence.MEDIUM,"Upgrade or exclude the owning direct dependencies before migrating.",oldSpringArtifacts.size()+" resolved Spring Framework 5 artifacts conflict with the framework generation used by Spring Boot "+knowledge.line()+".",docs("BOOT_"+knowledge.line()+"_FRAMEWORK",knowledge.bootEvidence(),"Spring Boot system requirements"));
    if(!hibernateArtifacts.isEmpty() && knowledge.hibernateMajor()!=null && hibernateVersions.stream().anyMatch(v -> !matchesVersionLine(v,knowledge.hibernateMajor()))) {
      String managedHibernate=targetBom==null?null:targetBom.managedVersion("org.hibernate.orm","hibernate-core");
      String hibernateTarget=managedHibernate==null?"Hibernate "+knowledge.hibernateMajor():"org.hibernate.orm:hibernate-core:"+managedHibernate;
      Evidence hibernateEvidence=targetBom==null?docs("BOOT_"+knowledge.line()+"_HIBERNATE",knowledge.bootEvidence(),"Spring Boot managed dependency documentation"):bomEvidence(targetBom);
      add(r,"DEPENDENCY","Hibernate ORM",String.join(", ",hibernateVersions),hibernateTarget,Status.UPGRADE_RECOMMENDED,Risk.HIGH,Confidence.HIGH,"Review the target Hibernate migration notes and application mappings once.","The resolved target Spring Boot BOM manages a different Hibernate generation across "+hibernateArtifacts.size()+" detected Hibernate artifact(s).",hibernateEvidence);
    }
  }
  private void sourceImports(Path base, ImpactReport r) {
    List<String> hits=new ArrayList<>();
    Pattern affectedImport=Pattern.compile("(?m)^\\s*import\\s+javax\\.(?:persistence|validation|servlet|annotation|transaction)(?:\\.|;)");
    try (Stream<Path> paths=Files.walk(base.resolve("src"))) { paths.filter(x->x.toString().endsWith(".java")).forEach(x->{ try { if(affectedImport.matcher(Files.readString(x)).find()) hits.add(base.relativize(x).toString()); } catch(IOException ignored){} }); } catch(IOException ignored) {}
    if(!hits.isEmpty()) { r.affectedSourceFiles.addAll(hits); add(r,"SOURCE_CODE","Jakarta namespace imports",hits.size()+" source file(s)","jakarta.* equivalents",Status.UPGRADE_REQUIRED,Risk.HIGH,Confidence.HIGH,"Replace only the listed affected Java EE imports; review each API's migration guidance.","Boot 3 uses Jakarta EE namespaces for these APIs. Files: "+String.join(", ",hits),docs("BOOT3_JAKARTA",BOOT_3_GUIDE,"Spring Boot 3 migration guide")); }
  }
  private void order(ImpactReport r) { r.migrationOrder.addAll(r.findings.stream().filter(f->f.category().equals("PREREQUISITE")||f.status()==Status.UPGRADE_REQUIRED).map(f->"REQUIRED_BEFORE_MIGRATION: "+f.recommendation()).distinct().toList()); r.migrationOrder.add("REVIEW_DURING_MIGRATION: Upgrade Spring Boot and validate dependency convergence."); r.migrationOrder.add("POST_MIGRATION_VALIDATION: Run the complete test suite and review runtime configuration."); }
  private static Stream<Artifact> artifacts(MavenProject p) { return p.getArtifacts()==null?Stream.empty():p.getArtifacts().stream(); }
  private static boolean managedOverride(MavenProject p,Artifact a) { return p.getOriginalModel().getDependencies().stream().anyMatch(d->d.getGroupId().equals(a.getGroupId())&&d.getArtifactId().equals(a.getArtifactId())&&d.getVersion()!=null); }
  private static String first(Properties p,String... keys) { for(String k:keys)if(p.getProperty(k)!=null)return p.getProperty(k);return null; }
  private static int parseJava(String v) { try { return Integer.parseInt(v.replace("1.","").replaceAll("[^0-9].*","")); } catch(Exception e){return 0;} }
  private static boolean matchesVersionLine(String version,String targetLine) { return version.startsWith(targetLine.replace(".x",".")); }
  private static Evidence bomEvidence(TargetBom targetBom) { String version=targetBom.resolvedVersion(); return new Evidence("BOM_METADATA","TARGET_BOM","https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-dependencies/"+version+"/spring-boot-dependencies-"+version+".pom","Exact resolved Spring Boot BOM"); }
  private static void add(ImpactReport r,String c,String s,String cur,String target,Status st,Risk risk,Confidence conf,String rec,String why,Evidence e) { r.findings.add(new Finding(c,s,cur,target,st,risk,conf,rec,why,List.of(e))); }
  private static void add(ImpactReport r,String c,String s,String cur,String target,Status st,Risk risk,Confidence conf,String rec,String why,List<Evidence> evidence) { r.findings.add(new Finding(c,s,cur,target,st,risk,conf,rec,why,List.copyOf(evidence))); }
}
