package org.jetbrains.bazel.sync.workspace.mapper.normal

import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.MavenCoordinates
import java.nio.file.Path

class MavenCoordinatesResolver {
  fun resolveMavenCoordinates(libraryLabel: Label, outputJar: Path): MavenCoordinates? {
    /* For example:
     * @@rules_jvm_external~override~maven~maven//:org_apache_commons_commons_lang3
     * @maven//:org_scala_lang_scala_library
     **/
    val orgStart =
      libraryLabel
        .toString()
        .split("//:")
        .lastOrNull()
        ?.split('_')
        ?.firstOrNull() ?: "org"
    // Matches the Maven group (organization), artifact, and version in the Bazel dependency
    // string such as .../execroot/monorepo/bazel-out/k8-fastbuild/bin/external/maven/com/google/guava/guava/31.1-jre/processed_guava-31.1-jre.jar
    // bazel-out/k8-fastbuild/bin/external/rules_jvm_external~~maven~name/v1/https/repo1.maven.org/maven2/com/google/auto/service/auto-service-annotations/1.1.1/header_auto-service-annotations-1.1.1.jar
    val regexPattern = """.*/($orgStart/.+)/([^/]+)/([^/]+)/[^/]+.jar""".toRegex()
    val matchResult = regexPattern.find(outputJar.toString())
    // If a match is found, group values are extracted; otherwise, null is returned
    return matchResult?.let {
      val (organization, artifact, version) = it.destructured
      MavenCoordinates(organization.replace("/", "."), artifact, version)
    }
  }
}
