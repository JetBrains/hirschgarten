package org.jetbrains.bazel.sync.workspace.mapper.normal

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.BuildTargetTag
import org.jetbrains.bsp.protocol.MavenCoordinates
import java.nio.file.Path
import kotlin.io.path.extension

@ApiStatus.Internal
object MavenCoordinatesResolver {
  private const val mavenCoordsTagPrefix = BuildTargetTag.MAVEN_COORDINATES + "="

  fun fromTargetTagsList(tags: Collection<String>): MavenCoordinates? {
    val mavenCoordsTag =
      tags
        .firstOrNull { it.startsWith(mavenCoordsTagPrefix) }
        ?.removePrefix(mavenCoordsTagPrefix)
      ?: return null

    val parts = mavenCoordsTag.split(':')
    val groupId = parts.getOrNull(0) ?: return null
    val artifactId = parts.getOrNull(1) ?: return null
    val version = parts.getOrNull(2) ?: return null
    return MavenCoordinates(
      groupId = groupId,
      artifactId = artifactId,
      version = version
    )
  }

  fun resolveMavenCoordinates(libraryLabel: Label, outputJar: Path): MavenCoordinates? {
    /* For example:
     * @@rules_jvm_external~override~maven~maven//:org_apache_commons_commons_lang3 -> org
     * @maven//:com_google_auto_service_auto_service_annotations -> com
     */
    val topLevelDomain =
      libraryLabel
        .targetName
        .split('_', limit = 2)
        .firstOrNull() ?: "org"
    // Match the Maven group (organization), artifact, and version in the Bazel dependency
    // string such as .../execroot/monorepo/bazel-out/k8-fastbuild/bin/external/maven/com/google/guava/guava/31.1-jre/processed_guava-31.1-jre.jar
    // bazel-out/k8-fastbuild/bin/external/rules_jvm_external~~maven~name/v1/https/repo1.maven.org/maven2/com/google/auto/service/auto-service-annotations/1.1.1/header_auto-service-annotations-1.1.1.jar
    if (outputJar.extension != "jar") return null

    val pathSegments = (0..<outputJar.nameCount).map { i -> outputJar.getName(i).toString() }
    val organizationFromIndex = pathSegments.indexOf(topLevelDomain).takeIf { it != -1 } ?: return null
    val organizationToIndex = pathSegments.size - 3
    if (organizationFromIndex >= organizationToIndex) return null
    val organization = pathSegments.subList(organizationFromIndex, organizationToIndex).joinToString(".")

    val artifact = pathSegments[pathSegments.size - 3]
    val version = pathSegments[pathSegments.size - 2]
    return MavenCoordinates(organization, artifact, version)
  }
}
