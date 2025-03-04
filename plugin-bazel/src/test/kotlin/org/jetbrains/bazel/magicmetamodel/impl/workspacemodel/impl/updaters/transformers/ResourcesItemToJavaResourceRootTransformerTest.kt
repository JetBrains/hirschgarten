package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.workspacemodel.entities.ResourceRoot
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetCapabilities
import org.jetbrains.bsp.protocol.ResourcesItem
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
    val emptyResources = listOf<BuildTargetAndResourcesItem>()

    // when
    val javaResources = resourcesItemToJavaResourceRootTransformer.transform(emptyResources)

    // then
    javaResources shouldBe emptyList()
  }

  @Test
  fun `should return single resource root for resources item with one file path`() {
    // given
    val buildTarget =
      BuildTarget(
        Label.parse("//target"),
        emptyList(),
        emptyList(),
        listOf(),
        BuildTargetCapabilities(),
      )
    val resourceFilePath = Files.createTempFile(projectBasePath, "resource", "File.txt")
    resourceFilePath.toFile().deleteOnExit()
    val resourceRawUri = resourceFilePath.toUri().toString()

    val resourcesItem =
      BuildTargetAndResourcesItem(
        buildTarget = buildTarget,
        resourcesItem =
          ResourcesItem(
            buildTarget.id,
            listOf(resourceRawUri),
          ),
      )

    // when
    val javaResources = resourcesItemToJavaResourceRootTransformer.transform(resourcesItem)

    // then
    val expectedJavaResource =
      ResourceRoot(
        resourcePath = resourceFilePath,
        rootType = SourceRootTypeId("java-resource"),
      )

    javaResources shouldContainExactlyInAnyOrder listOf(expectedJavaResource)
  }

  @Test
  fun `should return resource root with type test for resources item coming from a build target having test tag`() {
    // given
    val buildTarget =
      BuildTarget(
        Label.parse("//target"),
        listOf("test"),
        emptyList(),
        listOf(),
        BuildTargetCapabilities(),
      )
    val resourceFilePath = Files.createTempFile(projectBasePath, "resource", "File.txt")
    resourceFilePath.toFile().deleteOnExit()
    val resourceRawUri = resourceFilePath.toUri().toString()

    val resourcesItem =
      BuildTargetAndResourcesItem(
        buildTarget = buildTarget,
        resourcesItem =
          ResourcesItem(
            buildTarget.id,
            listOf(resourceRawUri),
          ),
      )

    // when
    val javaResources = resourcesItemToJavaResourceRootTransformer.transform(resourcesItem)

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
    val buildTarget =
      BuildTarget(
        Label.parse("//target"),
        emptyList(),
        emptyList(),
        listOf(),
        BuildTargetCapabilities(),
      )
    val resourceDirPath = Files.createTempDirectory(projectBasePath, "resource")
    resourceDirPath.toFile().deleteOnExit()
    val resourceRawUri = resourceDirPath.toUri().toString()

    val resourcesItem =
      BuildTargetAndResourcesItem(
        buildTarget = buildTarget,
        resourcesItem =
          ResourcesItem(
            buildTarget.id,
            listOf(resourceRawUri),
          ),
      )

    // when
    val javaResources = resourcesItemToJavaResourceRootTransformer.transform(resourcesItem)

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
    val buildTarget =
      BuildTarget(
        Label.parse("//target"),
        emptyList(),
        emptyList(),
        listOf(),
        BuildTargetCapabilities(),
      )
    val resourceFilePath1 = Files.createTempFile(projectBasePath, "resource", "File1.txt")
    resourceFilePath1.toFile().deleteOnExit()
    val resourceRawUri1 = resourceFilePath1.toUri().toString()
    val resourceFilePath2 = Files.createTempFile(projectBasePath, "resource", "File2.txt")
    resourceFilePath2.toFile().deleteOnExit()
    val resourceRawUri2 = resourceFilePath2.toUri().toString()
    val resourceFilePath3 = Files.createTempFile(projectBasePath, "resource", "File3.txt")
    resourceFilePath3.toFile().deleteOnExit()
    val resourceRawUri3 = resourceFilePath3.toUri().toString()

    val resourcesItem =
      BuildTargetAndResourcesItem(
        buildTarget = buildTarget,
        resourcesItem =
          ResourcesItem(
            buildTarget.id,
            listOf(resourceRawUri1, resourceRawUri2, resourceRawUri3),
          ),
      )

    // when
    val javaResources = resourcesItemToJavaResourceRootTransformer.transform(resourcesItem)

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
    val buildTarget =
      BuildTarget(
        Label.parse("//target"),
        emptyList(),
        emptyList(),
        listOf(),
        BuildTargetCapabilities(),
      )
    val resourceFilePath = Files.createTempFile(projectBasePath, "resource", "File1.txt")
    resourceFilePath.toFile().deleteOnExit()
    val resourceRawUri = resourceFilePath.toUri().toString()
    val resourceDirPath = Files.createTempDirectory(projectBasePath, "resourcedir")
    resourceDirPath.toFile().deleteOnExit()
    val resourceDirRawUri = resourceDirPath.toUri().toString()

    val resourcesItem =
      BuildTargetAndResourcesItem(
        buildTarget = buildTarget,
        resourcesItem =
          ResourcesItem(
            buildTarget.id,
            listOf(resourceRawUri, resourceDirRawUri),
          ),
      )

    // when
    val javaResources = resourcesItemToJavaResourceRootTransformer.transform(resourcesItem)

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
    val buildTarget =
      BuildTarget(
        Label.parse("//target"),
        emptyList(),
        emptyList(),
        listOf(),
        BuildTargetCapabilities(),
      )
    val resourceFilePath1 = Files.createTempFile(projectBasePath, "resource", "File1.txt")
    resourceFilePath1.toFile().deleteOnExit()
    val resourceRawUri1 = resourceFilePath1.toUri().toString()

    val resourceFilePath2 = Files.createTempFile(projectBasePath, "resource", "File2.txt")
    resourceFilePath2.toFile().deleteOnExit()
    val resourceRawUri2 = resourceFilePath2.toUri().toString()

    val resourceDirPath3 = Files.createTempDirectory(projectBasePath, "resourcedir")
    resourceDirPath3.toFile().deleteOnExit()
    val resourceDirRawUri3 = resourceDirPath3.toUri().toString()

    val resourcesItem1 =
      BuildTargetAndResourcesItem(
        buildTarget = buildTarget,
        resourcesItem =
          ResourcesItem(
            buildTarget.id,
            listOf(resourceRawUri1, resourceRawUri2),
          ),
      )

    val resourcesItem2 =
      BuildTargetAndResourcesItem(
        buildTarget = buildTarget,
        resourcesItem =
          ResourcesItem(
            buildTarget.id,
            listOf(resourceRawUri2, resourceDirRawUri3),
          ),
      )

    val resourcesItems = listOf(resourcesItem1, resourcesItem2)

    // when
    val javaResources = resourcesItemToJavaResourceRootTransformer.transform(resourcesItems)

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
    val buildTarget =
      BuildTarget(
        Label.parse("//target"),
        emptyList(),
        emptyList(),
        listOf(),
        BuildTargetCapabilities(),
      )

    val resourceFilePath1 = Files.createTempFile(projectBasePath, "resource1", "File1.txt")
    resourceFilePath1.toFile().deleteOnExit()
    val resourceRawUri1 = resourceFilePath1.toUri().toString()

    val resourceFilePath2 = Files.createTempFile("resource2", "File2.txt")
    resourceFilePath2.toFile().deleteOnExit()
    val resourceRawUri2 = resourceFilePath2.toUri().toString()

    val resourceDirPath3 = Files.createTempDirectory(projectBasePath, "resourcedir")
    resourceDirPath3.toFile().deleteOnExit()
    val resourceDirRawUri3 = resourceDirPath3.toUri().toString()

    val resourcesItem1 =
      BuildTargetAndResourcesItem(
        buildTarget = buildTarget,
        resourcesItem =
          ResourcesItem(
            buildTarget.id,
            listOf(resourceRawUri1, resourceRawUri2),
          ),
      )
    val resourcesItem2 =
      BuildTargetAndResourcesItem(
        buildTarget = buildTarget,
        resourcesItem =
          ResourcesItem(
            buildTarget.id,
            listOf(resourceRawUri2, resourceDirRawUri3),
          ),
      )

    val resourcesItems = listOf(resourcesItem1, resourcesItem2)

    // when
    val javaResources = resourcesItemToJavaResourceRootTransformer.transform(resourcesItems)

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
