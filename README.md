# Migration Impact Analyzer

A read-only Maven plugin that maps a Spring Boot migration's likely blast radius before any project files are changed. It combines the target Spring Boot BOM with a curated migration knowledge package to recommend dependency targets and identify work that still needs human review.

> **Coverage notice:** The plugin does not yet cover every third-party library used by Spring Boot applications. Unsupported or insufficiently documented libraries are reported as `UNKNOWN` or manual review instead of receiving a guessed compatibility recommendation. The bundled catalog will continue to grow in future releases as rules are verified against authoritative sources.

## Requirements and usage

```bash
mvn io.github.sandeep092001:migration-impact-maven-plugin:0.1.1:analyze \
  -Dsource=spring-boot:2.7 -Dtarget=spring-boot:3.5 -DreportFormat=all
```

Version 0.1.1 is published on Maven Central. It requires Java 17+ and Maven 3.9+.

For the shorter command, declare this under your application's `build/plugins`, not `dependencies`:

```xml
<plugin>
  <groupId>io.github.sandeep092001</groupId>
  <artifactId>migration-impact-maven-plugin</artifactId>
  <version>0.1.1</version>
</plugin>
```

Then run `mvn migration:analyze -Dsource=spring-boot:2.7 -Dtarget=spring-boot:3.5 -DreportFormat=all`.

Reports are written to `target/migration-impact/` (`report.json` and `report.html`). The plugin does not modify application source or POM files.

Exact stable releases are selected only within reviewed ranges when repository metadata is available. Recommendations do not guarantee application compatibility or freedom from vulnerabilities: test the migrated application. See [coverage, evidence requirements, and limitations](KNOWLEDGE.md).

## What the report means

Console findings are grouped into required changes, recommended changes, manual reviews, and confirmed compatibility. The final impact summary counts affected source files and unique library actions. Ordinary transitive version changes managed automatically by the target Spring Boot BOM are counted once instead of being repeated as individual recommendations.

The bundled rule set supports Spring Boot 2.0–2.7, 3.0–3.5, and 4.0–4.1 version lines, including patch inputs such as `3.0.1` → `3.0.2`. It covers Java requirements, Spring Cloud, Jakarta imports, managed-version changes, Maven build plugins, and the reviewed third-party families listed below. Findings state their evidence and confidence; when a rule cannot establish compatibility it emits `UNKNOWN`, not a positive claim.

## Evidence policy

Rules link to official documentation or to the Maven project model used as evidence. Target facts live in [`src/main/resources/knowledge/spring-boot-targets.properties`](src/main/resources/knowledge/spring-boot-targets.properties), and third-party rules live in [`src/main/resources/knowledge/library-rules.properties`](src/main/resources/knowledge/library-rules.properties). They are versioned separately from the analysis logic inside the source tree.

Version 0.1.1 includes reviewed knowledge for Springdoc, MyBatis Spring Boot Starter, Spring Cloud AWS, MyBatis-Plus, JJWT, Resend Java SDK, Apache PDFBox, Apache Commons Lang/Text/CSV/IO/Codec/Compress, jsoup, and MapStruct. The target Spring Boot BOM dynamically supplies versions for the much larger set of dependencies it manages. This list is meaningful coverage, not universal coverage; unmanaged libraries without a verified rule remain visible as `UNKNOWN`.

Knowledge updates are bundled with plugin releases in V1. Developers should use the newest plugin release to receive new rules and corrections. A separately updatable knowledge package remains a future architectural extension.

To test an updated knowledge package before it is bundled, pass a directory containing compatible `spring-boot-targets.properties` and `library-rules.properties` files:

```bash
mvn migration:analyze -Dsource=spring-boot:3.0.1 -Dtarget=spring-boot:3.0.2 \
  -DknowledgeDirectory=/path/to/migration-knowledge
```

## Development

```bash
mvn verify
```

User-visible work planned for the next release is tracked in [`CHANGELOG.md`](CHANGELOG.md).

Contributions that add knowledge must include authoritative evidence, a narrow artifact/version matcher, and automated coverage. See [KNOWLEDGE.md](KNOWLEDGE.md) before proposing compatibility rules.

## Demo project

[`examples/spring-boot-2x-demo`](examples/spring-boot-2x-demo) is an intentionally legacy-shaped Spring Boot 2.7 fixture for manual end-to-end testing. It exercises the current Java, Spring Cloud, Springdoc, Hibernate, compiler-plugin, and Jakarta-import rules.
