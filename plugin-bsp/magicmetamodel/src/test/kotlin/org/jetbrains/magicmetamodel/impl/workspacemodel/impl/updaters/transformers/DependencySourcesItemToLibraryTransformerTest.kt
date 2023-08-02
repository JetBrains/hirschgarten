package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.DependencySourcesItem
import ch.epfl.scala.bsp4j.JavacOptionsItem
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.jetbrains.magicmetamodel.impl.workspacemodel.Library
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("DependencySourcesItemToLibraryTransformer.transform(dependencySourcesItem) tests")
class DependencySourcesItemToLibraryTransformerTest {

  @Test
  fun `should return no libraries for no dependency sources items`() {
    // given
    val emptyDependencySourcesItems = listOf<DependencySourcesAndJavacOptions>()

    // when
    val libraries = DependencySourcesItemToLibraryTransformer.transform(emptyDependencySourcesItems)

    // then
    libraries shouldBe emptyList()
  }

  @Test
  fun `should return single library for dependency sources item with one dependency`() {
    // given
    val dependencySourcesAndJavacOptions =
      DependencySourcesAndJavacOptions(
        dependencySources = DependencySourcesItem(
          BuildTargetIdentifier("//target"),
          listOf("file:///m2/repo.maven.apache.org/test/1.0.0/test-1.0.0-sources.jar")
        ),
        javacOptions = JavacOptionsItem(
          BuildTargetIdentifier("//target"),
          listOf("opt1"),
          listOf("file:///m2/repo.maven.apache.org/test/1.0.0/test-1.0.0.jar"),
          "file:///compiler/output.jar"
        )
      )

    // when
    val libraries = DependencySourcesItemToLibraryTransformer.transform(dependencySourcesAndJavacOptions)

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
    val dependencySourcesAndJavacOptions =
      DependencySourcesAndJavacOptions(
        dependencySources = DependencySourcesItem(
          BuildTargetIdentifier("//target"),
          listOf(
            "file:///m2/repo.maven.apache.org/test2/2.0.0/test2-2.0.0-sources.jar",
            "file:///m2/repo.maven.apache.org/test3/3.0.0/test3-3.0.0-sources.jar",
          ),
        ),
        javacOptions = JavacOptionsItem(
          BuildTargetIdentifier("//target"),
          listOf(),
          listOf(
            "file:///m2/repo.maven.apache.org/test1/1.0.0/test1-1.0.0.jar",
            "file:///m2/repo.maven.apache.org/test2/2.0.0/test2-2.0.0.jar",
          ),
          "file:///compiler/output.jar"
        )
      )

    // when
    val libraries = DependencySourcesItemToLibraryTransformer.transform(dependencySourcesAndJavacOptions)

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
    val dependencySourcesAndJavacOptions1 =
      DependencySourcesAndJavacOptions(
        dependencySources = DependencySourcesItem(
          BuildTargetIdentifier("//target"),
          listOf(
            "file:///m2/repo.maven.apache.org/test2/2.0.0/test2-2.0.0-sources.jar",
          )
        ),
        javacOptions = JavacOptionsItem(
          BuildTargetIdentifier("//target"),
          listOf(),
          listOf(
            "file:///m2/repo.maven.apache.org/test2/2.0.0/test2-2.0.0.jar",
          ),
          "file:///compiler/output.jar"
        )
      )
    val dependencySourcesAndJavacOptions2 =
      DependencySourcesAndJavacOptions(
        dependencySources = DependencySourcesItem(
          BuildTargetIdentifier("//target"),
          listOf(
            "file:///m2/repo.maven.apache.org/test3/3.0.0/test3-3.0.0-sources.jar",
          )
        ),
        javacOptions = JavacOptionsItem(
          BuildTargetIdentifier("//target"),
          listOf(),
          listOf(
            "file:///m2/repo.maven.apache.org/test1/1.0.0/test1-1.0.0.jar",
          ),
          "file:///compiler/output1.jar"
        )

      )

    val dependencySourceItems = listOf(dependencySourcesAndJavacOptions1, dependencySourcesAndJavacOptions2)

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
