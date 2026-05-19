package org.jetbrains.bazel.server.sync.projectResolver

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.TargetIdeInfo
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.TargetKey
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.Dependency
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
        b.toBuilder().depsBuilderList,
        rawTargetInfoMap,
      )
    processedDependencies shouldContainExactlyInAnyOrder listOf(dependency("@@//a-lib"))
  }

  private fun targetInfo(
    id: String,
    dependenciesIds: List<String>,
    kind: String,
  ): TargetIdeInfo =
    TargetIdeInfo
      .newBuilder()
      .setKey(TargetKey.newBuilder().setLabel(id).build())
      .addAllDeps(
        dependenciesIds.map { dependency(it) },
      ).setKind(kind)
      .build()

  private fun dependency(id: String): Dependency =
    Dependency
      .newBuilder()
      .setTarget(TargetKey.newBuilder().setLabel(id))
      .build()

  private fun toIdToTargetInfoMap(vararg targetIds: TargetIdeInfo): Map<Label, TargetIdeInfo> =
    targetIds.associateBy { targetId -> Label.parse(targetId.key.label) }
}
