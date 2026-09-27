package io.github.migrationimpact.bom;

import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.*;
import java.util.*;

/** Resolves the official Spring Boot BOM through Maven, including the latest patch in a requested line. */
public final class TargetBomResolver {
  private final RepositorySystem repositories; private final RepositorySystemSession session; private final List<RemoteRepository> remotes;
  public TargetBomResolver(RepositorySystem repositories, RepositorySystemSession session, List<RemoteRepository> remotes) { this.repositories=repositories; this.session=session; this.remotes=remotes; }
  public TargetBom resolveSpringBoot(String target) throws Exception {
    if(target==null || !target.matches("spring-boot:\\d+\\.\\d+(?:\\.\\d+)?")) throw new IllegalArgumentException("Expected spring-boot:<major>.<minor>[.<patch>]");
    String requested=target.substring("spring-boot:".length()); String version=exactVersion(requested);
    var bom=new DefaultArtifact("org.springframework.boot", "spring-boot-dependencies", "", "pom", version);
    var request=new ArtifactDescriptorRequest().setArtifact(bom).setRepositories(remotes);
    var descriptor=repositories.readArtifactDescriptor(session, request);
    Map<String,String> managed=new TreeMap<>();
    for (Dependency dependency:descriptor.getManagedDependencies()) { var artifact=dependency.getArtifact(); if (artifact.getVersion()!=null) managed.put(artifact.getGroupId()+":"+artifact.getArtifactId(), artifact.getVersion()); }
    return new TargetBom("org.springframework.boot:spring-boot-dependencies", version, Map.copyOf(managed));
  }
  private String exactVersion(String requested) throws Exception {
    if (!requested.matches("\\d+\\.\\d+")) return requested;
    String[] bits=requested.split("\\."); int next=Integer.parseInt(bits[1])+1;
    var range="["+requested+","+bits[0]+"."+next+")";
    var artifact=new DefaultArtifact("org.springframework.boot", "spring-boot-dependencies", "", "pom", range);
    var result=repositories.resolveVersionRange(session,new VersionRangeRequest().setArtifact(artifact).setRepositories(remotes));
    String stablePrefix=requested+".";
    return result.getVersions().stream().filter(v -> v.toString().startsWith(stablePrefix) && v.toString().matches("\\d+\\.\\d+\\.\\d+"))
        .max(Comparator.naturalOrder()).orElseThrow(() -> new IllegalArgumentException("No stable Spring Boot BOM release found in "+range)).toString();
  }
}
