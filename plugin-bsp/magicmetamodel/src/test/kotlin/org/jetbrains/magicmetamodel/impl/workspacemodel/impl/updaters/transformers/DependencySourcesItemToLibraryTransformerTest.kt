package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.DependencySourcesItem
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.Library
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("DependencySourcesItemToLibraryTransformer.transform(dependencySourcesItem) tests")
class DependencySourcesItemToLibraryTransformerTest {

  @Test
  fun `should return no libraries for no dependency sources items`() {
    // given
    val emptyDependencySourcesItems = listOf<DependencySourcesItem>()

    // when
    val libraries = DependencySourcesItemToLibraryTransformer.transform(emptyDependencySourcesItems)

    // then
    libraries shouldBe emptyList()
  }

  @Test
  fun `should return single library for dependency sources item with one dependency`() {
    // given
    val dependencySource = "file:///dependency/test/1.0.0/test-1.0.0-sources.jar"

    val dependencySourceItem = DependencySourcesItem(
      BuildTargetIdentifier("//target"),
      listOf(dependencySource)
    )

    // when
    val libraries = DependencySourcesItemToLibraryTransformer.transform(dependencySourceItem)

    // then
    val expectedLibrary = Library(
      displayName = dependencySource,
      jar = "jar:$dependencySource!/"
    )

    libraries shouldContainExactlyInAnyOrder listOf(expectedLibrary)
  }

  @Test
  fun `should return multiple libraries for dependency sources item with multiple dependencies`() {
    // given
    val dependencySource1 = "file:///dependency/test1/1.0.0/test1-1.0.0-sources.jar"
    val dependencySource2 = "file:///dependency/test2/1.0.0/test2-1.0.0-sources.jar"
    val dependencySource3 = "file:///dependency/test3/1.0.0/test3-1.0.0-sources.jar"

    val dependencySourceItem = DependencySourcesItem(
      BuildTargetIdentifier("//target"),
      listOf(dependencySource1, dependencySource2, dependencySource3)
    )

    // when
    val libraries = DependencySourcesItemToLibraryTransformer.transform(dependencySourceItem)

    // then
    val expectedLibrary1 = Library(
      displayName = dependencySource1,
      jar = "jar:$dependencySource1!/"
    )
    val expectedLibrary2 = Library(
      displayName = dependencySource2,
      jar = "jar:$dependencySource2!/"
    )
    val expectedLibrary3 = Library(
      displayName = dependencySource3,
      jar = "jar:$dependencySource3!/"
    )

    libraries shouldContainExactlyInAnyOrder listOf(expectedLibrary1, expectedLibrary2, expectedLibrary3)
  }

  @Test
  fun `should return multiple libraries for multiple dependency sources items`() {
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
    val libraries = DependencySourcesItemToLibraryTransformer.transform(dependencySourceItems)

    // then
    val expectedLibrary1 = Library(
      displayName = dependencySource1,
      jar = "jar:$dependencySource1!/"
    )
    val expectedLibrary2 = Library(
      displayName = dependencySource2,
      jar = "jar:$dependencySource2!/"
    )
    val expectedLibrary3 = Library(
      displayName = dependencySource3,
      jar = "jar:$dependencySource3!/"
    )

    libraries shouldContainExactlyInAnyOrder listOf(expectedLibrary1, expectedLibrary2, expectedLibrary3)
  }
}
