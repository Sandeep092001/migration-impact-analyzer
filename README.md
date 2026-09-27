# Migration Impact Analyzer

A read-only Maven plugin that maps a Spring Boot migration's likely blast radius before any project files are changed.

```bash
mvn install

# In the Maven project you want to inspect:
mvn io.github.sandeep092001:migration-impact-maven-plugin:0.1.1:analyze \
  -Dsource=spring-boot:2.7 -Dtarget=spring-boot:3.5 -DreportFormat=all
```

Requires Java 17+ and Maven 3.9+. Install 0.1.1 locally until it is published to Central.

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

Third-party coverage is curated, not universal. Exact stable releases are selected within reviewed ranges when metadata is available. Recommendations do not guarantee application compatibility or freedom from vulnerabilities: test the migrated application. See [coverage and limitations](KNOWLEDGE.md).

Console findings are grouped into required changes, recommended changes, manual reviews, and confirmed compatibility. The final impact summary counts affected source files and unique library actions. Ordinary transitive version changes managed automatically by the target Spring Boot BOM are counted once instead of being repeated as individual recommendations.

The initial rule set supports Spring Boot 2.0–2.7, 3.0–3.5, and 4.0–4.1 version lines, including patch inputs such as `3.0.1` → `3.0.2`. It covers Java requirements, Spring Cloud, Springdoc 1.x, Jakarta imports, managed-version changes, and Maven build plugins. Findings always state their evidence and confidence; when a rule cannot establish compatibility it emits `UNKNOWN`, not a positive claim.

## Evidence policy

Rules link only to official documentation or the Maven project model used as evidence. Target facts live in [`src/main/resources/knowledge/spring-boot-targets.properties`](src/main/resources/knowledge/spring-boot-targets.properties), and third-party compatibility rules live in [`src/main/resources/knowledge/library-rules.properties`](src/main/resources/knowledge/library-rules.properties). They can be reviewed, versioned, and updated without changing analysis logic. V1 includes verified rules for Springdoc, MyBatis, Spring Cloud AWS, and MyBatis-Plus; target-BOM metadata covers managed libraries dynamically. Unmanaged libraries without a verified rule are reported as `UNKNOWN`.

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

## Demo project

[`examples/spring-boot-2x-demo`](examples/spring-boot-2x-demo) is an intentionally legacy-shaped Spring Boot 2.7 fixture for manual end-to-end testing. It exercises the current Java, Spring Cloud, Springdoc, Hibernate, compiler-plugin, and Jakarta-import rules.
