package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.workspace.model.test.framework.createRawBuildTarget
import org.jetbrains.bazel.workspacemodel.entities.ResourceRoot
import org.jetbrains.bsp.protocol.JvmBuildTarget
import org.jetbrains.bsp.protocol.KotlinBuildTarget
import org.jetbrains.bsp.protocol.SourceItem
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile

@DisplayName("ResourcesItemToJavaResourceRootTransformer.transform(resourcesItem) tests")
class ResourcesItemToJavaResourceRootTransformerTest {
  @TempDir
  private lateinit var tempDir: Path

  private lateinit var projectBasePath: Path

  private val resourcesItemToJavaResourceRootTransformer = ResourcesItemToJavaResourceRootTransformer()

  @BeforeEach
  fun beforeEach() {
    projectBasePath = tempDir.resolve("project").createDirectories()
  }

  @Test
  fun `should return no resources roots for no resources items`() {
    // given
    val buildTarget = createRawBuildTarget()

    // when
    val javaResources = resourcesItemToJavaResourceRootTransformer.transform(buildTarget)

    // then
    javaResources shouldBe emptyList()
  }

  @Test
  fun `should return resource root with type test for resources item coming from a build target having test target kind`() {
    // given
    val resourceFilePath = projectBasePath.resolve("resourceFile.txt").createFile()

    val buildTarget = createRawBuildTarget(
      kind = TargetKind(
        kindString = "java_test",
        ruleType = RuleType.TEST,
        languageClasses = setOf(LanguageClass.JAVA),
      ),
      resources = listOf(resourceFilePath),
    )

    // when
    val javaResources = resourcesItemToJavaResourceRootTransformer.transform(buildTarget)

    // then

    javaResources shouldHaveSingleElement ResourceRoot(
      resourcePath = resourceFilePath,
      rootType = SourceRootTypeId("java-test-resource"),
    )
  }

  @Test
  fun `should return single resource root for resources item with one file in non standard directory`() {
    // given
    val resourceFilePath = projectBasePath.resolve("resourceFile.txt").createFile()

    val buildTarget = createRawBuildTarget(resources = listOf(resourceFilePath))

    // when
    val javaResources = resourcesItemToJavaResourceRootTransformer.transform(buildTarget)

    // then
    javaResources shouldHaveSingleElement ResourceRoot(
      resourcePath = resourceFilePath,
      rootType = SourceRootTypeId("java-resource"),
    )

  }

  @Test
  fun `should return single resource root for single and non standard directory`() {
    // given
    val resourceDirPath = projectBasePath.resolve("resource").createDirectories()

    val buildTarget = createRawBuildTarget(resources = listOf(resourceDirPath))

    // when
    val javaResources = resourcesItemToJavaResourceRootTransformer.transform(buildTarget)

    // then

    javaResources shouldHaveSingleElement ResourceRoot(
      resourcePath = resourceDirPath,
      rootType = SourceRootTypeId("java-resource"),
    )
  }

  @Test
  fun `should return resource root for each file when in the same non standard directory`() {
    // given
    val resourceFilePath1 = projectBasePath.resolve("resourceFile1.txt").createFile()
    val resourceFilePath2 = projectBasePath.resolve("resourceFile2.txt").createFile()
    val resourceFilePath3 = projectBasePath.resolve("resourceFile3.txt").createFile()

    val buildTarget = createRawBuildTarget(
      resources = listOf(resourceFilePath1, resourceFilePath2, resourceFilePath3),
    )

    // when
    val javaResources = resourcesItemToJavaResourceRootTransformer.transform(buildTarget)

    // then
    javaResources shouldContainExactlyInAnyOrder listOf(
      ResourceRoot(
        resourcePath = resourceFilePath1,
        rootType = SourceRootTypeId("java-resource"),
      ),
      ResourceRoot(
        resourcePath = resourceFilePath2,
        rootType = SourceRootTypeId("java-resource"),
      ),
      ResourceRoot(
        resourcePath = resourceFilePath3,
        rootType = SourceRootTypeId("java-resource"),
      ),
    )
  }

  @Test
  fun `should return resource root for each resource when in non standard directory`() {
    // given
    val resourceFilePath1 = projectBasePath.resolve("resourceFile1.txt").createFile()
    val resourceFilePath2 = projectBasePath.resolve("resourceFile2.txt").createFile()
    val resourceDirPath3 = projectBasePath.resolve("resourcedir").createDirectories()

    val buildTarget =
      createRawBuildTarget(resources = listOf(resourceFilePath1, resourceFilePath2, resourceDirPath3))

    // when
    val javaResources = resourcesItemToJavaResourceRootTransformer.transform(buildTarget)

    // then
    javaResources shouldContainExactlyInAnyOrder listOf(
      ResourceRoot(
        resourcePath = resourceFilePath1,
        rootType = SourceRootTypeId("java-resource"),
      ),
      ResourceRoot(
        resourcePath = resourceFilePath2,
        rootType = SourceRootTypeId("java-resource"),
      ),
      ResourceRoot(
        resourcePath = resourceDirPath3,
        rootType = SourceRootTypeId("java-resource"),
      ),
    )
  }

