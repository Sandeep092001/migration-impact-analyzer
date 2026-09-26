# Release guide

This project is technically prepared for a first public release, but publisher identity must be supplied by the maintainer.

## Required decisions

1. Create the public GitHub repository and choose its final URL.
2. Choose and verify a Maven Central namespace. A `io.github.<github-user>` group is normally tied to that GitHub identity.
3. Confirm the maintainer identity in `pom.xml`; project URL/SCM coordinates already point to the configured GitHub origin.
4. Create a Central Portal account/token and a public GPG signing key.
5. Change `0.1.0-SNAPSHOT` to `0.1.0` only after the release bundle passes validation.

## Verification

```bash
mvn clean verify
```

After real publisher metadata is present, build the signed Central bundle with:

```bash
mvn clean deploy -Prelease
```

The `release` profile attaches source and Javadoc JARs, signs artifacts, and uploads through the Central Portal plugin with manual publication enabled.

Then install the snapshot and run the analyzer against `examples/spring-boot-2x-demo` for Boot 3.5 and 4.0 targets. Review both JSON and HTML output.

## Central requirements

Maven Central requires a release POM with project URL, license, developer, SCM, verified coordinates, source and Javadoc JARs, and signatures. Do not publish placeholder identity metadata. Releases are immutable after publication.

Official instructions: https://central.sonatype.org/publish/publish-portal-maven/
