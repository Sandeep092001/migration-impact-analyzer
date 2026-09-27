package io.github.migrationimpact.platform;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Reads the bundled, reviewable target-version facts. It deliberately has no network access. */
final class TargetKnowledge {
  private final String line; private final Properties facts;
  private TargetKnowledge(String line, Properties facts) { this.line=line; this.facts=facts; }
  static TargetKnowledge forTarget(String target) { return load(null, target); }
  static TargetKnowledge load(Path knowledgeDirectory, String target) {
    String[] parts=target.split(":",2);
    if(parts.length!=2 || !parts[0].equals("spring-boot") || !parts[1].matches("\\d+\\.\\d+(?:\\.\\d+)?")) return null;
    Properties facts=new Properties();
    try (InputStream input=knowledgeDirectory == null ? TargetKnowledge.class.getResourceAsStream("/knowledge/spring-boot-targets.properties") : Files.newInputStream(knowledgeDirectory.resolve("spring-boot-targets.properties"))) { if(input==null)return null; facts.load(input); } catch(IOException e) { return null; }
    String version=parts[1]; String line=version.replaceFirst("^(\\d+\\.\\d+).*","$1");
    return facts.containsKey(line+".java.minimum") ? new TargetKnowledge(line,facts) : null;
  }
  String line(){return line;} String javaMinimum(){return get("java.minimum");} String framework(){return get("spring.framework");}
  String version(){return facts.getProperty("knowledge.version", "unknown");}
  String cloudTrain(){return get("spring.cloud.train");} String springdocMajor(){return get("springdoc.major");}
  String hibernateMajor(){return get("hibernate.major");}
  String springdocArtifact(){return get("springdoc.artifact");} String bootEvidence(){return get("evidence.boot");}
  String cloudEvidence(){return get("evidence.cloud");} String springdocEvidence(){return get("evidence.springdoc");}
  private String get(String name){return facts.getProperty(line+"."+name);}
}