  @Test
  fun `should return resource roots regardless they have resource items in project base path or not`() {
    // given
    val resourceFilePath1 = projectBasePath.resolve("resource1File1.txt").createFile()
    val resourceFilePath2 = tempDir.resolve("resource2File2.txt").createFile()
    val resourceDirPath3 = projectBasePath.resolve("resourcedir").createDirectories()

    val buildTarget =
      createRawBuildTarget(resources = listOf(resourceFilePath1, resourceFilePath2, resourceDirPath3))

    // when
    val javaResources = resourcesItemToJavaResourceRootTransformer.transform(buildTarget)

    // then
    javaResources shouldContainExactlyInAnyOrder listOf(
      ResourceRoot(
        resourcePath = resourceFilePath1,
        rootType = SourceRootTypeId("java-resource"),
      ),
      ResourceRoot(
        resourcePath = resourceFilePath2,
        rootType = SourceRootTypeId("java-resource"),
      ),
      ResourceRoot(
        resourcePath = resourceDirPath3,
        rootType = SourceRootTypeId("java-resource"),
      ),
    )
  }

  @Test
  fun `should use explicit resourceStripPrefix from JvmBuildTarget`() {
    // given
    val resourcesDir = projectBasePath.resolve("src/main/resources").createDirectories()
    val resourceFile1 = resourcesDir.resolve("config.properties").createFile()
    val resourcesMessagesDir = resourcesDir.resolve("messages").createDirectories()
    val resourceFile2 = resourcesMessagesDir.resolve("Bundle.properties").createFile()
    val resourceFile3 = resourcesMessagesDir.resolve("OtherBundle.properties").createFile()

    val buildTarget = createRawBuildTarget(
      resources = listOf(resourceFile1, resourceFile2, resourceFile3),
      data = JvmBuildTarget(
        javaVersion = "17",
        resourceStripPrefix = resourcesDir,
      ),
    )

    // when
    val javaResources = resourcesItemToJavaResourceRootTransformer.transform(buildTarget)

    // then
    javaResources shouldHaveSingleElement ResourceRoot(
      resourcePath = resourcesDir,
      rootType = SourceRootTypeId("java-resource"),
    )
  }

  @Test
  fun `should detect src main resources heuristic strip prefix`() {
    // given
    val resourcesDir = projectBasePath.resolve("src/main/resources").createDirectories()
    val resourceFile = resourcesDir.resolve("config.properties").createFile()

    val buildTarget = createRawBuildTarget(resources = listOf(resourceFile))

    // when
    val javaResources = resourcesItemToJavaResourceRootTransformer.transform(buildTarget)

    // then
    javaResources shouldHaveSingleElement ResourceRoot(
      resourcePath = resourcesDir,
      rootType = SourceRootTypeId("java-resource"),
    )
  }

  @Test
  fun `should detect src test resources heuristic strip prefix`() {
    // given
    val resourcesDir = projectBasePath.resolve("src/test/resources").createDirectories()
    val resourceFile = resourcesDir.resolve("test-config.properties").createFile()

    val buildTarget = createRawBuildTarget(
      kind = TargetKind(
        kindString = "java_test",
        ruleType = RuleType.TEST,
        languageClasses = setOf(LanguageClass.JAVA),
      ),
      resources = listOf(resourceFile),
    )

    // when
    val javaResources = resourcesItemToJavaResourceRootTransformer.transform(buildTarget)

    // then
    javaResources shouldHaveSingleElement ResourceRoot(
      resourcePath = resourcesDir,
      rootType = SourceRootTypeId("java-test-resource"),
    )
  }

  @Test
  fun `should detect java segment heuristic strip prefix`() {
    // given
    val javaDir = projectBasePath.resolve("java").createDirectories()
    val packageDir = javaDir.resolve("com/example").createDirectories()
    val resourceFile = packageDir.resolve("data.xml").createFile()

    val buildTarget = createRawBuildTarget(resources = listOf(resourceFile))

    // when
    val javaResources = resourcesItemToJavaResourceRootTransformer.transform(buildTarget)

    // then
    javaResources shouldHaveSingleElement ResourceRoot(
      resourcePath = javaDir,
      rootType = SourceRootTypeId("java-resource"),
    )
  }

