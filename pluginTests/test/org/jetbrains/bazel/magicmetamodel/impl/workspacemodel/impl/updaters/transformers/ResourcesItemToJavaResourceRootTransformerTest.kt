package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import com.intellij.testFramework.utils.io.createFile
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.workspace.model.test.framework.createRawBuildTarget
import org.jetbrains.bazel.workspacemodel.entities.ResourceRoot
import org.jetbrains.bsp.protocol.ResourceItem
import org.jetbrains.bsp.protocol.UltimateBuildTarget
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.createTempFile

@DisplayName("ResourcesItemToJavaResourceRootTransformer.transform(resourcesItem) tests")
class ResourcesItemToJavaResourceRootTransformerTest {
  @TempDir
  private lateinit var tempDir: Path

  private lateinit var projectBasePath: Path

  private val resourcesDir1 by lazy { tempDir.resolve("base/dir1") }
  private val resources1 by lazy { listOf(resourcesDir1.resolve("resource1")) }
  private val resourcesDir2 by lazy { tempDir.resolve("base/dir2") }
  private val resources2 by lazy { listOf(resourcesDir2.resolve("resource2")) }

  private val resourcesItemToJavaResourceRootTransformer by lazy {
    val ultimateTargets = listOf(
      createRawBuildTarget(
        id = Label.parse("//:resources_1"),
        kind = TargetKind("resourcegroup", setOf(LanguageClass.ULTIMATE), RuleType.UNKNOWN),
        data = UltimateBuildTarget(
          resources = resources1,
          stripPrefix = resourcesDir1,
          addPrefix = "new/prefix",
        ),
      ),
      createRawBuildTarget(
        id = Label.parse("//:resources_2"),
        kind = TargetKind("resourcegroup", setOf(LanguageClass.ULTIMATE), RuleType.UNKNOWN),
        data = UltimateBuildTarget(
          resources = resources2,
          stripPrefix = resourcesDir2,
          addPrefix = null,
        ),
      ),
    ).associateBy { it.id }
    ResourcesItemToJavaResourceRootTransformer(ultimateTargets)
  }

  @BeforeEach
  fun beforeEach() {
    projectBasePath = tempDir.resolve("project").createDirectories()
  }

  @Test
  fun `should exclude resources that are not present in the strip prefix`() {
    // given
    val buildTarget = createRawBuildTarget(
      resources = listOf(
        ResourceItem.Target(Label.parse("//:resources_1")),
        ResourceItem.Target(Label.parse("//:resources_2")),
      ),
    )

    resourcesDir1.createDirectories()
    resourcesDir2.createDirectories()
    resources1.forEach { it.createFile() }
    resources2.forEach { it.createFile() }
    val excludedFile = createTempFile(resourcesDir1, "resource2")

    // when
    val javaResources = resourcesItemToJavaResourceRootTransformer.transform(buildTarget)

    // then
    javaResources shouldContainExactly listOf(
      ResourceRoot(
        resourcePath = resourcesDir1,
        rootType = SourceRootTypeId("java-resource"),
        relativePath = "new/prefix",
        excluded = listOf(excludedFile),
      ),
      ResourceRoot(
        resourcePath = resourcesDir2,
        rootType = SourceRootTypeId("java-resource"),
        relativePath = null,
      ),
    )
  }

  @Test
  fun `should return resource root for target resources`() {
    // given
    val buildTarget = createRawBuildTarget(
      resources = listOf(
        ResourceItem.Target(Label.parse("//:resources_1")),
        ResourceItem.Target(Label.parse("//:resources_2")),
      ),
    )

    // when
    val javaResources = resourcesItemToJavaResourceRootTransformer.transform(buildTarget)

    // then
    javaResources shouldContainExactly listOf(
      ResourceRoot(
        resourcePath = resourcesDir1,
        rootType = SourceRootTypeId("java-resource"),
        relativePath = "new/prefix",
      ),
      ResourceRoot(
        resourcePath = resourcesDir2,
        rootType = SourceRootTypeId("java-resource"),
        relativePath = null,
      ),
    )
  }

