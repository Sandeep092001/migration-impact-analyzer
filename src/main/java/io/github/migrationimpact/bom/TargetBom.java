package io.github.migrationimpact.bom;

import java.util.Map;

/** Resolved target BOM facts, kept separate from framework migration knowledge. */
public record TargetBom(String coordinates, String resolvedVersion, Map<String, String> managedVersions) {
  public String managedVersion(String groupId, String artifactId) { return managedVersions.get(groupId + ":" + artifactId); }
}
