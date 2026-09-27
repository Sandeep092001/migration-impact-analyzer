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
    assertTrue(rules.stream().anyMatch(r->r.id().equals("jjwt-java-library") && r.displayName().equals("JJWT modules") && r.alignVersions()));
    assertTrue(rules.stream().anyMatch(r->r.id().equals("resend-java-boot4") && r.javaMinimum()==8));
    assertTrue(rules.stream().anyMatch(r->r.id().equals("pdfbox3-java-library") && r.status().name().equals("COMPATIBLE")));
    var jjwt=rules.stream().filter(r->r.id().equals("jjwt-java-library")).findFirst().orElseThrow();
    assertTrue(jjwt.matches("io.jsonwebtoken","jjwt-api","0.12.6","4.0"));
    assertTrue(jjwt.matches("io.jsonwebtoken","jjwt-jackson","0.12.6","4.0"));
    var resend=rules.stream().filter(r->r.id().equals("resend-java-boot4")).findFirst().orElseThrow();
    assertEquals(2,resend.evidence().size());
    assertTrue(resend.matches("com.resend","resend-java","4.13.0","4.0"));
  }
}
