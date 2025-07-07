package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.JavaSourceRoot
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.jetbrains.bsp.protocol.SourceItem
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.io.path.Path

@DisplayName("SourcesItemToWorkspaceModelJavaSourceRootTransformer.transform(sourcesItem)")
class SourcesItemToJavaSourceRootTransformerTest {
  private val projectBasePath = Path("")
  private val projectBasePathURIStr = projectBasePath.toUri().toString()

  private val sourcesItemToJavaSourceRootTransformer = SourcesItemToJavaSourceRootTransformer()

  @Test
  fun `should return no sources roots for no sources items`() {
    // given
    val emptySources = listOf<RawBuildTarget>()

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
        path = Path("$rootDir/example/package/File.java"),
        generated = false,
        jvmPackagePrefix = "example.package",
      )

    val buildTargetAndSourceItem =
      RawBuildTarget(
        Label.parse("target"),
        listOf(),
        emptyList(),
        TargetKind(
          kindString = "java_binary",
          ruleType = RuleType.BINARY,
          languageClasses = setOf(LanguageClass.JAVA),
        ),
        listOf(sourceItem),
        emptyList(),
        baseDirectory = Path("base/dir"),
      )

    // when
    val javaSources = sourcesItemToJavaSourceRootTransformer.transform(buildTargetAndSourceItem)

    // then
    val expectedJavaSourceRoot =
      JavaSourceRoot(
        sourcePath = Path("$rootDir/example/package/File.java"),
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
        path = Path("$rootDir/example/package/File.java"),
        generated = false,
        jvmPackagePrefix = "example.package",
      )

    val buildTargetAndSourceItem =
      RawBuildTarget(
        Label.parse("target"),
        listOf(),
        emptyList(),
        TargetKind(
          kindString = "java_test",
          ruleType = RuleType.TEST,
          languageClasses = setOf(LanguageClass.JAVA),
        ),
        listOf(sourceItem),
        emptyList(),
        baseDirectory = Path("base/dir"),
      )

    // when
    val javaSources = sourcesItemToJavaSourceRootTransformer.transform(buildTargetAndSourceItem)

    // then
    val expectedJavaSourceRoot =
      JavaSourceRoot(
        sourcePath = Path("$rootDir/example/package/File.java"),
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
        path = Path("$rootDir/example/package/File1.java"),
        generated = false,
        jvmPackagePrefix = "example.package",
      )
    val sourceItem2 =
      SourceItem(
        path = Path("$rootDir/example/package/File2.java"),
        generated = false,
        jvmPackagePrefix = "example.package",
      )

    val buildTargetAndSourceItem =
      RawBuildTarget(
        Label.parse("target"),
        listOf(),
        emptyList(),
        TargetKind(
          kindString = "java_binary",
          ruleType = RuleType.BINARY,
          languageClasses = setOf(LanguageClass.JAVA),
        ),
        listOf(sourceItem1, sourceItem2),
        emptyList(),
        baseDirectory = Path("base/dir"),
      )

    // when
    val javaSources = sourcesItemToJavaSourceRootTransformer.transform(buildTargetAndSourceItem)

    // then
    val expectedJavaSourceRoot1 =
      JavaSourceRoot(
        sourcePath = Path("$rootDir/example/package/File1.java"),
        generated = false,
        packagePrefix = "example.package",
        rootType = SourceRootTypeId("java-source"),
      )
    val expectedJavaSourceRoot2 =
      JavaSourceRoot(
        sourcePath = Path("$rootDir/example/package/File2.java"),
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
        Path("$rootDir/example/package/File1.java"),
        false,
        jvmPackagePrefix = "example.package",
      )
    val sourceItem2 =
      SourceItem(
        Path("$rootDir/example/package/File2.java"),
        false,
        jvmPackagePrefix = "example.package",
      )

    val buildTargetAndSourceItem1 =
      RawBuildTarget(
        Label.parse("target"),
        listOf(),
        emptyList(),
        TargetKind(
          kindString = "java_binary",
          ruleType = RuleType.BINARY,
          languageClasses = setOf(LanguageClass.JAVA),
        ),
        listOf(sourceItem1),
        emptyList(),
        baseDirectory = Path("base/dir"),
      )
    val buildTargetAndSourceItem2 =
      RawBuildTarget(
        Label.parse("target"),
        listOf(),
        emptyList(),
        TargetKind(
          kindString = "java_binary",
          ruleType = RuleType.BINARY,
          languageClasses = setOf(LanguageClass.JAVA),
        ),
        listOf(sourceItem2),
        emptyList(),
        baseDirectory = Path("base/dir"),
      )

    val buildTargetAndSourceItems = listOf(buildTargetAndSourceItem1, buildTargetAndSourceItem2)

    // when
    val javaSources = sourcesItemToJavaSourceRootTransformer.transform(buildTargetAndSourceItems)

    // then
    val expectedJavaSourceRoot1 =
      JavaSourceRoot(
        sourcePath = Path("$rootDir/example/package/File1.java"),
        generated = false,
        packagePrefix = "example.package",
        rootType = SourceRootTypeId("java-source"),
      )
    val expectedJavaSourceRoot2 =
      JavaSourceRoot(
        sourcePath = Path("$rootDir/example/package/File2.java"),
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
    val anotherRootDir = Path("/var/tmp/another/root/dir")

    val sourceItem1 =
      SourceItem(
        path = Path("$rootDir/example/package/File1.java"),
        generated = false,
        jvmPackagePrefix = "example.package",
      )
    val sourceItem2 =
      SourceItem(
        path = Path("$anotherRootDir/example/package/File2.java"),
        generated = false,
        jvmPackagePrefix = "example.package",
      )

    val buildTargetAndSourceItem1 =
      RawBuildTarget(
        Label.parse("target"),
        listOf(),
        emptyList(),
        TargetKind(
          kindString = "java_binary",
          ruleType = RuleType.BINARY,
          languageClasses = setOf(LanguageClass.JAVA),
        ),
        listOf(sourceItem1),
        emptyList(),
        baseDirectory = Path("base/dir"),
      )

    val buildTargetAndSourceItem2 =
      RawBuildTarget(
        Label.parse("target"),
        listOf(),
        emptyList(),
        TargetKind(
          kindString = "java_binary",
          ruleType = RuleType.BINARY,
          languageClasses = setOf(LanguageClass.JAVA),
        ),
        listOf(sourceItem2),
        emptyList(),
        baseDirectory = Path("base/dir"),
      )

    val buildTargetAndSourceItems = listOf(buildTargetAndSourceItem1, buildTargetAndSourceItem2)

    // when
    val javaSources = sourcesItemToJavaSourceRootTransformer.transform(buildTargetAndSourceItems)

    // then
    val expectedJavaSourceRoot1 =
      JavaSourceRoot(
        sourcePath = Path("$rootDir/example/package/File1.java"),
        generated = false,
        packagePrefix = "example.package",
        rootType = SourceRootTypeId("java-source"),
      )
    val expectedJavaSourceRoot2 =
      JavaSourceRoot(
        sourcePath = Path("$anotherRootDir/example/package/File2.java"),
        generated = false,
        packagePrefix = "example.package",
        rootType = SourceRootTypeId("java-source"),
      )
    javaSources shouldContainExactlyInAnyOrder listOf(expectedJavaSourceRoot1, expectedJavaSourceRoot2)
  }
}
