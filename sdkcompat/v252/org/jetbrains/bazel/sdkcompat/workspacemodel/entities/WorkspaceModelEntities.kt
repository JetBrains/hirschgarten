package org.jetbrains.bazel.sdkcompat.workspacemodel.entities

import com.intellij.platform.workspace.jps.entities.ModuleTypeId
import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bsp.protocol.MavenCoordinates
import java.nio.file.Path
import kotlin.io.path.extension

abstract class WorkspaceModelEntity

data class ContentRoot(val path: Path) : WorkspaceModelEntity()

interface ResourceRootEntity

interface EntityDependency

data class GenericSourceRoot(val sourcePath: Path, val rootType: SourceRootTypeId) : WorkspaceModelEntity()

data class ResourceRoot(val resourcePath: Path, val rootType: SourceRootTypeId) :
  WorkspaceModelEntity(),
  ResourceRootEntity

data class Library(
  val displayName: String,
  val iJars: List<Path> = listOf(),
  val sourceJars: List<Path> = listOf(),
  val classJars: List<Path> = listOf(),
  val mavenCoordinates: MavenCoordinates? = null,
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
data class GenericModuleInfo(
  val name: String,
  val type: ModuleTypeId,
  val dependencies: List<String>,
  val kind: TargetKind,
  val associates: List<String> = listOf(),
  val isDummy: Boolean = false,
  val isLibraryModule: Boolean = false,
) : WorkspaceModelEntity()

interface Module {
  fun getModuleName(): String
}

data class CompiledSourceCodeInsideJarExclude(
  val relativePathsInsideJarToExclude: Set<String>,
  val librariesFromInternalTargetsUrls: Set<String>,
) : WorkspaceModelEntity()
