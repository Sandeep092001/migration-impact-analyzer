# Knowledge coverage and contribution policy

The analyzer intentionally separates observable Maven facts from compatibility claims.

## Coverage levels

- **BOM managed:** Exact target versions are resolved dynamically from the selected Spring Boot BOM.
- **Verified Boot rule:** A bundled third-party rule is backed by the library's official Spring Boot compatibility documentation.
- **Verified Java-library rule:** A framework-independent library is evaluated using its official Java baseline, module-alignment/runtime contract, and any documented interaction with target-platform changes such as Jackson 3.
- **Unknown:** The library is not managed by the target BOM and has no verified bundled rule. The report asks for review and never claims compatibility.

Verified third-party families currently include Springdoc, MyBatis Spring Boot Starter, Spring Cloud AWS, MyBatis-Plus, JJWT, the Resend Java SDK, and Apache PDFBox. Spring Cloud train mappings and Spring Boot/Java/framework facts are also bundled.

## Coverage target

“80–90% coverage” means that 80–90% of direct dependency occurrences in a documented, representative Spring Boot application corpus receive either exact target-BOM analysis or an evidence-backed family rule. It does not mean that the project claims knowledge of 80–90% of every artifact in Maven Central.

Coverage must be measured by a reproducible fixture/corpus report before a percentage is advertised. Transitive artifacts managed automatically by the target BOM do not each require handwritten rules. Priority is given to direct libraries that repeatedly appear as `UNKNOWN`, especially framework integrations, security/authentication, persistence, messaging, cloud SDKs, HTTP clients, serialization, testing, reporting, and document processing.

## Adding knowledge

Update `src/main/resources/knowledge/library-rules.properties` without changing the engine. Every rule must include official evidence, a precise artifact matcher, current version prefixes, target Boot lines, status, risk, confidence, and an actionable recommendation. Family rules may additionally define a display name, Java minimum, version-alignment requirement, runtime-only modules, and a target description. Add an automated assertion and a fixture before release.

Do not add a compatibility claim based only on a blog, forum answer, search snippet, or an LLM response. When maintainers publish no compatibility contract, leave the result `UNKNOWN`.

The knowledge version must change whenever compatibility data changes. Exact dependency versions should come from Maven/BOM metadata rather than the rule file wherever possible.
