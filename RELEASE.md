# Release guide

Version 0.1.1 is published from `main` and tagged `v0.1.1`. Java 17+ and Maven 3.9+ are required. Coverage is curated, not universal; this release is not a security or application-compatibility certification.

## Required decisions

The namespace, identity, signing setup and Central token were established for 0.1.0. Keep credentials private and outside Git. Version 0.1.0 is immutable; this release uses 0.1.1.

## Verification

```bash
mvn --batch-mode --no-transfer-progress clean verify -Prelease -Dgpg.skip=true
mvn install
mvn -f examples/spring-boot-2x-demo/pom.xml migration:analyze -Dsource=spring-boot:2.7 -Dtarget=spring-boot:3.5 -DreportFormat=all
mvn -f examples/spring-boot-2x-demo/pom.xml migration:analyze -Dsource=spring-boot:2.7 -Dtarget=spring-boot:4.0 -DreportFormat=all
git diff --check
```

Run these from the repository root. The release verification builds source and Javadoc JARs without signing or uploading. Review each demo report before the next run overwrites it.

Run verification from a clean `main` checkout. Before deployment, confirm tag `v0.1.1` identifies the plugin source, POM, and knowledge resources being released: the POM references this tag. Later documentation-only commits do not require moving the release tag. If Java code, the POM, or bundled resources change after tagging but before publication, make the tag match the new release commit before pushing it. Do not move a tag after artifacts have been published.

Then, from that checkout in your own terminal, build and upload the signed Central bundle:

```bash
export GPG_TTY=$(tty)
mvn --no-transfer-progress clean deploy -Prelease
```

The `release` profile attaches source and Javadoc JARs, signs artifacts, and uploads through the Central Portal plugin with manual publication enabled.

Do not skip signing for deployment. Maven uses your existing local signing setup and private Maven settings (`central` server ID). Never commit credentials, private keys or target output, or share debug logs containing secrets.

Wait for Central validation, inspect the deployment, then click Publish. Confirm PUBLISHED before announcing availability. Verify the published version from a separate consumer environment without a locally installed copy. Record the publication date in CHANGELOG.md and begin the next snapshot on a development branch; never overwrite published 0.1.1 artifacts.

Release notes and announcements must say that the third-party catalog is incomplete and will expand through future plugin/knowledge releases. Do not advertise universal coverage or guaranteed compatibility.

## Central requirements

Maven Central requires a release POM with project URL, license, developer, SCM, verified coordinates, source and Javadoc JARs, and signatures. Do not publish placeholder identity metadata. Releases are immutable after publication.

Official instructions: https://central.sonatype.org/publish/publish-portal-maven/
