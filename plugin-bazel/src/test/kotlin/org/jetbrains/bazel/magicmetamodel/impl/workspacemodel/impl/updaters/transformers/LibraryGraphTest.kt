package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.LibraryItem
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import kotlin.io.path.Path

class LibraryGraphTest {
  @Nested
  inner class LibraryGraphTransitiveDependenciesTest {
    @Test
    fun `should return empty set for no libraries`() {
      // given
      val target =
        mockTarget(
          id = "target",
          dependencies = emptyList(),
        )
      val libraries = emptyList<LibraryItem>()
      val libraryGraph = LibraryGraph(libraries)

      // when
      val dependencies = libraryGraph.calculateAllDependencies(target, true)

      // then
      dependencies shouldBe emptyList()
    }

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
      val dependencies = libraryGraph.calculateAllDependencies(target, true)

      // then
      dependencies shouldBe
        listOf(
          Label.parse("lib1"),
          Label.parse("lib2"),
        )
    }

    @Test
    fun `should return all transitive dependencies`() {
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
      val dependencies = libraryGraph.calculateAllDependencies(target, true)

      // then
      dependencies shouldContainExactlyInAnyOrder
        listOf(
          Label.parse("lib1"),
          Label.parse("lib2"),
          Label.parse("lib3"),
          Label.parse("lib4"),
          Label.parse("lib5"),
          Label.parse("lib6"),
          Label.parse("lib7"),
        )
    }

    @Test
    fun `should return all transitive dependencies including workspace targets`() {
      // given
      val target =
        mockTarget(
          id = "target1",
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
            listOf("lib5", "lib6", "target2"),
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
            listOf("target3"),
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
      val dependencies = libraryGraph.calculateAllDependencies(target, true)

      // then
      dependencies shouldContainExactlyInAnyOrder
        listOf(
          Label.parse("lib1"),
          Label.parse("lib2"),
          Label.parse("lib3"),
          Label.parse("lib4"),
          Label.parse("lib5"),
          Label.parse("lib6"),
          Label.parse("lib7"),
          Label.parse("target2"),
          Label.parse("target3"),
        )
    }

    @Test
    @Timeout(value = 30, unit = java.util.concurrent.TimeUnit.SECONDS)
    fun `should return all transitive dependencies for complex graph in under 30 secs (old impl was failing)`() {
      // given
      val target =
        mockTarget(
          id = "target",
          dependencies = (1..1000).map { "lib$it" },
        )
      val libraries =
        (1..1000).map {
          mockLibraryItem(
            "lib$it",
            (it + 1..1000).map { depId -> "lib$depId" },
          )
        }
      val libraryGraph = LibraryGraph(libraries)

      // when
      val dependencies = libraryGraph.calculateAllDependencies(target, true)

      // then
      dependencies shouldContainExactlyInAnyOrder (1..1000).map { Label.parse("lib$it") }
    }
  }

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
      val dependencies = libraryGraph.calculateAllDependencies(target, false)

      // then
      dependencies shouldBe
        listOf(
          Label.parse("lib1"),
          Label.parse("lib2"),
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
      val dependencies = libraryGraph.calculateAllDependencies(target, false)

      // then
      dependencies shouldBe
        listOf(
          Label.parse("lib1"),
          Label.parse("lib2"),
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
      val dependencies = libraryGraph.calculateAllDependencies(target, false)

      // then
      dependencies shouldBe
        listOf(
          Label.parse("lib1"),
          Label.parse("target2"),
          Label.parse("lib2"),
          Label.parse("target3"),
        )
    }
  }
}

private fun mockTarget(id: String, dependencies: List<String>): RawBuildTarget =
  RawBuildTarget(
    Label.parse(id),
    emptyList(),
    dependencies.map { Label.parse(it) },
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
    dependencies = dependencies.map { Label.parse(it) },
    ijars = emptyList(),
    jars = emptyList(),
    sourceJars = emptyList(),
    mavenCoordinates = null,
    isFromInternalTarget = false,
    isLowPriority = false,
  )
