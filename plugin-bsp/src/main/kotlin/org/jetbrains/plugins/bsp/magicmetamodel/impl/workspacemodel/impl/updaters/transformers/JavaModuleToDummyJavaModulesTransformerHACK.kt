package org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import com.intellij.openapi.module.StdModuleTypes
import com.intellij.platform.workspace.jps.entities.ModuleTypeId
import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.ContentRoot
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.GenericModuleInfo
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.JavaAddendum
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.JavaModule
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.JavaSourceRoot
import org.jetbrains.plugins.bsp.utils.replaceDots
import org.jetbrains.plugins.bsp.utils.shortenTargetPath
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.pathString

/**
 * This is a HACK for letting single source Java files to be resolved normally
 * Should remove soon and replace with a more robust solution
 */
public class JavaModuleToDummyJavaModulesTransformerHACK(private val projectBasePath: Path) :
  WorkspaceModelEntityPartitionTransformer<JavaModule, JavaModule> {
  internal companion object {
    val DUMMY_JAVA_SOURCE_MODULE_ROOT_TYPE = SourceRootTypeId("java-source")
  }

  override fun transform(inputEntity: JavaModule): List<JavaModule> {
    val dummyJavaModuleSourceRoots = calculateDummyJavaSourceRoots(inputEntity.sourceRoots)
    val dummyJavaModuleNames = calculateDummyJavaModuleNames(dummyJavaModuleSourceRoots, projectBasePath)
    return dummyJavaModuleSourceRoots.zip(dummyJavaModuleNames)
      .mapNotNull {
        calculateDummyJavaSourceModule(
          name = it.second,
          sourceRoot = it.first,
          jdkName = inputEntity.jvmJdkName,
          javaAddendum = inputEntity.javaAddendum,
        )
      }
  }

  private fun calculateDummyJavaSourceModule(
    name: String,
    sourceRoot: Path,
    jdkName: String?,
    javaAddendum: JavaAddendum?,
  ) =
    if (name.isEmpty()) null
    else JavaModule(
      genericModuleInfo = GenericModuleInfo(
        name = name,
        type = ModuleTypeId(StdModuleTypes.JAVA.id),
        modulesDependencies = listOf(),
        librariesDependencies = listOf(),
        isDummy = true,
      ),
      baseDirContentRoot = ContentRoot(path = sourceRoot),
      sourceRoots = listOf(
        JavaSourceRoot(
          sourcePath = sourceRoot,
          generated = false,
          packagePrefix = "",
          rootType = DUMMY_JAVA_SOURCE_MODULE_ROOT_TYPE,
        ),
      ),
      resourceRoots = listOf(),
      moduleLevelLibraries = listOf(),
      jvmJdkName = jdkName,
      kotlinAddendum = null,
      javaAddendum = javaAddendum,
    )
}

internal fun calculateDummyJavaSourceRoots(sourceRoots: List<JavaSourceRoot>): List<Path> =
  sourceRoots.mapNotNull {
    restoreSourceRootFromPackagePrefix(it)
  }.distinct()

private fun restoreSourceRootFromPackagePrefix(sourceRoot: JavaSourceRoot): Path? {
  if (sourceRoot.sourcePath.isDirectory()) return null
  val packagePrefixPath = sourceRoot.packagePrefix.replace('.', File.separatorChar)
  val directory = sourceRoot.sourcePath.parent
  val sourceRootString = directory.pathString.removeSuffix(packagePrefixPath)
  return Path(sourceRootString)
}

internal fun calculateDummyJavaModuleNames(
  dummyJavaModuleSourceRoots: List<Path>,
  projectBasePath: Path,
): List<String> =
  dummyJavaModuleSourceRoots.mapNotNull { calculateDummyJavaModuleName(it, projectBasePath) }

internal fun calculateDummyJavaModuleName(sourceRoot: Path, projectBasePath: Path): String? {
  val absoluteSourceRoot = sourceRoot.toAbsolutePath()
  val absoluteProjectBasePath = projectBasePath.toAbsolutePath()
  // Don't create dummy Java modules for source roots outside the project directory so that they aren't indexed
  if (!absoluteSourceRoot.startsWith(absoluteProjectBasePath)) return null
  return absoluteSourceRoot.toString()
    .substringAfter(absoluteProjectBasePath.toString())
    .trim { it == File.separatorChar }
    .replaceDots()
    .replace(File.separator, ".")
    .shortenTargetPath()
}
