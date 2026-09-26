package io.github.migrationimpact.platform;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class KnowledgeTest {
  @Test void patchVersionsMapToTheirKnowledgeLine() {
    assertEquals("3.0",TargetKnowledge.forTarget("spring-boot:3.0.2").line());
    assertEquals("4.1",TargetKnowledge.forTarget("spring-boot:4.1.1").line());
  }
  @Test void bundledLibraryRulesContainVerifiedEcosystemMappings() {
    var rules=LibraryKnowledge.load(null);
    assertTrue(rules.stream().anyMatch(r->r.id().equals("springdoc-boot4")));
    assertTrue(rules.stream().anyMatch(r->r.id().equals("mybatis-boot3")));
    assertTrue(rules.stream().anyMatch(r->r.id().equals("cloud-aws-boot4")));
  }
}
