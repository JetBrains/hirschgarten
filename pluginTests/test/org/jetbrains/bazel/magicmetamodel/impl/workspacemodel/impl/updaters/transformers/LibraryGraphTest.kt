package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.DependencyLabel
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.LibraryItem
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.io.path.Path

class LibraryGraphTest {
  @Nested
  inner class LibraryGraphDirectDependenciesTest {
    @Test
    fun `should return direct libraries if libraries dont have dependencies`() {
      // given
      val target =
        mockTarget(
          id = "target",
          dependencies = listOf("lib1", "lib2"),
        )
      val libraries =
        listOf(
          mockLibraryItem(
            "lib1",
            emptyList(),
          ),
          mockLibraryItem(
            "lib2",
            emptyList(),
          ),
        )
      val libraryGraph = LibraryGraph(libraries)

      // when
      val dependencies = libraryGraph.calculateAllDependencies(target)

      // then
      dependencies shouldBe
        listOf(
          DependencyLabel.parse("lib1"),
          DependencyLabel.parse("lib2"),
        )
    }

    @Test
    fun `should return all direct dependencies`() {
      // given
      val target =
        mockTarget(
          id = "target",
          dependencies = listOf("lib1", "lib2"),
        )
      val libraries =
        listOf(
          mockLibraryItem(
            "lib1",
            listOf("lib3"),
          ),
          mockLibraryItem(
            "lib2",
            listOf("lib4"),
          ),
          mockLibraryItem(
            "lib3",
            listOf("lib5", "lib6"),
          ),
          mockLibraryItem(
            "lib4",
            emptyList(),
          ),
          mockLibraryItem(
            "lib5",
            listOf("lib7"),
          ),
          mockLibraryItem(
            "lib6",
            emptyList(),
          ),
          mockLibraryItem(
            "lib7",
            emptyList(),
          ),
          mockLibraryItem(
            "not_included_lib_1",
            listOf("not_included_lib_2"),
          ),
          mockLibraryItem(
            "not_included_lib_2",
            emptyList(),
          ),
        )
      val libraryGraph = LibraryGraph(libraries)

      // when
      val dependencies = libraryGraph.calculateAllDependencies(target)

      // then
      dependencies shouldBe
        listOf(
          DependencyLabel.parse("lib1"),
          DependencyLabel.parse("lib2"),
        )
    }

    @Test
    fun `should return all direct dependencies including workspace targets`() {
      // given
      val target =
        mockTarget(
          id = "target1",
          dependencies = listOf("lib1", "target2", "lib2", "target3"),
        )
      val libraries =
        listOf(
          mockLibraryItem(
            "lib1",
            listOf("lib3"),
          ),
          mockLibraryItem(
            "lib2",
            listOf("lib4"),
          ),
          mockLibraryItem(
            "lib3",
            listOf("lib5", "lib6"),
          ),
          mockLibraryItem(
            "lib4",
            emptyList(),
          ),
          mockLibraryItem(
            "lib5",
            listOf("lib7"),
          ),
          mockLibraryItem(
            "not_included_lib_1",
            listOf("not_included_lib_2"),
          ),
          mockLibraryItem(
            "not_included_lib_2",
            emptyList(),
          ),
        )
      val libraryGraph = LibraryGraph(libraries)

      // when
      val dependencies = libraryGraph.calculateAllDependencies(target)

      // then
      dependencies shouldBe
        listOf(
          DependencyLabel.parse("lib1"),
          DependencyLabel.parse("target2"),
          DependencyLabel.parse("lib2"),
          DependencyLabel.parse("target3"),
        )
    }
  }
}

private fun mockTarget(id: String, dependencies: List<String>): RawBuildTarget =
  RawBuildTarget(
    Label.parse(id),
    emptyList(),
    dependencies.map { DependencyLabel.parse(it) },
    TargetKind(
      kindString = "java_binary",
      ruleType = RuleType.BINARY,
      languageClasses = setOf(),
    ),
    emptyList(),
    emptyList(),
    Path("base/dir"),
  )

private fun mockLibraryItem(id: String, dependencies: List<String>): LibraryItem =
  LibraryItem(
    id = Label.parse(id),
    dependencies = dependencies.map { DependencyLabel.parse(it) },
    ijars = emptyList(),
    jars = emptyList(),
    sourceJars = emptyList(),
    mavenCoordinates = null,
    containsInternalJars = false,
    isLowPriority = false,
  )
