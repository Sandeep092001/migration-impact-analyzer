# Knowledge coverage and contribution policy

The analyzer intentionally separates observable Maven facts from compatibility claims.

## Coverage levels

- **BOM managed:** Exact target versions are resolved dynamically from the selected Spring Boot BOM.
- **Verified rule:** A bundled third-party rule is backed by the library's official compatibility documentation.
- **Unknown:** The library is not managed by the target BOM and has no verified bundled rule. The report asks for review and never claims compatibility.

V1 verified third-party families are Springdoc, MyBatis Spring Boot Starter, Spring Cloud AWS, and MyBatis-Plus. Spring Cloud train mappings and Spring Boot/Java/framework facts are also bundled.

## Adding knowledge

Update `src/main/resources/knowledge/library-rules.properties` without changing the engine. Every rule must include an official evidence URL, precise artifact matcher, current version prefix, target Boot lines, target artifact/version line, and status. Add an automated assertion and a fixture before release.

Do not add a compatibility claim based only on a blog, forum answer, search snippet, or an LLM response. When maintainers publish no compatibility contract, leave the result `UNKNOWN`.

The knowledge version must change whenever compatibility data changes. Exact dependency versions should come from Maven/BOM metadata rather than the rule file wherever possible.
