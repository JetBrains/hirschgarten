package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.workspacemodel.entities.JavaSourceRoot
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetCapabilities
import org.jetbrains.bsp.protocol.SourceItem
import org.jetbrains.bsp.protocol.SourcesItem
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.net.URI
import kotlin.io.path.Path
import kotlin.io.path.toPath

@DisplayName("SourcesItemToWorkspaceModelJavaSourceRootTransformer.transform(sourcesItem)")
class SourcesItemToJavaSourceRootTransformerTest {
  private val projectBasePath = Path("")
  private val projectBasePathURIStr = projectBasePath.toUri().toString()

  private val sourcesItemToJavaSourceRootTransformer = SourcesItemToJavaSourceRootTransformer()

  @Test
  fun `should return no sources roots for no sources items`() {
    // given
    val emptySources = listOf<BuildTargetAndSourceItem>()

    // when
    val javaSources = sourcesItemToJavaSourceRootTransformer.transform(emptySources)

    // then
    javaSources shouldBe emptyList()
  }

  @Test
  fun `should return single source root for sources item with one file source`() {
    // given
    val rootDir = "${projectBasePathURIStr}root/dir"
    val sourceItem =
      SourceItem(
        uri = "$rootDir/example/package/File.java",
        generated = false,
        jvmPackagePrefix = "example.package",
      )

    val buildTargetAndSourceItem =
      BuildTargetAndSourceItem(
        buildTarget =
          BuildTarget(
            Label.parse("target"),
            listOf("library"),
            listOf("java"),
            emptyList(),
            BuildTargetCapabilities(),
          ),
        sourcesItem =
          SourcesItem(
            Label.parse("target"),
            listOf(sourceItem),
          ),
      )

    // when
    val javaSources = sourcesItemToJavaSourceRootTransformer.transform(buildTargetAndSourceItem)

    // then
    val expectedJavaSourceRoot =
      JavaSourceRoot(
        sourcePath = URI.create("$rootDir/example/package/File.java").toPath(),
        generated = false,
        packagePrefix = "example.package",
        rootType = SourceRootTypeId("java-source"),
      )

    javaSources shouldContainExactlyInAnyOrder listOf(expectedJavaSourceRoot)
  }

  @Test
  fun `should return single source test root for sources item with one file source`() {
    // given
    val rootDir = "${projectBasePathURIStr}root/dir"
    val sourceItem =
      SourceItem(
        uri = "$rootDir/example/package/File.java",
        generated = false,
        jvmPackagePrefix = "example.package",
      )

    val buildTargetAndSourceItem =
      BuildTargetAndSourceItem(
        buildTarget =
          BuildTarget(
            Label.parse("target"),
            listOf("test"),
            listOf("java"),
            emptyList(),
            BuildTargetCapabilities(),
          ),
        sourcesItem =
          SourcesItem(
            Label.parse("target"),
            listOf(sourceItem),
          ),
      )

    // when
    val javaSources = sourcesItemToJavaSourceRootTransformer.transform(buildTargetAndSourceItem)

    // then
    val expectedJavaSourceRoot =
      JavaSourceRoot(
        sourcePath = URI.create("$rootDir/example/package/File.java").toPath(),
        generated = false,
        packagePrefix = "example.package",
        rootType = SourceRootTypeId("java-test"),
      )

    javaSources shouldContainExactlyInAnyOrder listOf(expectedJavaSourceRoot)
  }

  @Test
  fun `should return sources roots for sources item with multiple sources`() {
    // given
    val rootDir = "${projectBasePathURIStr}root/dir"
    val anotherRootDir = "${projectBasePathURIStr}another/root/dir"

    val sourceItem1 =
      SourceItem(
        uri = "$rootDir/example/package/File1.java",
        generated = false,
        jvmPackagePrefix = "example.package",
      )
    val sourceItem2 =
      SourceItem(
        uri = "$rootDir/example/package/File2.java",
        generated = false,
        jvmPackagePrefix = "example.package",
      )

    val buildTargetAndSourceItem =
      BuildTargetAndSourceItem(
        buildTarget =
          BuildTarget(
            Label.parse("target"),
            listOf("library"),
            listOf("java"),
            emptyList(),
            BuildTargetCapabilities(),
          ),
        sourcesItem =
          SourcesItem(
            Label.parse("target"),
            listOf(sourceItem1, sourceItem2),
          ),
      )

    // when
    val javaSources = sourcesItemToJavaSourceRootTransformer.transform(buildTargetAndSourceItem)

    // then
    val expectedJavaSourceRoot1 =
      JavaSourceRoot(
        sourcePath = URI.create("$rootDir/example/package/File1.java").toPath(),
        generated = false,
        packagePrefix = "example.package",
        rootType = SourceRootTypeId("java-source"),
      )
    val expectedJavaSourceRoot2 =
      JavaSourceRoot(
        sourcePath = URI.create("$rootDir/example/package/File2.java").toPath(),
        generated = false,
        packagePrefix = "example.package",
        rootType = SourceRootTypeId("java-source"),
      )

    javaSources shouldContainExactlyInAnyOrder listOf(expectedJavaSourceRoot1, expectedJavaSourceRoot2)
  }

