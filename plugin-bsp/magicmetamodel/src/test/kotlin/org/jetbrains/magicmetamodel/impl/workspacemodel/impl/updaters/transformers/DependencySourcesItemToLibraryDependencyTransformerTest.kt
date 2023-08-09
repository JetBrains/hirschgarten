package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.DependencySourcesItem
import ch.epfl.scala.bsp4j.JavacOptionsItem
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.jetbrains.magicmetamodel.impl.workspacemodel.LibraryDependency
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("DependencySourcesItemToLibraryDependencyTransformer.transformer(library) tests")
class DependencySourcesItemToLibraryDependencyTransformerTest {
  @Test
  fun `should return no library dependencies for no dependency sources`() {
    // given
    val emptyDependencySourcesItems = listOf<DependencySourcesAndJavacOptions>()

    // when
    val librariesDependencies =
      DependencySourcesItemToLibraryDependencyTransformer.transform(emptyDependencySourcesItems)

    // then
    librariesDependencies shouldBe emptyList()
  }

  @Test
  fun `should return single library dependency for dependency sources item with one dependency`() {
    // given
    val dependencySource = "file:///dependency/test/test-1.0.0-sources.jar"

    val dependencySourceItem = DependencySourcesAndJavacOptions(
      dependencySources = DependencySourcesItem(
        BuildTargetIdentifier("//target"),
        listOf(dependencySource),
      ),
      javacOptions = JavacOptionsItem(
        BuildTargetIdentifier("//target"),
        emptyList(),
        listOf("file:///dependency/test/test-1.0.0.jar"),
        "file:///compiler/output.jar",
      ),
    )

    // when
    val librariesDependencies = DependencySourcesItemToLibraryDependencyTransformer.transform(dependencySourceItem)

    // then
    val expectedLibraryDependency = LibraryDependency(
      libraryName = "BSP: file:///dependency/test/test-1.0.0.jar",
    )

    librariesDependencies shouldContainExactlyInAnyOrder listOf(expectedLibraryDependency)
  }

  @Test
  fun `should return multiple libraries dependencies for dependency sources item with multiple dependencies`() {
    // given
    val dependencySource1 = "file:///dependency/test1/test1-1.0.0-sources.jar"
    val dependencySource2 = "file:///dependency/test2/test2-1.0.0-sources.jar"
    val dependencySource3 = "file:///dependency/test3/test3-1.0.0-sources.jar"

    val dependencySourceItem = DependencySourcesAndJavacOptions(
      dependencySources = DependencySourcesItem(
        BuildTargetIdentifier("//target"),
        listOf(dependencySource1, dependencySource2, dependencySource3),
      ),
      javacOptions = JavacOptionsItem(
        BuildTargetIdentifier("//target"),
        emptyList(),
        listOf(
          "file:///dependency/test1/test1-1.0.0.jar",
          "file:///dependency/test2/test2-1.0.0.jar",
          "file:///dependency/test3/test3-1.0.0.jar",
        ),
        "file:///compiler/output.jar",
      ),
    )

    // when
    val librariesDependencies = DependencySourcesItemToLibraryDependencyTransformer.transform(dependencySourceItem)

    // then
    val expectedLibraryDependency1 = LibraryDependency(
      libraryName = "BSP: file:///dependency/test1/test1-1.0.0.jar",
    )
    val expectedLibraryDependency2 = LibraryDependency(
      libraryName = "BSP: file:///dependency/test2/test2-1.0.0.jar",
    )
    val expectedLibraryDependency3 = LibraryDependency(
      libraryName = "BSP: file:///dependency/test3/test3-1.0.0.jar",
    )

    librariesDependencies shouldContainExactlyInAnyOrder listOf(
      expectedLibraryDependency1,
      expectedLibraryDependency2,
      expectedLibraryDependency3,
    )
  }

  @Test
  fun `should return multiple libraries dependencies for multiple dependency sources items`() {
    // given
    val dependencySource1 = "file:///dependency/test1/test1-1.0.0-sources.jar"
    val dependencySource2 = "file:///dependency/test2/test2-1.0.0-sources.jar"
    val dependencySource3 = "file:///dependency/test3/test3-1.0.0-sources.jar"

    val dependencySourceItem1 = DependencySourcesAndJavacOptions(
      dependencySources = DependencySourcesItem(
        BuildTargetIdentifier("//target"),
        listOf(dependencySource1, dependencySource2),
      ),
      javacOptions = JavacOptionsItem(
        BuildTargetIdentifier("//target"),
        emptyList(),
        listOf(
          "file:///dependency/test1/test1-1.0.0.jar",
          "file:///dependency/test2/test2-1.0.0.jar",
        ),
        "file:///compiler/output.jar",
      ),
    )
    val dependencySourceItem2 = DependencySourcesAndJavacOptions(
      dependencySources = DependencySourcesItem(
        BuildTargetIdentifier("//target"),
        listOf(dependencySource2, dependencySource3),
      ),
      javacOptions = JavacOptionsItem(
        BuildTargetIdentifier("//target"),
        emptyList(),
        listOf(
          "file:///dependency/test2/test2-1.0.0.jar",
          "file:///dependency/test3/test3-1.0.0.jar",
        ),
        "file:///compiler/output.jar",
      ),
    )

    val dependencySourceItems = listOf(dependencySourceItem1, dependencySourceItem2)

    // when
    val librariesDependencies = DependencySourcesItemToLibraryDependencyTransformer.transform(dependencySourceItems)

    // then
    val expectedLibraryDependency1 = LibraryDependency(
      libraryName = "BSP: file:///dependency/test1/test1-1.0.0.jar",
    )
    val expectedLibraryDependency2 = LibraryDependency(
      libraryName = "BSP: file:///dependency/test2/test2-1.0.0.jar",
    )
    val expectedLibraryDependency3 = LibraryDependency(
      libraryName = "BSP: file:///dependency/test3/test3-1.0.0.jar",
    )

    librariesDependencies shouldContainExactlyInAnyOrder listOf(
      expectedLibraryDependency1,
      expectedLibraryDependency2,
      expectedLibraryDependency3,
    )
  }
}
