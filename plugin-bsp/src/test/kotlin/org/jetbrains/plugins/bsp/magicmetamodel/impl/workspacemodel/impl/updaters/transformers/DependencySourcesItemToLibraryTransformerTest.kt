package org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.DependencySourcesItem
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.Library
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("DependencySourcesItemToLibraryTransformer.transform(dependencySourcesItem) tests")
class DependencySourcesItemToLibraryTransformerTest {
  @Test
  fun `should return no libraries for no dependency sources items`() {
    // given
    val emptyDependencySourcesItems = listOf<DependencySourcesAndJvmClassPaths>()

    // when
    val libraries = DependencySourcesItemToLibraryTransformer.transform(emptyDependencySourcesItems)

    // then
    libraries shouldBe emptyList()
  }

  @Test
  fun `should return single library for dependency sources item with one dependency`() {
    // given
    val dependencySourcesAndJvmClasspaths =
      DependencySourcesAndJvmClassPaths(
        dependencySources = DependencySourcesItem(
          BuildTargetIdentifier("//target"),
          listOf("file:///m2/repo.maven.apache.org/test/1.0.0/test-1.0.0-sources.jar"),
        ),
        listOf("file:///m2/repo.maven.apache.org/test/1.0.0/test-1.0.0.jar"),
      )

    // when
    val libraries = DependencySourcesItemToLibraryTransformer.transform(dependencySourcesAndJvmClasspaths)

    // then
    val expectedLibrary = Library(
      displayName = "BSP: file:///m2/repo.maven.apache.org/test/1.0.0/test-1.0.0.jar",
      sourceJars = listOf("jar:///m2/repo.maven.apache.org/test/1.0.0/test-1.0.0-sources.jar!/"),
      classJars = listOf("jar:///m2/repo.maven.apache.org/test/1.0.0/test-1.0.0.jar!/"),
    )
    libraries shouldContainExactlyInAnyOrder listOf(expectedLibrary)
  }

  @Test
  fun `should return multiple libraries for dependency sources item with multiple dependencies and multiple (including non-overlapping classes)`() {
    // given
    val dependencySourcesAndJvmClasspaths =
      DependencySourcesAndJvmClassPaths(
        dependencySources = DependencySourcesItem(
          BuildTargetIdentifier("//target"),
          listOf(
            "file:///m2/repo.maven.apache.org/test2/2.0.0/test2-2.0.0-sources.jar",
            "file:///m2/repo.maven.apache.org/test3/3.0.0/test3-3.0.0-sources.jar",
          ),
        ),
        listOf(
          "file:///m2/repo.maven.apache.org/test1/1.0.0/test1-1.0.0.jar",
          "file:///m2/repo.maven.apache.org/test2/2.0.0/test2-2.0.0.jar",
        ),
      )

    // when
    val libraries = DependencySourcesItemToLibraryTransformer.transform(dependencySourcesAndJvmClasspaths)

    // then
    val expectedLibrary1 = Library(
      displayName = "BSP: file:///m2/repo.maven.apache.org/test1/1.0.0/test1-1.0.0.jar",
      classJars = listOf("jar:///m2/repo.maven.apache.org/test1/1.0.0/test1-1.0.0.jar!/"),
    )
    val expectedLibrary2 = Library(
      displayName = "BSP: file:///m2/repo.maven.apache.org/test2/2.0.0/test2-2.0.0.jar",
      sourceJars = listOf("jar:///m2/repo.maven.apache.org/test2/2.0.0/test2-2.0.0-sources.jar!/"),
      classJars = listOf("jar:///m2/repo.maven.apache.org/test2/2.0.0/test2-2.0.0.jar!/"),
    )
    val expectedLibrary3 = Library(
      displayName = "BSP: file:///m2/repo.maven.apache.org/test3/3.0.0/test3-3.0.0-sources.jar",
      sourceJars = listOf("jar:///m2/repo.maven.apache.org/test3/3.0.0/test3-3.0.0-sources.jar!/"),
    )
    libraries shouldContainExactlyInAnyOrder listOf(expectedLibrary1, expectedLibrary2, expectedLibrary3)
  }

  @Test
  fun `should return multiple libraries for multiple dependency sources items`() {
    // given
    val dependencySourcesAndJvmClassPaths1 =
      DependencySourcesAndJvmClassPaths(
        dependencySources = DependencySourcesItem(
          BuildTargetIdentifier("//target"),
          listOf(
            "file:///m2/repo.maven.apache.org/test2/2.0.0/test2-2.0.0-sources.jar",
          ),
        ),
        listOf(
          "file:///m2/repo.maven.apache.org/test2/2.0.0/test2-2.0.0.jar",
        ),
      )
    val dependencySourcesAndJvmClassPaths2 =
      DependencySourcesAndJvmClassPaths(
        dependencySources = DependencySourcesItem(
          BuildTargetIdentifier("//target"),
          listOf(
            "file:///m2/repo.maven.apache.org/test3/3.0.0/test3-3.0.0-sources.jar",
          ),
        ),
        listOf(
          "file:///m2/repo.maven.apache.org/test1/1.0.0/test1-1.0.0.jar",
        ),
      )

    val dependencySourceItems = listOf(dependencySourcesAndJvmClassPaths1, dependencySourcesAndJvmClassPaths2)

    // when
    val libraries = DependencySourcesItemToLibraryTransformer.transform(dependencySourceItems)

    // then
    val expectedLibrary1 = Library(
      displayName = "BSP: file:///m2/repo.maven.apache.org/test1/1.0.0/test1-1.0.0.jar",
      classJars = listOf("jar:///m2/repo.maven.apache.org/test1/1.0.0/test1-1.0.0.jar!/"),
    )
    val expectedLibrary2 = Library(
      displayName = "BSP: file:///m2/repo.maven.apache.org/test2/2.0.0/test2-2.0.0.jar",
      sourceJars = listOf("jar:///m2/repo.maven.apache.org/test2/2.0.0/test2-2.0.0-sources.jar!/"),
      classJars = listOf("jar:///m2/repo.maven.apache.org/test2/2.0.0/test2-2.0.0.jar!/"),
    )
    val expectedLibrary3 = Library(
      displayName = "BSP: file:///m2/repo.maven.apache.org/test3/3.0.0/test3-3.0.0-sources.jar",
      sourceJars = listOf("jar:///m2/repo.maven.apache.org/test3/3.0.0/test3-3.0.0-sources.jar!/"),
    )
    libraries shouldContainExactlyInAnyOrder listOf(expectedLibrary1, expectedLibrary2, expectedLibrary3)
  }
}
