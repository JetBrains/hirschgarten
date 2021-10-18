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
    val dependencySourceItem = DependencySourcesItem(
      BuildTargetIdentifier("//target"),
      listOf("file:///dependency/test/1.0.0/test-1.0.0-sources.jar")
    )

    // when
    val libraries = DependencySourcesItemToLibraryTransformer.transform(dependencySourceItem)

    print(libraries)
    // then
    val expectedLibrary = Library(
      displayName = "file:///dependency/test/1.0.0/test-1.0.0-sources.jar",
      sourcesJar = "jar:///dependency/test/1.0.0/test-1.0.0-sources.jar!/",
      classesJar = "jar:///dependency/test/1.0.0/test-1.0.0.jar!/",
    )

    libraries shouldContainExactlyInAnyOrder listOf(expectedLibrary)
  }

  @Test
  fun `should return multiple libraries for dependency sources item with multiple dependencies`() {
    // given
    val dependencySourceItem = DependencySourcesItem(
      BuildTargetIdentifier("//target"),
      listOf(
        "file:///dependency/test1/1.0.0/test1-1.0.0-sources.jar",
        "file:///dependency/test2/2.0.0/test2-2.0.0-sources.jar",
        "file:///dependency/test3/3.0.0/test3-3.0.0-sources.jar",
      )
    )

    // when
    val libraries = DependencySourcesItemToLibraryTransformer.transform(dependencySourceItem)

    // then
    val expectedLibrary1 = Library(
      displayName = "file:///dependency/test1/1.0.0/test1-1.0.0-sources.jar",
      sourcesJar = "jar:///dependency/test1/1.0.0/test1-1.0.0-sources.jar!/",
      classesJar = "jar:///dependency/test1/1.0.0/test1-1.0.0.jar!/",
    )
    val expectedLibrary2 = Library(
      displayName = "file:///dependency/test2/2.0.0/test2-2.0.0-sources.jar",
      sourcesJar = "jar:///dependency/test2/2.0.0/test2-2.0.0-sources.jar!/",
      classesJar = "jar:///dependency/test2/2.0.0/test2-2.0.0.jar!/",
    )
    val expectedLibrary3 = Library(
      displayName = "file:///dependency/test3/3.0.0/test3-3.0.0-sources.jar",
      sourcesJar = "jar:///dependency/test3/3.0.0/test3-3.0.0-sources.jar!/",
      classesJar = "jar:///dependency/test3/3.0.0/test3-3.0.0.jar!/",
    )

    libraries shouldContainExactlyInAnyOrder listOf(expectedLibrary1, expectedLibrary2, expectedLibrary3)
  }

  @Test
  fun `should return multiple libraries for multiple dependency sources items`() {
    // given
    val dependencySourceItem1 = DependencySourcesItem(
      BuildTargetIdentifier("//target"),
      listOf(
        "file:///dependency/test1/1.0.0/test1-1.0.0-sources.jar",
        "file:///dependency/test2/2.0.0/test2-2.0.0-sources.jar",
      )
    )
    val dependencySourceItem2 = DependencySourcesItem(
      BuildTargetIdentifier("//target"),
      listOf(
        "file:///dependency/test2/2.0.0/test2-2.0.0-sources.jar",
        "file:///dependency/test3/3.0.0/test3-3.0.0-sources.jar",
      )
    )

    val dependencySourceItems = listOf(dependencySourceItem1, dependencySourceItem2)

    // when
    val libraries = DependencySourcesItemToLibraryTransformer.transform(dependencySourceItems)

    // then
    val expectedLibrary1 = Library(
      displayName = "file:///dependency/test1/1.0.0/test1-1.0.0-sources.jar",
      sourcesJar = "jar:///dependency/test1/1.0.0/test1-1.0.0-sources.jar!/",
      classesJar = "jar:///dependency/test1/1.0.0/test1-1.0.0.jar!/",
    )
    val expectedLibrary2 = Library(
      displayName = "file:///dependency/test2/2.0.0/test2-2.0.0-sources.jar",
      sourcesJar = "jar:///dependency/test2/2.0.0/test2-2.0.0-sources.jar!/",
      classesJar = "jar:///dependency/test2/2.0.0/test2-2.0.0.jar!/",
    )
    val expectedLibrary3 = Library(
      displayName = "file:///dependency/test3/3.0.0/test3-3.0.0-sources.jar",
      sourcesJar = "jar:///dependency/test3/3.0.0/test3-3.0.0-sources.jar!/",
      classesJar = "jar:///dependency/test3/3.0.0/test3-3.0.0.jar!/",
    )

    libraries shouldContainExactlyInAnyOrder listOf(expectedLibrary1, expectedLibrary2, expectedLibrary3)
  }
}
