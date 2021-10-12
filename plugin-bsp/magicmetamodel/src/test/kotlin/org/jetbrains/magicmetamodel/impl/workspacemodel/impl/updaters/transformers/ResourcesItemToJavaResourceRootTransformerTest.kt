package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.ResourcesItem
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.JavaResourceRoot
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.file.Files

@DisplayName("ResourcesItemToJavaResourceRootTransformer.transform(resourcesItem) tests")
class ResourcesItemToJavaResourceRootTransformerTest {

  @Test
  fun `should return no resources roots for no resources items`() {
    // given
    val emptyResources = listOf<ResourcesItem>()

    // when
    val javaResources = ResourcesItemToJavaResourceRootTransformer.transform(emptyResources)

    // then
    javaResources shouldBe emptyList()
  }

  @Test
  fun `should return single resource root for resources item with one file path`() {
    // given
    val resourceFilePath = Files.createTempFile("resource", "File.txt")
    val resourceRawUri = resourceFilePath.toUri().toString()

    val resourcesItem = ResourcesItem(
      BuildTargetIdentifier("//target"),
      listOf(resourceRawUri)
    )

    // when
    val javaResources = ResourcesItemToJavaResourceRootTransformer.transform(resourcesItem)

    // then
    val expectedJavaResource = JavaResourceRoot(
      resourcePath = resourceFilePath.parent
    )

    javaResources shouldContainExactlyInAnyOrder listOf(expectedJavaResource)
  }

  @Test
  fun `should return single resource root for resources item with one dir path`() {
    // given
    val resourceDirPath = Files.createTempDirectory("resource")
    val resourceRawUri = resourceDirPath.toUri().toString()

    val resourcesItem = ResourcesItem(
      BuildTargetIdentifier("//target"),
      listOf(resourceRawUri)
    )

    // when
    val javaResources = ResourcesItemToJavaResourceRootTransformer.transform(resourcesItem)

    // then
    val expectedJavaResource = JavaResourceRoot(
      resourcePath = resourceDirPath
    )

    javaResources shouldContainExactlyInAnyOrder listOf(expectedJavaResource)
  }

  @Test
  fun `should return one resource root for resources item with multiple paths with the same directories`() {
    // given
    val resourceFilePath1 = Files.createTempFile("resource", "File1.txt")
    val resourceRawUri1 = resourceFilePath1.toUri().toString()
    val resourceFilePath2 = Files.createTempFile("resource", "File2.txt")
    val resourceRawUri2 = resourceFilePath2.toUri().toString()
    val resourceFilePath3 = Files.createTempFile("resource", "File3.txt")
    val resourceRawUri3 = resourceFilePath3.toUri().toString()

    val resourcesItem = ResourcesItem(
      BuildTargetIdentifier("//target"),
      listOf(resourceRawUri1, resourceRawUri2, resourceRawUri3)
    )

    // when
    val javaResources = ResourcesItemToJavaResourceRootTransformer.transform(resourcesItem)

    // then
    val expectedJavaResource = JavaResourceRoot(
      resourcePath = resourceFilePath1.parent
    )

    javaResources shouldContainExactlyInAnyOrder listOf(expectedJavaResource)
  }

  @Test
  fun `should return multiple resource roots for resources item with multiple paths`() {
    // given
    val resourceFilePath = Files.createTempFile("resource", "File1.txt")
    val resourceRawUri = resourceFilePath.toUri().toString()
    val resourceDirPath = Files.createTempDirectory("resourcedir")
    val resourceDirRawUri = resourceDirPath.toUri().toString()

    val resourcesItem = ResourcesItem(
      BuildTargetIdentifier("//target"),
      listOf(resourceRawUri, resourceDirRawUri),
    )

    // when
    val javaResources = ResourcesItemToJavaResourceRootTransformer.transform(resourcesItem)

    // then
    val expectedJavaResource1 = JavaResourceRoot(
      resourcePath = resourceFilePath.parent
    )
    val expectedJavaResource2 = JavaResourceRoot(
      resourcePath = resourceDirPath
    )

    javaResources shouldContainExactlyInAnyOrder listOf(expectedJavaResource1, expectedJavaResource2)
  }

  @Test
  fun `should return multiple resource roots for multiple resources items`() {
    // given
    val resourceFilePath1 = Files.createTempFile("resource", "File1.txt")
    val resourceRawUri1 = resourceFilePath1.toUri().toString()

    val resourceFilePath2 = Files.createTempFile("resource", "File2.txt")
    val resourceRawUri2 = resourceFilePath2.toUri().toString()

    val resourceDirPath3 = Files.createTempDirectory("resourcedir")
    val resourceDirRawUri3 = resourceDirPath3.toUri().toString()

    val resourcesItem1 = ResourcesItem(
      BuildTargetIdentifier("//target"),
      listOf(resourceRawUri1, resourceRawUri2)
    )
    val resourcesItem2 = ResourcesItem(
      BuildTargetIdentifier("//target"),
      listOf(resourceRawUri2, resourceDirRawUri3)
    )

    val resourcesItems = listOf(resourcesItem1, resourcesItem2)

    // when
    val javaResources = ResourcesItemToJavaResourceRootTransformer.transform(resourcesItems)

    // then
    val expectedJavaResource1 = JavaResourceRoot(
      resourcePath = resourceFilePath1.parent
    )
    val expectedJavaResource2 = JavaResourceRoot(
      resourcePath = resourceDirPath3
    )

    javaResources shouldContainExactlyInAnyOrder listOf(
      expectedJavaResource1,
      expectedJavaResource2,
    )
  }
}
