package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.DependencySourcesItem
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.LibraryDependency
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("DependencySourcesItemToLibraryDependencyTransformer.transformer(library) tests")
class DependencySourcesItemToLibraryDependencyTransformerTest {

  @Test
  fun `should return no library dependencies for no dependency sources`() {
    // given
    val emptyDependencySourcesItems = listOf<DependencySourcesItem>()

    // when
    val librariesDependencies =
      DependencySourcesItemToLibraryDependencyTransformer.transform(emptyDependencySourcesItems)

    // then
    librariesDependencies shouldBe emptyList()
  }

  @Test
  fun `should return single library dependency for dependency sources item with one dependency`() {
    // given
    val dependencySource = "file:///dependency/test/1.0.0/test-1.0.0-sources.jar"

    val dependencySourceItem = DependencySourcesItem(
      BuildTargetIdentifier("//target"),
      listOf(dependencySource)
    )

    // when
    val librariesDependencies = DependencySourcesItemToLibraryDependencyTransformer.transform(dependencySourceItem)

    // then
    val expectedLibraryDependency = LibraryDependency(
      libraryName = dependencySource,
    )

    librariesDependencies shouldContainExactlyInAnyOrder listOf(expectedLibraryDependency)
  }

  @Test
  fun `should return multiple libraries dependencies for dependency sources item with multiple dependencies`() {
    // given
    val dependencySource1 = "file:///dependency/test1/1.0.0/test1-1.0.0-sources.jar"
    val dependencySource2 = "file:///dependency/test2/1.0.0/test2-1.0.0-sources.jar"
    val dependencySource3 = "file:///dependency/test3/1.0.0/test3-1.0.0-sources.jar"

    val dependencySourceItem = DependencySourcesItem(
      BuildTargetIdentifier("//target"),
      listOf(dependencySource1, dependencySource2, dependencySource3)
    )

    // when
    val librariesDependencies = DependencySourcesItemToLibraryDependencyTransformer.transform(dependencySourceItem)

    // then
    val expectedLibraryDependency1 = LibraryDependency(
      libraryName = dependencySource1,
    )
    val expectedLibraryDependency2 = LibraryDependency(
      libraryName = dependencySource2,
    )
    val expectedLibraryDependency3 = LibraryDependency(
      libraryName = dependencySource3,
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
    val dependencySource1 = "file:///dependency/test1/1.0.0/test1-1.0.0-sources.jar"
    val dependencySource2 = "file:///dependency/test2/1.0.0/test2-1.0.0-sources.jar"
    val dependencySource3 = "file:///dependency/test3/1.0.0/test3-1.0.0-sources.jar"

    val dependencySourceItem1 = DependencySourcesItem(
      BuildTargetIdentifier("//target"),
      listOf(dependencySource1, dependencySource2)
    )
    val dependencySourceItem2 = DependencySourcesItem(
      BuildTargetIdentifier("//target"),
      listOf(dependencySource2, dependencySource3)
    )

    val dependencySourceItems = listOf(dependencySourceItem1, dependencySourceItem2)

    // when
    val librariesDependencies = DependencySourcesItemToLibraryDependencyTransformer.transform(dependencySourceItems)

    // then
    val expectedLibraryDependency1 = LibraryDependency(
      libraryName = dependencySource1,
    )
    val expectedLibraryDependency2 = LibraryDependency(
      libraryName = dependencySource2,
    )
    val expectedLibraryDependency3 = LibraryDependency(
      libraryName = dependencySource3,
    )

    librariesDependencies shouldContainExactlyInAnyOrder listOf(
      expectedLibraryDependency1,
      expectedLibraryDependency2,
      expectedLibraryDependency3,
    )
  }
}
