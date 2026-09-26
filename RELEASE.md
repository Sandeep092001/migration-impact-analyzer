# Release guide

This project is technically prepared for a first public release, but publisher identity must be supplied by the maintainer.

## Required decisions

1. Create the public GitHub repository and choose its final URL.
2. Verify the `io.github.sandeep092001` namespace in Maven Central Portal.
3. Maintainer identity and project URL/SCM coordinates are present in `pom.xml`.
4. Create a Central Portal account/token and a public GPG signing key.
5. Version `0.1.0` is selected for the first immutable release; do not reuse it after publication.

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
