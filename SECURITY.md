# Security policy

## Supported versions

Security fixes are developed for the next release. Users should run the newest published plugin version and a currently supported Maven 3.9.x release on a maintained JDK.

## Reporting a vulnerability

Do not open a public issue containing an exploit, credential, private repository URL, or sensitive report. Use GitHub's private **Report a vulnerability** feature for this repository. Include the affected plugin version, a minimal reproduction with secrets removed, impact, and any suggested mitigation.

Never attach Maven `settings.xml`, Central tokens, private GPG keys, environment dumps, or debug logs that may contain repository credentials.

## Security boundaries

The analyzer reads the Maven project model, resolved dependency graph, selected Java source files, bundled or explicitly selected knowledge files, and metadata from repositories already configured in Maven. It writes only `report.json` and/or `report.html` to the configured report directory. It does not execute project source code, invoke a shell, modify the analyzed POM or source tree, collect telemetry, or intentionally read Maven credentials.

Maven builds and project-configured repositories are outside the plugin's trust boundary. Analyze only projects and external knowledge packages you trust. The generated report can contain project coordinates, dependency versions, relative source paths, and migration findings; review it before publishing it.

Compatibility recommendations are not vulnerability findings and are not a substitute for dependency vulnerability scanning, application testing, or security review.
