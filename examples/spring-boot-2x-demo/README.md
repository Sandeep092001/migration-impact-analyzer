# Spring Boot 2.x migration fixture

This intentionally uses Spring Boot 2.7, Java 11, Spring Cloud 2021.0.x, Springdoc 1.x, JPA/Hibernate, and `javax.*` imports. It is a fixture for validating the analyzer; it is not a production application.

From the analyzer project root, first install the snapshot:

```bash
mvn install
```

Then run this from this directory:

```bash
mvn io.github.migration-impact:migration-impact-maven-plugin:0.1.0-SNAPSHOT:analyze \
  -Dsource=spring-boot:2.7 -Dtarget=spring-boot:3.5 -DreportFormat=all
```

Expected verified findings include Java 17+, Spring Cloud upgrade, Springdoc replacement, Hibernate review, the compiler configuration, and affected Jakarta imports. The JSON and HTML reports appear under `target/migration-impact/`.