  @Test
  fun `should return multiple resource roots for mixed resources of files and targets`() {
    // given
    val resourceFilePath = createTempFile(projectBasePath, "resource", "File.txt")

    val buildTarget = createRawBuildTarget(
      resources = listOf(
        ResourceItem.File(resourceFilePath),
        ResourceItem.Target(Label.parse("//:resources_1")),
      ),
    )

    // when
    val javaResources = resourcesItemToJavaResourceRootTransformer.transform(buildTarget)

    // then
    javaResources shouldContainExactlyInAnyOrder listOf(
      ResourceRoot(resourcePath = resourceFilePath, rootType = SourceRootTypeId("java-resource")),
      ResourceRoot(resourcePath = resourcesDir1, rootType = SourceRootTypeId("java-resource"), relativePath = "new/prefix"),
    )
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
  fun `should return single resource root for resources item with one file`() {
    // given
    val resourceFilePath = createTempFile(projectBasePath, "resource", "File.txt")

    val buildTarget = createRawBuildTarget(resources = listOf(ResourceItem.File(resourceFilePath)))

    // when
    val javaResources = resourcesItemToJavaResourceRootTransformer.transform(buildTarget)

    // then
    javaResources shouldHaveSingleElement ResourceRoot(
      resourcePath = resourceFilePath,
      rootType = SourceRootTypeId("java-resource"),
    )

  }

  @Test
  fun `should return resource root with type test for resources item coming from a build target having test target kind`() {
    // given
    val resourceFilePath = createTempFile(projectBasePath, "resource", "File.txt")

    val buildTarget = createRawBuildTarget(
      kind = TargetKind(
        kindString = "java_test",
        ruleType = RuleType.TEST,
        languageClasses = setOf(LanguageClass.JAVA),
      ),
      resources = listOf(ResourceItem.File(resourceFilePath)),
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
  fun `should return single resource root for resources item with one dir path`() {
    // given
    val resourceDirPath = createTempDirectory(projectBasePath, "resource")

    val buildTarget = createRawBuildTarget(resources = listOf(ResourceItem.File(resourceDirPath)))

    // when
    val javaResources = resourcesItemToJavaResourceRootTransformer.transform(buildTarget)

    // then

    javaResources shouldHaveSingleElement ResourceRoot(
      resourcePath = resourceDirPath,
      rootType = SourceRootTypeId("java-resource"),
    )
  }

  @Test
  fun `should return multiple resource roots for resources item with multiple paths with the same directories`() {
    // given
    val resourceFilePath1 = createTempFile(projectBasePath, "resource", "File1.txt")
    val resourceFilePath2 = createTempFile(projectBasePath, "resource", "File2.txt")
    val resourceFilePath3 = createTempFile(projectBasePath, "resource", "File3.txt")

    val buildTarget = createRawBuildTarget(
      resources = listOf(resourceFilePath1, resourceFilePath2, resourceFilePath3).map(ResourceItem::File),
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
  fun `should return multiple resource roots for resources item with multiple paths`() {
    // given
    val resourceFilePath = createTempFile(projectBasePath, "resource", "File1.txt")
    val resourceDirPath = createTempDirectory(projectBasePath, "resourcedir")

    val buildTarget = createRawBuildTarget(resources = listOf(resourceFilePath, resourceDirPath).map(ResourceItem::File))

    // when
    val javaResources = resourcesItemToJavaResourceRootTransformer.transform(buildTarget)

    // then
    javaResources shouldContainExactlyInAnyOrder listOf(
      ResourceRoot(
        resourcePath = resourceFilePath,
        rootType = SourceRootTypeId("java-resource"),
      ),
      ResourceRoot(
        resourcePath = resourceDirPath,
        rootType = SourceRootTypeId("java-resource"),
      ),
    )
  }

  @Test
  fun `should return multiple resource roots for multiple resources items`() {
    // given
    val resourceFilePath1 = createTempFile(projectBasePath, "resource", "File1.txt")
    val resourceFilePath2 = createTempFile(projectBasePath, "resource", "File2.txt")
    val resourceDirPath3 = createTempDirectory(projectBasePath, "resourcedir")

    val buildTarget =
      createRawBuildTarget(resources = listOf(resourceFilePath1, resourceFilePath2, resourceDirPath3).map(ResourceItem::File))

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
    val resourceFilePath1 = createTempFile(projectBasePath, "resource1", "File1.txt")
    val resourceFilePath2 = createTempFile(tempDir, "resource2", "File2.txt")
    val resourceDirPath3 = createTempDirectory(projectBasePath, "resourcedir")

    val buildTarget =
      createRawBuildTarget(resources = listOf(resourceFilePath1, resourceFilePath2, resourceDirPath3).map(ResourceItem::File))

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
}
