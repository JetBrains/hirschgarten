package org.jetbrains.bazel.workspacemodel.entities

import com.intellij.platform.workspace.jps.entities.ModuleTypeId
import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bsp.protocol.MavenCoordinates
import java.nio.file.Path
import kotlin.io.path.extension

@ApiStatus.Internal
abstract class WorkspaceModelEntity

@ApiStatus.Internal
data class ContentRoot(val path: Path) : WorkspaceModelEntity()

internal interface ResourceRootEntity

internal interface EntityDependency

@ApiStatus.Internal
data class GenericSourceRoot(val sourcePath: Path, val rootType: SourceRootTypeId) : WorkspaceModelEntity()

@ApiStatus.Internal
data class ResourceRoot(val resourcePath: Path, val rootType: SourceRootTypeId) :
  WorkspaceModelEntity(),
  ResourceRootEntity

@ApiStatus.Internal
data class Library(
  val displayName: String,
  val iJars: List<Path> = listOf(),
  val sourceJars: List<Path> = listOf(),
  val classJars: List<Path> = listOf(),
  val mavenCoordinates: MavenCoordinates? = null,
  val isLowPriority: Boolean = false,
) : WorkspaceModelEntity(),
  ResourceRootEntity {
  companion object {
    fun formatJarString(jar: Path): String =
      if (jar.extension == "jar") {
        "jar://$jar!/"
      } else {
        // There can be other library roots except for jars, e.g., Android resources. Use the file:// scheme then.
        "file://$jar"
      }
  }
}

/**
This class holds basic module data that are not language-specific
 */
@ApiStatus.Internal
data class GenericModuleInfo(
  val name: String,
  val type: ModuleTypeId,
  val dependencies: List<Dependency>,
  val kind: TargetKind,
  val associates: List<String> = listOf(),
  val isDummy: Boolean = false,
  val isLibraryModule: Boolean = false,
) : WorkspaceModelEntity()

@ApiStatus.Internal
data class Dependency(
  val id: String,
  val isRuntimeOnly: Boolean = false,
  val exported: Boolean = false,
)

@ApiStatus.Internal
interface Module {
  fun getModuleName(): String
}

@ApiStatus.Internal
data class CompiledSourceCodeInsideJarExclude(
  val relativePathsInsideJarToExclude: Set<String>,
  val librariesFromInternalTargetsUrls: Set<String>,
) : WorkspaceModelEntity()
