package io.github.migrationimpact.platform;

import io.github.migrationimpact.model.ImpactReport.Status;
import java.io.*;
import java.nio.file.*;
import java.util.*;

final class LibraryKnowledge {
  record Rule(String id, String group, List<String> artifacts, List<String> currentPrefixes,
              Set<String> targets, String targetArtifact, String targetVersion,
              Status status, String reason, String evidence) {
    boolean matches(String g, String artifact, String version, String targetLine) {
      if (!group.equals(g) || !targets.contains(targetLine)) return false;
      boolean artifactMatch=artifacts.stream().anyMatch(x -> x.endsWith("*") ? artifact.startsWith(x.substring(0,x.length()-1)) : artifact.equals(x));
      boolean versionMatch=currentPrefixes.contains("*") || currentPrefixes.stream().anyMatch(version::startsWith);
      return artifactMatch && versionMatch;
    }
  }
  static List<Rule> load(Path directory) {
    Properties p=new Properties();
    try(InputStream in=directory==null ? LibraryKnowledge.class.getResourceAsStream("/knowledge/library-rules.properties") : Files.newInputStream(directory.resolve("library-rules.properties"))) {
      if(in==null)return List.of(); p.load(in);
    } catch(IOException e){return List.of();}
    List<Rule> result=new ArrayList<>();
    for(String id:csv(p.getProperty("rules",""))) { String k="rule."+id+"."; result.add(new Rule(id,p.getProperty(k+"group"),csv(p.getProperty(k+"artifact")),csv(p.getProperty(k+"current")),Set.copyOf(csv(p.getProperty(k+"targets"))),p.getProperty(k+"targetArtifact"),p.getProperty(k+"targetVersion"),Status.valueOf(p.getProperty(k+"status")),p.getProperty(k+"reason"),p.getProperty(k+"evidence"))); }
    return List.copyOf(result);
  }
  private static List<String> csv(String value){return Arrays.stream(value.split(",")).map(String::trim).filter(x->!x.isEmpty()).toList();}
}
