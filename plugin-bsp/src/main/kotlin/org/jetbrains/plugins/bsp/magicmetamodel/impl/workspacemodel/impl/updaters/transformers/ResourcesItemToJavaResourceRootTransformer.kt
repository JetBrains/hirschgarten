package org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.ResourcesItem
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.ResourceRoot
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.toPath

internal data class BuildTargetAndResourcesItem(
  val buildTarget: BuildTarget,
  val resourcesItem: ResourcesItem,
)

internal class ResourcesItemToJavaResourceRootTransformer(private val projectBasePath: Path) :
  WorkspaceModelEntityPartitionTransformer<BuildTargetAndResourcesItem, ResourceRoot> {
  override fun transform(inputEntity: BuildTargetAndResourcesItem): List<ResourceRoot> {
    val rootType = inputEntity.buildTarget.inferRootType()
    return inputEntity.resourcesItem.resources
      .map { toJavaResourceRoot(it, rootType) }
      .filter { it.resourcePath.isPathInProjectBasePath(projectBasePath) }
      .distinct()
  }

  private fun toJavaResourceRoot(resourcePath: String, rootType: String) =
    ResourceRoot(
      resourcePath = URI.create(resourcePath).toPath(),
      rootType = rootType,
    )

  private fun BuildTarget.inferRootType(): String =
    if (tags.contains("test")) JAVA_TEST_RESOURCE_ROOT_TYPE else JAVA_RESOURCE_ROOT_TYPE

  companion object {
    private const val JAVA_RESOURCE_ROOT_TYPE = "java-resource"
    private const val JAVA_TEST_RESOURCE_ROOT_TYPE = "java-test-resource"
  }
}

internal fun Path.isPathInProjectBasePath(projectBasePath: Path) =
  this.toAbsolutePath().startsWith(projectBasePath.toAbsolutePath())