  @Test
  fun `should return sources roots for multiple sources items`() {
    // given
    val rootDir = "${projectBasePathURIStr}root/dir"
    val anotherRootDir = "${projectBasePathURIStr}another/root/dir"

    val sourceItem1 =
      SourceItem(
        "$rootDir/example/package/File1.java",
        false,
        jvmPackagePrefix = "example.package",
      )
    val sourceItem2 =
      SourceItem(
        "$rootDir/example/package/File2.java",
        false,
        jvmPackagePrefix = "example.package",
      )

    val buildTargetAndSourceItem1 =
      BuildTargetAndSourceItem(
        buildTarget =
          BuildTarget(
            Label.parse("target"),
            listOf("library"),
            listOf("java"),
            emptyList(),
            BuildTargetCapabilities(),
          ),
        sourcesItem =
          SourcesItem(
            Label.parse("target"),
            listOf(sourceItem1),
          ),
      )
    val buildTargetAndSourceItem2 =
      BuildTargetAndSourceItem(
        buildTarget =
          BuildTarget(
            Label.parse("target"),
            listOf("library"),
            listOf("java"),
            emptyList(),
            BuildTargetCapabilities(),
          ),
        sourcesItem =
          SourcesItem(
            Label.parse("target"),
            listOf(sourceItem2),
          ),
      )

    val buildTargetAndSourceItems = listOf(buildTargetAndSourceItem1, buildTargetAndSourceItem2)

    // when
    val javaSources = sourcesItemToJavaSourceRootTransformer.transform(buildTargetAndSourceItems)

    // then
    val expectedJavaSourceRoot1 =
      JavaSourceRoot(
        sourcePath = URI.create("$rootDir/example/package/File1.java").toPath(),
        generated = false,
        packagePrefix = "example.package",
        rootType = SourceRootTypeId("java-source"),
      )
    val expectedJavaSourceRoot2 =
      JavaSourceRoot(
        sourcePath = URI.create("$rootDir/example/package/File2.java").toPath(),
        generated = false,
        packagePrefix = "example.package",
        rootType = SourceRootTypeId("java-source"),
      )
    javaSources shouldContainExactlyInAnyOrder listOf(expectedJavaSourceRoot1, expectedJavaSourceRoot2)
  }

  @Test
  fun `should return source roots regardless they have source items in project base path or not`() {
    // given
    val rootDir = "${projectBasePathURIStr}root/dir"
    val anotherRootDir = "file:///var/tmp/another/root/dir"

    val sourceItem1 =
      SourceItem(
        uri = "$rootDir/example/package/File1.java",
        generated = false,
        jvmPackagePrefix = "example.package",
      )
    val sourceItem2 =
      SourceItem(
        uri = "$anotherRootDir/example/package/File2.java",
        generated = false,
        jvmPackagePrefix = "example.package",
      )

    val buildTargetAndSourceItem1 =
      BuildTargetAndSourceItem(
        buildTarget =
          BuildTarget(
            Label.parse("target"),
            listOf("library"),
            listOf("java"),
            emptyList(),
            BuildTargetCapabilities(),
          ),
        sourcesItem =
          SourcesItem(
            Label.parse("target"),
            listOf(sourceItem1),
          ),
      )
    val buildTargetAndSourceItem2 =
      BuildTargetAndSourceItem(
        buildTarget =
          BuildTarget(
            Label.parse("target"),
            listOf("library"),
            listOf("java"),
            emptyList(),
            BuildTargetCapabilities(),
          ),
        sourcesItem =
          SourcesItem(
            Label.parse("target"),
            listOf(sourceItem2),
          ),
      )

    val buildTargetAndSourceItems = listOf(buildTargetAndSourceItem1, buildTargetAndSourceItem2)

    // when
    val javaSources = sourcesItemToJavaSourceRootTransformer.transform(buildTargetAndSourceItems)

    // then
    val expectedJavaSourceRoot1 =
      JavaSourceRoot(
        sourcePath = URI.create("$rootDir/example/package/File1.java").toPath(),
        generated = false,
        packagePrefix = "example.package",
        rootType = SourceRootTypeId("java-source"),
      )
    val expectedJavaSourceRoot2 =
      JavaSourceRoot(
        sourcePath = URI.create("$anotherRootDir/example/package/File2.java").toPath(),
        generated = false,
        packagePrefix = "example.package",
        rootType = SourceRootTypeId("java-source"),
      )
    javaSources shouldContainExactlyInAnyOrder listOf(expectedJavaSourceRoot1, expectedJavaSourceRoot2)
  }
}
