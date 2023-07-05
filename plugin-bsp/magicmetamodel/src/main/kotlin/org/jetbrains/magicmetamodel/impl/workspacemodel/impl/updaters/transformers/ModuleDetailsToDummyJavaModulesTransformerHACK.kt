package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import com.intellij.openapi.module.ModuleTypeId
import org.jetbrains.magicmetamodel.impl.workspacemodel.ModuleDetails
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.ContentRoot
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.JavaModule
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.JavaSourceRoot
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.Module
import java.io.File
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.toPath

/**
 * This is a HACK for letting single source Java files to be resolved normally
 * Should remove soon and replace with a more robust solution
 */
internal class ModuleDetailsToDummyJavaModulesTransformerHACK(private val projectBasePath: Path) :
  WorkspaceModelEntityPartitionTransformer<ModuleDetails, JavaModule> {

  companion object {
    const val DUMMY_JAVA_SOURCE_MODULE_ROOT_TYPE = "java-source"
  }

  override fun transform(inputEntity: ModuleDetails): List<JavaModule> {
    val dummyJavaModuleSourceRoots = calculateDummyJavaSourceRoots(inputEntity)
    val dummyJavaModuleNames = calculateDummyJavaModuleNames(dummyJavaModuleSourceRoots, projectBasePath)
    return dummyJavaModuleSourceRoots.zip(dummyJavaModuleNames)
      .mapNotNull { calculateDummyJavaSourceModule(name = it.second, sourceRoot = it.first) }
  }

  private fun calculateDummyJavaSourceModule(name: String, sourceRoot: Path) =
    if (name.isEmpty()) null
    else JavaModule(
      module = Module(
        name = name,
        type = ModuleTypeId.JAVA_MODULE,
        modulesDependencies = listOf(),
        librariesDependencies = listOf()
      ),
      baseDirContentRoot = ContentRoot(url = sourceRoot),
      sourceRoots = listOf(
        JavaSourceRoot(
          sourcePath = sourceRoot,
          generated = true,
          packagePrefix = "",
          rootType = DUMMY_JAVA_SOURCE_MODULE_ROOT_TYPE,
          targetId = BuildTargetIdentifier(name)
        )
      ),
      resourceRoots = listOf(),
      libraries = listOf(),
      compilerOutput = null,
      jvmJdkInfo = null
    )
}


public fun calculateDummyJavaModuleNames(inputEntity: ModuleDetails, projectBasePath: Path): List<String> {
  val dummyJavaModuleSourceRoots = calculateDummyJavaSourceRoots(inputEntity)
  return calculateDummyJavaModuleNames(dummyJavaModuleSourceRoots, projectBasePath).filter { it.isNotEmpty() }
}

private fun calculateDummyJavaSourceRoots(inputEntity: ModuleDetails): List<Path> =
  inputEntity.sources.mapNotNull { it.roots }.flatten().map { URI.create(it) }.map { it.toPath() }

private fun calculateDummyJavaModuleNames(dummyJavaModuleSourceRoots: List<Path>, projectBasePath: Path): List<String> =
  dummyJavaModuleSourceRoots.map { calculateDummyJavaModuleName(it, projectBasePath) }

internal fun calculateDummyJavaModuleName(sourceRoot: Path, projectBasePath: Path): String {
  val absoluteSourceRoot = sourceRoot.toAbsolutePath().toString()
  val absoluteProjectBasePath = projectBasePath.toAbsolutePath().toString()
  return absoluteSourceRoot
    .substringAfter(absoluteProjectBasePath)
    .trim { it == File.separatorChar }
    .replace(File.separator, ".")
}