  @Test
  fun `should detect kotlin segment heuristic strip prefix for KotlinBuildTarget`() {
    // given
    val kotlinDir = projectBasePath.resolve("kotlin").createDirectories()
    val packageDir = kotlinDir.resolve("com/example").createDirectories()
    val resourceFile = packageDir.resolve("data.xml").createFile()

    val buildTarget = createRawBuildTarget(
      kind = TargetKind(
        kindString = "kt_jvm_library",
        ruleType = RuleType.LIBRARY,
        languageClasses = setOf(LanguageClass.KOTLIN),
      ),
      resources = listOf(resourceFile),
      data = KotlinBuildTarget(
        languageVersion = "1.9",
        apiVersion = "1.9",
        kotlincOptions = emptyList(),
        associates = emptyList(),
      ),
    )

    // when
    val javaResources = resourcesItemToJavaResourceRootTransformer.transform(buildTarget)

    // then
    javaResources shouldHaveSingleElement ResourceRoot(
      resourcePath = kotlinDir,
      rootType = SourceRootTypeId("java-resource"),
    )
  }
  
  @Test
  fun `should handle multiple strip prefixes from different heuristics`() {
    // given
    val srcMainResources = projectBasePath.resolve("src/main/resources").createDirectories()
    val resourceFile1 = srcMainResources.resolve("config.properties").createFile()

    val javaDir = projectBasePath.resolve("java").createDirectories()
    val javaPackageDir = javaDir.resolve("com/example").createDirectories()
    val resourceFile2 = javaPackageDir.resolve("data.xml").createFile()

    val buildTarget = createRawBuildTarget(resources = listOf(resourceFile1, resourceFile2))

    // when
    val javaResources = resourcesItemToJavaResourceRootTransformer.transform(buildTarget)

    javaResources shouldContainExactlyInAnyOrder listOf(
      ResourceRoot(
        resourcePath = srcMainResources,
        rootType = SourceRootTypeId("java-resource"),
      ),
      ResourceRoot(
        resourcePath = javaDir,
        rootType = SourceRootTypeId("java-resource"),
      ),
    )
  }

  @Test
  fun `should not use kotlin segment heuristic for non-Kotlin targets`() {
    // given
    val kotlinDir = projectBasePath.resolve("kotlin").createDirectories()
    val packageDir = kotlinDir.resolve("com/example").createDirectories()
    val resourceFile = packageDir.resolve("data.xml").createFile()

    // Java target, not Kotlin
    val buildTarget = createRawBuildTarget(
      kind = TargetKind(
        kindString = "java_library",
        ruleType = RuleType.LIBRARY,
        languageClasses = setOf(LanguageClass.JAVA),
      ),
      resources = listOf(resourceFile),
    )

    // when
    val javaResources = resourcesItemToJavaResourceRootTransformer.transform(buildTarget)

    // then - should return individual resource file, not merged under kotlin/ prefix
    javaResources shouldHaveSingleElement ResourceRoot(
      resourcePath = resourceFile,
      rootType = SourceRootTypeId("java-resource"),
    )
  }

  @Test
  fun `should detect deep nested src resources pattern`() {
    // given
    val resourcesDir = projectBasePath.resolve("modules/core/src/main/resources").createDirectories()
    val resourceFile = resourcesDir.resolve("config.properties").createFile()

    val buildTarget = createRawBuildTarget(resources = listOf(resourceFile))

    // when
    val javaResources = resourcesItemToJavaResourceRootTransformer.transform(buildTarget)

    // then
    javaResources shouldHaveSingleElement ResourceRoot(
      resourcePath = resourcesDir,
      rootType = SourceRootTypeId("java-resource"),
    )
  }

  @Test
  fun `should return leftover resources that do not match any strip prefix`() {
    // given
    val resourcesDir = projectBasePath.resolve("src/main/resources").createDirectories()
    val resourceFile1 = resourcesDir.resolve("config.properties").createFile()

    // This file is outside any detected strip prefix
    val outsideDir = projectBasePath.resolve("other").createDirectories()
    val outsideFile = outsideDir.resolve("data.txt").createFile()

    val buildTarget = createRawBuildTarget(resources = listOf(resourceFile1, outsideFile))

    // when
    val javaResources = resourcesItemToJavaResourceRootTransformer.transform(buildTarget)

    // then
    javaResources shouldContainExactlyInAnyOrder listOf(
      ResourceRoot(
        resourcePath = resourcesDir,
        rootType = SourceRootTypeId("java-resource"),
      ),
      ResourceRoot(
        resourcePath = outsideFile,
        rootType = SourceRootTypeId("java-resource"),
      ),
    )
  }
}
