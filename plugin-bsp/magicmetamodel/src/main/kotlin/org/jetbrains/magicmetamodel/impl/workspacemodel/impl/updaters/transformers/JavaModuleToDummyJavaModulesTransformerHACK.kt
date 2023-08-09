package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import com.intellij.openapi.module.ModuleTypeId
import org.jetbrains.magicmetamodel.impl.workspacemodel.ContentRoot
import org.jetbrains.magicmetamodel.impl.workspacemodel.GenericModuleInfo
import org.jetbrains.magicmetamodel.impl.workspacemodel.JavaModule
import org.jetbrains.magicmetamodel.impl.workspacemodel.JavaSourceRoot
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
    const val DUMMY_JAVA_SOURCE_MODULE_ROOT_TYPE = "java-source"
  }

  override fun transform(inputEntity: JavaModule): List<JavaModule> {
    val dummyJavaModuleSourceRoots = calculateDummyJavaSourceRoots(inputEntity)
    val dummyJavaModuleNames = calculateDummyJavaModuleNames(dummyJavaModuleSourceRoots, projectBasePath)
    return dummyJavaModuleSourceRoots.zip(dummyJavaModuleNames)
      .mapNotNull { calculateDummyJavaSourceModule(name = it.second, sourceRoot = it.first) }
  }

  private fun calculateDummyJavaSourceModule(name: String, sourceRoot: Path) =
    if (name.isEmpty()) null
    else JavaModule(
      genericModuleInfo = GenericModuleInfo(
        name = name,
        type = ModuleTypeId.JAVA_MODULE,
        modulesDependencies = listOf(),
        librariesDependencies = listOf(),
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
      compilerOutput = null,
      jvmJdkName = null,
      kotlinAddendum = null,
    )
  private fun calculateDummyJavaSourceRoots(inputEntity: JavaModule): List<Path> =
    inputEntity.sourceRoots.map {
      restoreSourceRootFromPackagePrefix(it)
    }.distinct()

  private fun restoreSourceRootFromPackagePrefix(sourceRoot: JavaSourceRoot): Path {
    val packagePrefixPath = sourceRoot.packagePrefix.replace('.', File.separatorChar)
    val directory =
      if (sourceRoot.sourcePath.isDirectory()) sourceRoot.sourcePath
      else sourceRoot.sourcePath.parent
    val sourceRootString = directory.pathString.removeSuffix(packagePrefixPath)
    return Path(sourceRootString)
  }
}

internal fun calculateDummyJavaModuleNames(
  dummyJavaModuleSourceRoots: List<Path>,
  projectBasePath: Path,
): List<String> =
  dummyJavaModuleSourceRoots.map { calculateDummyJavaModuleName(it, projectBasePath) }

internal fun calculateDummyJavaModuleName(sourceRoot: Path, projectBasePath: Path): String {
  val absoluteSourceRoot = sourceRoot.toAbsolutePath().toString()
  val absoluteProjectBasePath = projectBasePath.toAbsolutePath().toString()
  return absoluteSourceRoot
    .substringAfter(absoluteProjectBasePath)
    .trim { it == File.separatorChar }
    .replace(File.separator, ".")
}
