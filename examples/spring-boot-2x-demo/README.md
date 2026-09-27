# Spring Boot 2.x migration fixture

This intentionally uses Spring Boot 2.7, Java 11, Spring Cloud 2021.0.x, Springdoc 1.x, JPA/Hibernate, and `javax.*` imports. It is a fixture for validating the analyzer; it is not a production application.

To test local source changes, first install the plugin from the analyzer project root:

```bash
mvn install
```

For the published 0.1.1 release, no local installation is required. Run this from the example directory:

```bash
mvn io.github.sandeep092001:migration-impact-maven-plugin:0.1.1:analyze \
  -Dsource=spring-boot:2.7 -Dtarget=spring-boot:3.5 -DreportFormat=all
```

Expected verified findings include Java 17+, Spring Cloud upgrade, Springdoc replacement, Hibernate review, the compiler configuration, and affected Jakarta imports. The JSON and HTML reports appear under `target/migration-impact/`.

This fixture demonstrates covered rules; it is not evidence that every third-party Spring Boot library is supported. In real projects, dependencies outside the target BOM and bundled knowledge catalog are intentionally reported for manual review. Coverage will expand in later plugin releases as additional rules gain authoritative evidence and automated tests.
