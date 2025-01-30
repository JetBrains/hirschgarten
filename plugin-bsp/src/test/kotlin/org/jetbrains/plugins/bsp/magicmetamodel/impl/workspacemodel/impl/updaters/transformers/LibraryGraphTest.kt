package org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetCapabilities
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import io.kotest.matchers.shouldBe
import org.jetbrains.bsp.protocol.Dependency
import org.jetbrains.bsp.protocol.LibraryItem
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout

class LibraryGraphTest {
  // TODO: remove LibraryGraphTransitiveDependenciesTest after dropping support for 2024.3
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
      val libraryGraph = LibraryGraph(libraries, true)

      // when
      val dependencies = libraryGraph.calculateAllDependencies(target, null)

      // then
      dependencies.libraryDependencies shouldBe emptySet()
      dependencies.moduleDependencies shouldBe emptySet()
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
      val libraryGraph = LibraryGraph(libraries, true)

      // when
      val dependencies = libraryGraph.calculateAllDependencies(target, null)

      // then
      dependencies.libraryDependencies shouldBe
        setOfExportedDependencies(
          BuildTargetIdentifier("lib1"),
          BuildTargetIdentifier("lib2"),
        )
      dependencies.moduleDependencies shouldBe emptySet()
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
            "not included lib 1",
            listOf("not included lib 2"),
          ),
          mockLibraryItem(
            "not included lib 2",
            emptyList(),
          ),
        )
      val libraryGraph = LibraryGraph(libraries, true)

      // when
      val dependencies = libraryGraph.calculateAllDependencies(target, null)

      // then
      dependencies.libraryDependencies shouldBe
        setOfExportedDependencies(
          BuildTargetIdentifier("lib1"),
          BuildTargetIdentifier("lib2"),
          BuildTargetIdentifier("lib3"),
          BuildTargetIdentifier("lib4"),
          BuildTargetIdentifier("lib5"),
          BuildTargetIdentifier("lib6"),
          BuildTargetIdentifier("lib7"),
        )
      dependencies.moduleDependencies shouldBe emptySet()
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
            "not included lib 1",
            listOf("not included lib 2"),
          ),
          mockLibraryItem(
            "not included lib 2",
            emptyList(),
          ),
        )
      val libraryGraph = LibraryGraph(libraries, true)

      // when
      val dependencies = libraryGraph.calculateAllDependencies(target, null)

      // then
      dependencies.libraryDependencies shouldBe
        setOfExportedDependencies(
          BuildTargetIdentifier("lib1"),
          BuildTargetIdentifier("lib2"),
          BuildTargetIdentifier("lib3"),
          BuildTargetIdentifier("lib4"),
          BuildTargetIdentifier("lib5"),
          BuildTargetIdentifier("lib6"),
          BuildTargetIdentifier("lib7"),
        )
      dependencies.moduleDependencies shouldBe
        setOfExportedDependencies(
          BuildTargetIdentifier("target2"),
          BuildTargetIdentifier("target3"),
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
      val libraryGraph = LibraryGraph(libraries, true)

      // when
      val dependencies = libraryGraph.calculateAllDependencies(target, null)

      // then
      dependencies.libraryDependencies shouldBe (1..1000).map { Dependency(BuildTargetIdentifier("lib$it"), exported = true) }
      dependencies.moduleDependencies shouldBe emptySet()
    }
  }

  @Nested
  inner class LibraryGraphDirectDependenciesTest {
    @Test
    fun `should return empty set for no libraries`() {
      // given
      val target =
        mockTarget(
          id = "target",
          dependencies = emptyList(),
        )
      val libraries = emptyList<LibraryItem>()
      val libraryGraph = LibraryGraph(libraries, false)

      // when
      val dependencies = libraryGraph.calculateAllDependencies(target, null)

      // then
      dependencies.libraryDependencies shouldBe emptySet()
      dependencies.moduleDependencies shouldBe emptySet()
    }

    @Test
    fun `should return direct libraries if libraries dont have dependencies`() {
      // given
      val target =
        mockTarget(
          id = "target",
          dependencies = listOf("lib1", "lib2"),
        )
      val dependenciesExported = listOf(true, false)
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
      val libraryGraph = LibraryGraph(libraries, false)

      // when
      val dependencies = libraryGraph.calculateAllDependencies(target, dependenciesExported)

      // then
      dependencies.libraryDependencies shouldBe
        setOf(
          Dependency(BuildTargetIdentifier("lib1"), exported = true),
          Dependency(BuildTargetIdentifier("lib2"), exported = false),
        )
      dependencies.moduleDependencies shouldBe emptySet()
    }

    @Test
    fun `should set all dependencies as exported if dependenciesExported response is empty`() {
      // given
      val target =
        mockTarget(
          id = "target",
          dependencies = listOf("lib1", "lib2"),
        )
      val dependenciesExported = null
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
      val libraryGraph = LibraryGraph(libraries, false)

      // when
      val dependencies = libraryGraph.calculateAllDependencies(target, dependenciesExported)

      // then
      dependencies.libraryDependencies shouldBe
        setOf(
          Dependency(BuildTargetIdentifier("lib1"), exported = true),
          Dependency(BuildTargetIdentifier("lib2"), exported = true),
        )
      dependencies.moduleDependencies shouldBe emptySet()
    }

    @Test
    fun `should return all direct dependencies`() {
      // given
      val target =
        mockTarget(
          id = "target",
          dependencies = listOf("lib1", "lib2"),
        )
      val dependenciesExported = listOf(false, true)
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
            "not included lib 1",
            listOf("not included lib 2"),
          ),
          mockLibraryItem(
            "not included lib 2",
            emptyList(),
          ),
        )
      val libraryGraph = LibraryGraph(libraries, false)

      // when
      val dependencies = libraryGraph.calculateAllDependencies(target, dependenciesExported)

      // then
      dependencies.libraryDependencies shouldBe
        setOf(
          Dependency(BuildTargetIdentifier("lib1"), exported = false),
          Dependency(BuildTargetIdentifier("lib2"), exported = true),
        )
      dependencies.moduleDependencies shouldBe emptySet()
    }

    @Test
    fun `should return all direct dependencies including workspace targets`() {
      // given
      val target =
        mockTarget(
          id = "target1",
          dependencies = listOf("lib1", "lib2", "target2", "target3"),
        )
      val dependenciesExported = listOf(true, false, true, false)
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
            "not included lib 1",
            listOf("not included lib 2"),
          ),
          mockLibraryItem(
            "not included lib 2",
            emptyList(),
          ),
        )
      val libraryGraph = LibraryGraph(libraries, false)

      // when
      val dependencies = libraryGraph.calculateAllDependencies(target, dependenciesExported)

      // then
      dependencies.libraryDependencies shouldBe
        setOf(
          Dependency(BuildTargetIdentifier("lib1"), exported = true),
          Dependency(BuildTargetIdentifier("lib2"), exported = false),
        )
      dependencies.moduleDependencies shouldBe
        setOf(
          Dependency(BuildTargetIdentifier("target2"), exported = true),
          Dependency(BuildTargetIdentifier("target3"), exported = false),
        )
    }
  }
}

private fun setOfExportedDependencies(vararg elements: BuildTargetIdentifier): Set<Dependency> =
  elements
    .map {
      Dependency(it, exported = true)
    }.toSet()

private fun mockTarget(id: String, dependencies: List<String>): BuildTarget =
  BuildTarget(
    BuildTargetIdentifier(id),
    emptyList(),
    emptyList(),
    dependencies.map { BuildTargetIdentifier(it) },
    BuildTargetCapabilities(),
  )

private fun mockLibraryItem(id: String, dependencies: List<String>): LibraryItem =
  LibraryItem(
    id = BuildTargetIdentifier(id),
    dependencies = dependencies.map { Dependency(BuildTargetIdentifier(it), exported = false) },
    ijars = emptyList(),
    jars = emptyList(),
    sourceJars = emptyList(),
    mavenCoordinates = null,
  )
