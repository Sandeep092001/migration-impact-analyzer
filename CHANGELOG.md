# Changelog

This file records user-visible changes while a release is being developed. Items under **Unreleased** will become the release notes when that version is finalized.

## 0.1.1 — 2026-09-27

Published to Maven Central. Third-party knowledge is curated and incomplete; uncovered libraries remain explicit manual-review items and will be addressed in later knowledge updates.

### Reporting

- Group console findings into required changes, recommended changes, manual review, and confirmed compatibility.
- Remove the repeated finding emitted for every transitive dependency whose version changes under the target Spring Boot BOM.
- Treat the resolved target BOM and knowledge package as report metadata rather than actionable findings.
- Deduplicate identical findings before console, JSON, and HTML rendering.
- Aggregate Spring Framework and Hibernate findings instead of repeating each resolved artifact.
- Keep aggregate console explanations concise and use the exact resolved target BOM as Hibernate version evidence.
- Add counts for affected source files, required library updates, recommended library updates, manual reviews, managed direct changes, and suppressed transitive BOM changes.
- Add the same summary and metadata to JSON and HTML reports.
- Harden JSON and HTML escaping and permit clickable evidence links only for HTTPS references.

### Analysis accuracy

- Report explicit direct dependency overrides as actionable BOM-alignment findings.
- Count ordinary BOM-managed direct and transitive changes without presenting each one as a manual upgrade action.
- Avoid recommending a Hibernate generation change when the resolved version already belongs to the target generation.

### Knowledge coverage

- Resolve exact stable releases using Maven metadata within explicit, reviewed `releaseRange` bounds. Cache lookups per analysis and exclude prereleases; expose unavailable metadata and newer unreviewed current versions without suggesting a downgrade.
- Add Commons Lang, Text, CSV, IO, Codec, Compress, jsoup and MapStruct rules, with Java baselines and official sources. Target Boot BOM management takes precedence over general Java-library release suggestions.
- Add exact-release resolution to JJWT, PDFBox, MyBatis, and Spring Cloud AWS rules. Exact version selection is not an application compatibility or vulnerability certification.
- Include a `dependencyTargets` inventory in JSON for every resolved direct dependency, with current version, target and knowledge/BOM basis. Unknown targets remain explicit.

- Extend the rule schema with display names, explicit targets, Java baselines, risk, confidence, custom recommendations, family version alignment, and runtime-scope requirements.
- Add evidence-backed compatibility knowledge for the Resend Java SDK, JJWT module family, and Apache PDFBox.
- Collapse the JJWT API, implementation, and JSON adapter artifacts into one family-level finding.
- Detect misaligned JJWT/PDFBox module versions and incorrect JJWT runtime scopes.
- Distinguish framework-independent Java compatibility from genuine Spring Boot version coupling instead of treating every unmanaged library as unknown.
- Stop emitting an unconditional Maven Compiler Plugin recommendation when the configured Java baseline already satisfies the target Spring Boot requirement.
