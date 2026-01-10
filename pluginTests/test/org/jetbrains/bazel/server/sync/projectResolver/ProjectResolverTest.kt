package org.jetbrains.bazel.server.sync.projectResolver

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import org.jetbrains.bazel.commons.RepoMappingDisabled
import org.jetbrains.bazel.info.BspTargetInfo.Dependency
import org.jetbrains.bazel.info.BspTargetInfo.TargetInfo
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.sync.ProjectResolver
import org.junit.jupiter.api.Test

class ProjectResolverTest {
  @Test
  fun `should replace dependency labels from maven_project_jar to their corresponding java library`() {
    val aProject = targetInfo("@@//a-project", emptyList(), "maven_project_jar")
    val aLib = targetInfo("@@//a-lib", emptyList(), "java_library")
    val b = targetInfo("@@//b", listOf("@@//a-project"), "java_test")
    val rawTargetInfoMap = toIdToTargetInfoMap(aProject, aLib, b)

    val processedDependencies =
      ProjectResolver.processDependenciesList(
        b.toBuilder().dependenciesBuilderList,
        rawTargetInfoMap,
      )
    processedDependencies shouldContainExactlyInAnyOrder listOf(dependency("@@//a-lib"))
  }

  private fun targetInfo(
    id: String,
    dependenciesIds: List<String>,
    kind: String,
  ): TargetInfo =
    TargetInfo
      .newBuilder()
      .setId(id)
      .addAllDependencies(
        dependenciesIds.map { dependency(it) },
      ).setKind(kind)
      .build()

  private fun dependency(id: String): Dependency =
    Dependency
      .newBuilder()
      .setId(id)
      .build()

  private fun toIdToTargetInfoMap(vararg targetIds: TargetInfo): Map<Label, TargetInfo> =
    targetIds.associateBy { targetId -> Label.parse(targetId.id) }
}
