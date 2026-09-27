# Changelog

This file records user-visible changes while a release is being developed. Items under **Unreleased** will become the release notes when that version is finalized.

## 0.1.1-SNAPSHOT — Unreleased

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
