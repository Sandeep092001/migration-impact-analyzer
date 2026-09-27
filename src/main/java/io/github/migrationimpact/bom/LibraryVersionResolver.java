package io.github.migrationimpact.bom;

import java.util.*;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.VersionRangeRequest;

/** Resolves releases only inside a range supplied by reviewed migration knowledge. */
public final class LibraryVersionResolver {
  private final RepositorySystem system;
  private final RepositorySystemSession session;
  private final List<RemoteRepository> repositories;
  private final Map<String,String> cache=new HashMap<>();
  public LibraryVersionResolver(RepositorySystem system,RepositorySystemSession session,List<RemoteRepository> repositories) {
    this.system=system; this.session=session; this.repositories=repositories;
  }
  public String resolve(String coordinates,String range) throws Exception {
    String key=coordinates+":"+range;
    if(cache.containsKey(key)) return cache.get(key);
    String[] parts=coordinates.split(":");
    if(parts.length!=2 || !range.startsWith("[")) throw new IllegalArgumentException("Expected group:artifact and a bounded release range");
    var artifact=new DefaultArtifact(parts[0],parts[1],"","pom",range);
    var result=system.resolveVersionRange(session,new VersionRangeRequest(artifact,repositories,"migration-knowledge"));
    String version=result.getVersions().stream().filter(v -> isStable(v.toString())).max(Comparator.naturalOrder())
        .orElseThrow(() -> new IllegalArgumentException("No stable release in reviewed range")).toString();
    cache.put(key,version);
    return version;
  }
  public static boolean isStable(String version) {
    return version.matches("[0-9]+(?:\\.[0-9]+)*(?:[.-](?:Final|RELEASE|GA))?");
  }
}
