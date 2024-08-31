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

public data class DummySourceRootWithPackagePrefix(val sourcePath: Path, val packagePrefix: String = "")

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
    return dummyJavaModuleSourceRoots
      .zip(dummyJavaModuleNames)
      .mapNotNull {
        calculateDummyJavaSourceModule(
          name = it.second,
          sourceRootWithPackagePrefix = it.first,
          jdkName = inputEntity.jvmJdkName,
          javaAddendum = inputEntity.javaAddendum,
        )
      }
  }

  private fun calculateDummyJavaSourceModule(
    name: String,
    sourceRootWithPackagePrefix: DummySourceRootWithPackagePrefix,
    jdkName: String?,
    javaAddendum: JavaAddendum?,
  ) = if (name.isEmpty()) {
    null
  } else {
    JavaModule(
      genericModuleInfo =
        GenericModuleInfo(
          name = name,
          type = ModuleTypeId(StdModuleTypes.JAVA.id),
          modulesDependencies = listOf(),
          librariesDependencies = listOf(),
          isDummy = true,
        ),
      baseDirContentRoot = ContentRoot(path = sourceRootWithPackagePrefix.sourcePath),
      sourceRoots =
        listOf(
          JavaSourceRoot(
            sourcePath = sourceRootWithPackagePrefix.sourcePath,
            generated = false,
            packagePrefix = sourceRootWithPackagePrefix.packagePrefix,
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
}

internal fun calculateDummyJavaSourceRoots(sourceRoots: List<JavaSourceRoot>): List<DummySourceRootWithPackagePrefix> =
  sourceRoots
    .asSequence()
    .filter { !it.generated }
    .mapNotNull {
      restoreSourceRootFromPackagePrefix(it)
    }.distinct()
    .toList()

private fun restoreSourceRootFromPackagePrefix(sourceRoot: JavaSourceRoot): DummySourceRootWithPackagePrefix? {
  if (sourceRoot.sourcePath.isDirectory()) return null
  val packagePrefixPath = sourceRoot.packagePrefix.replace('.', File.separatorChar)
  val sourceParent = sourceRoot.sourcePath.parent.pathString
  val sourceRootString = sourceParent.removeSuffix(packagePrefixPath)
  val sourceRootPath = Path(sourceRootString)
  if (sourceParent == sourceRootString) return DummySourceRootWithPackagePrefix(sourceRootPath, sourceRoot.packagePrefix)
  return DummySourceRootWithPackagePrefix(sourceRootPath)
}

internal fun calculateDummyJavaModuleNames(
  dummyJavaModuleSourceRoots: List<DummySourceRootWithPackagePrefix>,
  projectBasePath: Path,
): List<String> = dummyJavaModuleSourceRoots.map { calculateDummyJavaModuleName(it.sourcePath, projectBasePath) }

internal fun calculateDummyJavaModuleName(sourceRoot: Path, projectBasePath: Path): String {
  val absoluteSourceRoot = sourceRoot.toAbsolutePath().toString()
  val absoluteProjectBasePath = projectBasePath.toAbsolutePath().toString()
  return absoluteSourceRoot
    .substringAfter(absoluteProjectBasePath)
    .trim { it == File.separatorChar }
    .replaceDots()
    .replace(File.separator, ".")
    .shortenTargetPath()
}
