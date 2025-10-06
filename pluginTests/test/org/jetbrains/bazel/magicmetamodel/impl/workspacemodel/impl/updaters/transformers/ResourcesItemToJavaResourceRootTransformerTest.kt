package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.ResourceRoot
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.file.Files
import kotlin.io.path.Path

@DisplayName("ResourcesItemToJavaResourceRootTransformer.transform(resourcesItem) tests")
class ResourcesItemToJavaResourceRootTransformerTest {
  private val projectBasePath = Path("").toAbsolutePath()

  private val resourcesItemToJavaResourceRootTransformer = ResourcesItemToJavaResourceRootTransformer()

  @Test
  fun `should return no resources roots for no resources items`() {
    // given
    val emptyResources = listOf<RawBuildTarget>()

    // when
    val javaResources = resourcesItemToJavaResourceRootTransformer.transform(emptyResources)

    // then
    javaResources shouldBe emptyList()
  }

  @Test
  fun `should return single resource root for resources item with one file path`() {
    // given
    val resourceFilePath = Files.createTempFile(projectBasePath, "resource", "File.txt")
    resourceFilePath.toFile().deleteOnExit()

    val buildTarget =
      RawBuildTarget(
        Label.parse("//target"),
        emptyList(),
        listOf(),
        TargetKind(
          kindString = "java_binary",
          ruleType = RuleType.BINARY,
          languageClasses = setOf(LanguageClass.JAVA),
        ),
        emptyList(),
        listOf(resourceFilePath),
        baseDirectory = Path("base/dir"),
      )

    // when
    val javaResources = resourcesItemToJavaResourceRootTransformer.transform(buildTarget)

    // then
    val expectedJavaResource =
      ResourceRoot(
        resourcePath = resourceFilePath,
        rootType = SourceRootTypeId("java-resource"),
      )

    javaResources shouldContainExactlyInAnyOrder listOf(expectedJavaResource)
  }

  @Test
  fun `should return resource root with type test for resources item coming from a build target having test target kind`() {
    // given
    val resourceFilePath = Files.createTempFile(projectBasePath, "resource", "File.txt")
    resourceFilePath.toFile().deleteOnExit()

    val buildTarget =
      RawBuildTarget(
        Label.parse("//target"),
        listOf(),
        listOf(),
        TargetKind(
          kindString = "java_test",
          ruleType = RuleType.TEST,
          languageClasses = setOf(LanguageClass.JAVA),
        ),
        emptyList(),
        listOf(resourceFilePath),
        baseDirectory = Path("base/dir"),
      )

    // when
    val javaResources = resourcesItemToJavaResourceRootTransformer.transform(buildTarget)

    // then
    val expectedJavaResource =
      ResourceRoot(
        resourcePath = resourceFilePath,
        rootType = SourceRootTypeId("java-test-resource"),
      )

    javaResources shouldContainExactlyInAnyOrder listOf(expectedJavaResource)
  }

  @Test
  fun `should return single resource root for resources item with one dir path`() {
    // given
    val resourceDirPath = Files.createTempDirectory(projectBasePath, "resource")
    resourceDirPath.toFile().deleteOnExit()

    val buildTarget =
      RawBuildTarget(
        Label.parse("//target"),
        emptyList(),
        listOf(),
        TargetKind(
          kindString = "java_binary",
          ruleType = RuleType.BINARY,
          languageClasses = setOf(LanguageClass.JAVA),
        ),
        emptyList(),
        listOf(resourceDirPath),
        baseDirectory = Path("base/dir"),
      )

    // when
    val javaResources = resourcesItemToJavaResourceRootTransformer.transform(buildTarget)

    // then
    val expectedJavaResource =
      ResourceRoot(
        resourcePath = resourceDirPath,
        rootType = SourceRootTypeId("java-resource"),
      )

    javaResources shouldContainExactlyInAnyOrder listOf(expectedJavaResource)
  }

  @Test
  fun `should return multiple resource roots for resources item with multiple paths with the same directories`() {
    // given

    val resourceFilePath1 = Files.createTempFile(projectBasePath, "resource", "File1.txt")
    resourceFilePath1.toFile().deleteOnExit()
    val resourceFilePath2 = Files.createTempFile(projectBasePath, "resource", "File2.txt")
    resourceFilePath2.toFile().deleteOnExit()
    val resourceFilePath3 = Files.createTempFile(projectBasePath, "resource", "File3.txt")
    resourceFilePath3.toFile().deleteOnExit()

    val buildTarget =
      RawBuildTarget(
        Label.parse("//target"),
        emptyList(),
        listOf(),
        TargetKind(
          kindString = "java_binary",
          ruleType = RuleType.BINARY,
          languageClasses = setOf(LanguageClass.JAVA),
        ),
        emptyList(),
        listOf(resourceFilePath1, resourceFilePath2, resourceFilePath3),
        baseDirectory = Path("base/dir"),
      )

    // when
    val javaResources = resourcesItemToJavaResourceRootTransformer.transform(buildTarget)

    // then
    val expectedJavaResource1 =
      ResourceRoot(
        resourcePath = resourceFilePath1,
        rootType = SourceRootTypeId("java-resource"),
      )

    val expectedJavaResource2 =
      ResourceRoot(
        resourcePath = resourceFilePath2,
        rootType = SourceRootTypeId("java-resource"),
      )

    val expectedJavaResource3 =
      ResourceRoot(
        resourcePath = resourceFilePath3,
        rootType = SourceRootTypeId("java-resource"),
      )

    val expectedJavaResources = listOf(expectedJavaResource1, expectedJavaResource2, expectedJavaResource3)

    javaResources shouldContainExactlyInAnyOrder expectedJavaResources
  }

