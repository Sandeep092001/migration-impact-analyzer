package io.github.migrationimpact.graph;

import org.apache.maven.project.*;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.graph.DependencyNode;
import java.util.*;

/** Builds a stable, human-readable path for each resolved dependency. */
public final class DependencyPathFinder {
  private final ProjectDependenciesResolver resolver; private final RepositorySystemSession session;
  public DependencyPathFinder(ProjectDependenciesResolver resolver, RepositorySystemSession session) { this.resolver=resolver; this.session=session; }
  public Map<String,String> paths(MavenProject project) throws DependencyResolutionException {
    DependencyNode root=resolver.resolve(new DefaultDependencyResolutionRequest(project,session)).getDependencyGraph();
    Map<String,String> found=new HashMap<>(); walk(root,new ArrayList<>(),found); return Map.copyOf(found);
  }
  private void walk(DependencyNode node, List<String> parent, Map<String,String> found) {
    for (DependencyNode child:node.getChildren()) {
      var artifact=child.getArtifact(); if(artifact==null) continue;
      String ga=artifact.getGroupId()+":"+artifact.getArtifactId();
      List<String> path=new ArrayList<>(parent); path.add(ga+":"+artifact.getVersion());
      found.putIfAbsent(ga,String.join(" -> ",path)); walk(child,path,found);
    }
  }
}
