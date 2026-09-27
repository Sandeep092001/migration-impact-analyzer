# Knowledge coverage and contribution policy

The analyzer intentionally separates observable Maven facts from compatibility claims. The catalog is useful but incomplete: it does not cover every library that can appear in a Spring Boot application, and no release should claim otherwise without a measured corpus.

## Coverage levels

- **BOM managed:** Exact target versions are resolved dynamically from the selected Spring Boot BOM.
- **Verified Boot rule:** A bundled third-party rule is backed by the library's official Spring Boot compatibility documentation.
- **Verified Java-library rule:** A framework-independent library is evaluated using its official Java baseline, module-alignment/runtime contract, and any documented interaction with target-platform changes such as Jackson 3.
- **Unknown:** The library is not managed by the target BOM and has no verified bundled rule. The report asks for review and never claims compatibility.

Reviewed third-party families currently include Springdoc, MyBatis Spring Boot Starter, Spring Cloud AWS, MyBatis-Plus, JJWT, the Resend Java SDK, Apache PDFBox, Apache Commons Lang/Text/CSV/IO/Codec/Compress, jsoup, and MapStruct. Some rules establish Spring Boot integration compatibility; framework-independent rules establish only documented Java baselines, module alignment, and reviewed release boundaries. Spring Cloud train mappings and Spring Boot/Java/framework facts are also bundled.

## Coverage target

“80–90% coverage” means that 80–90% of direct dependency occurrences in a documented, representative Spring Boot application corpus receive either exact target-BOM analysis or an evidence-backed family rule. It does not mean that the project claims knowledge of 80–90% of every artifact in Maven Central.

Coverage must be measured by a reproducible fixture/corpus report before a percentage is advertised. Transitive artifacts managed automatically by the target BOM do not each require handwritten rules. Priority is given to direct libraries that repeatedly appear as `UNKNOWN`, especially framework integrations, security/authentication, persistence, messaging, cloud SDKs, HTTP clients, serialization, testing, reporting, and document processing.

Each future plugin release may add or correct bundled knowledge. Until separately versioned knowledge packages are implemented, users receive those updates by upgrading the plugin. Missing knowledge is expected to remain visible in reports so the next catalog additions can be prioritized from real projects.

## Adding knowledge

### Exact release selection

Optional `releaseArtifact=group:artifact` and `releaseRange=[minimum,exclusive-upper)` fields select an exact stable release through Maven Resolver using the user's configured repositories and offline policy. Ranges are reviewed compatibility boundaries, not unrestricted latest-version queries. Qualifiers other than Final/RELEASE/GA are excluded. Missing metadata stays unresolved, and a newer installed version never receives an automatic downgrade recommendation.

Knowledge package 2026.09.27.2 adds reviewed release lines for Commons Lang 3.20, Text 1.15, CSV 1.14, IO 2.22, Codec 1.22, Compress 1.28, jsoup 1.23 and MapStruct 1.6. Evidence links are recorded per rule. These Java-library rules apply to Boot 3.x/4.x; they describe a Java baseline and a release candidate, not tested application integration. Security advisories, custom serialization, transitive convergence and application API usage still require separate checks.

The `dependencyTargets` JSON array inventories resolved direct artifacts even when console findings are grouped. `TARGET_BOM` means the exact version selected by the target platform; `RULE:<id>` links to a reviewed family rule; `UNKNOWN` exposes missing knowledge. Family targets identify a representative artifact or a BOM and do not instruct replacing every module with that representative artifact. The catalog does not claim universal or measured 80–90% coverage yet.

Update `src/main/resources/knowledge/library-rules.properties` without changing the engine. Every rule must include official evidence, a precise artifact matcher, current version prefixes, target Boot lines, status, risk, confidence, and an actionable recommendation. Family rules may additionally define a display name, Java minimum, version-alignment requirement, runtime-only modules, and a target description. Add an automated assertion and a fixture before release.

Do not add a compatibility claim based only on a blog, forum answer, search snippet, or an LLM response. When maintainers publish no compatibility contract, leave the result `UNKNOWN`.

The knowledge version must change whenever compatibility data changes. Exact dependency versions should come from Maven/BOM metadata rather than the rule file wherever possible.
