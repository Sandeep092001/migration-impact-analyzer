# Migration Impact Analyzer

A read-only Maven plugin that maps a Spring Boot migration's likely blast radius before any project files are changed.

```bash
mvn install

# In the Maven project you want to inspect:
mvn io.github.migration-impact:migration-impact-maven-plugin:0.1.0-SNAPSHOT:analyze \
  -Dsource=spring-boot:2.7 -Dtarget=spring-boot:3.5 -DreportFormat=all
```

Reports are written to `target/migration-impact/` (`report.json` and `report.html`) and a concise report is printed to the console. The plugin uses Maven's resolved `MavenProject` artifacts, dependency-management model, plugins, profiles, and properties; it never writes to the analysed project.

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

## Demo project

[`examples/spring-boot-2x-demo`](examples/spring-boot-2x-demo) is an intentionally legacy-shaped Spring Boot 2.7 fixture for manual end-to-end testing. It exercises the current Java, Spring Cloud, Springdoc, Hibernate, compiler-plugin, and Jakarta-import rules.