  @Test
  fun `should return multiple resource roots for resources item with multiple paths`() {
    // given
    val resourceFilePath = Files.createTempFile(projectBasePath, "resource", "File1.txt")
    resourceFilePath.toFile().deleteOnExit()

    val resourceDirPath = Files.createTempDirectory(projectBasePath, "resourcedir")
    resourceDirPath.toFile().deleteOnExit()

    val buildTarget =
      RawBuildTarget(
        Label.parse("//target"),
        emptyList(),
        listOf(),
        TargetKind(
          kindString = "java_binary",
          ruleType = RuleType.BINARY,
          languageClasses = setOf(LanguageClass.JAVA),
        ),
        emptyList(),
        listOf(resourceFilePath, resourceDirPath),
        baseDirectory = Path("base/dir"),
      )

    // when
    val javaResources = resourcesItemToJavaResourceRootTransformer.transform(buildTarget)

    // then
    val expectedJavaResource1 =
      ResourceRoot(
        resourcePath = resourceFilePath,
        rootType = SourceRootTypeId("java-resource"),
      )
    val expectedJavaResource2 =
      ResourceRoot(
        resourcePath = resourceDirPath,
        rootType = SourceRootTypeId("java-resource"),
      )

    javaResources shouldContainExactlyInAnyOrder listOf(expectedJavaResource1, expectedJavaResource2)
  }

  @Test
  fun `should return multiple resource roots for multiple resources items`() {
    // given

    val resourceFilePath1 = Files.createTempFile(projectBasePath, "resource", "File1.txt")
    resourceFilePath1.toFile().deleteOnExit()

    val resourceFilePath2 = Files.createTempFile(projectBasePath, "resource", "File2.txt")
    resourceFilePath2.toFile().deleteOnExit()

    val resourceDirPath3 = Files.createTempDirectory(projectBasePath, "resourcedir")
    resourceDirPath3.toFile().deleteOnExit()

    val buildTarget =
      RawBuildTarget(
        Label.parse("//target"),
        emptyList(),
        listOf(),
        TargetKind(
          kindString = "java_binary",
          ruleType = RuleType.BINARY,
          languageClasses = setOf(LanguageClass.JAVA),
        ),
        emptyList(),
        listOf(resourceFilePath1, resourceFilePath2, resourceDirPath3),
        baseDirectory = Path("base/dir"),
      )

    // when
    val javaResources = resourcesItemToJavaResourceRootTransformer.transform(buildTarget)

    // then
    val expectedJavaResource1 =
      ResourceRoot(
        resourcePath = resourceFilePath1,
        rootType = SourceRootTypeId("java-resource"),
      )
    val expectedJavaResource2 =
      ResourceRoot(
        resourcePath = resourceFilePath2,
        rootType = SourceRootTypeId("java-resource"),
      )
    val expectedJavaResource3 =
      ResourceRoot(
        resourcePath = resourceDirPath3,
        rootType = SourceRootTypeId("java-resource"),
      )

    javaResources shouldContainExactlyInAnyOrder
      listOf(
        expectedJavaResource1,
        expectedJavaResource2,
        expectedJavaResource3,
      )
  }

  @Test
  fun `should return resource roots regardless they have resource items in project base path or not`() {
    // given

    val resourceFilePath1 = Files.createTempFile(projectBasePath, "resource1", "File1.txt")
    resourceFilePath1.toFile().deleteOnExit()

    val resourceFilePath2 = Files.createTempFile("resource2", "File2.txt")
    resourceFilePath2.toFile().deleteOnExit()

    val resourceDirPath3 = Files.createTempDirectory(projectBasePath, "resourcedir")
    resourceDirPath3.toFile().deleteOnExit()

    val buildTarget =
      RawBuildTarget(
        Label.parse("//target"),
        emptyList(),
        listOf(),
        TargetKind(
          kindString = "java_binary",
          ruleType = RuleType.BINARY,
          languageClasses = setOf(LanguageClass.JAVA),
        ),
        emptyList(),
        listOf(resourceFilePath1, resourceFilePath2, resourceDirPath3),
        baseDirectory = Path("base/dir"),
      )

    // when
    val javaResources = resourcesItemToJavaResourceRootTransformer.transform(buildTarget)

    // then
    val expectedJavaResource1 =
      ResourceRoot(
        resourcePath = resourceFilePath1,
        rootType = SourceRootTypeId("java-resource"),
      )
    val expectedJavaResource2 =
      ResourceRoot(
        resourcePath = resourceFilePath2,
        rootType = SourceRootTypeId("java-resource"),
      )
    val expectedJavaResource3 =
      ResourceRoot(
        resourcePath = resourceDirPath3,
        rootType = SourceRootTypeId("java-resource"),
      )

    javaResources shouldContainExactlyInAnyOrder
      listOf(
        expectedJavaResource1,
        expectedJavaResource2,
        expectedJavaResource3,
      )
  }
}
