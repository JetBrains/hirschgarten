package org.jetbrains.plugins.bsp.impl.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import com.intellij.openapi.module.StdModuleTypes
import com.intellij.platform.workspace.jps.entities.ModuleTypeId
import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import org.jetbrains.plugins.bsp.impl.magicmetamodel.extensions.allSubdirectoriesSequence
import org.jetbrains.plugins.bsp.impl.magicmetamodel.impl.workspacemodel.JavaAddendum
import org.jetbrains.plugins.bsp.impl.magicmetamodel.impl.workspacemodel.JavaModule
import org.jetbrains.plugins.bsp.impl.magicmetamodel.impl.workspacemodel.JavaSourceRoot
import org.jetbrains.plugins.bsp.impl.utils.replaceDots
import org.jetbrains.plugins.bsp.impl.utils.shortenTargetPath
import org.jetbrains.plugins.bsp.workspacemodel.entities.ContentRoot
import org.jetbrains.plugins.bsp.workspacemodel.entities.GenericModuleInfo
import org.jetbrains.plugins.bsp.workspacemodel.entities.ResourceRoot
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.notExists
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
    val DUMMY_JAVA_RESOURCE_MODULE_ROOT_TYPE = SourceRootTypeId("java-resource")
  }

  override fun transform(inputEntity: JavaModule): List<JavaModule> {
    val dummyJavaModuleSourceRoots = calculateDummyJavaSourceRoots(inputEntity.sourceRoots)
    val dummyJavaModuleNames = calculateDummyJavaModuleNames(dummyJavaModuleSourceRoots, projectBasePath)
    val dummyJavaResourcePath = calculateDummyResourceRootPath(inputEntity, dummyJavaModuleSourceRoots, projectBasePath)
    return dummyJavaModuleSourceRoots
      .zip(dummyJavaModuleNames)
      .mapNotNull {
        calculateDummyJavaSourceModule(
          name = it.second,
          sourceRootWithPackagePrefix = it.first,
          jdkName = inputEntity.jvmJdkName,
          javaAddendum = inputEntity.javaAddendum,
          resourceRootPath = dummyJavaResourcePath,
        )
      }.distinctBy { it.genericModuleInfo.name }
  }

  private fun calculateDummyJavaSourceModule(
    name: String,
    sourceRootWithPackagePrefix: DummySourceRootWithPackagePrefix,
    jdkName: String?,
    javaAddendum: JavaAddendum?,
    resourceRootPath: Path? = null,
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
      resourceRoots =
        if (resourceRootPath != null) {
          listOf(
            ResourceRoot(
              resourcePath = resourceRootPath,
              rootType = DUMMY_JAVA_RESOURCE_MODULE_ROOT_TYPE,
            ),
          )
        } else {
          listOf()
        },
      moduleLevelLibraries = listOf(),
      jvmJdkName = jdkName,
      kotlinAddendum = null,
      javaAddendum = javaAddendum,
    )
  }
}

internal fun calculateDummyResourceRootPath(
  entity: JavaModule,
  dummySources: List<DummySourceRootWithPackagePrefix>,
  projectBasePath: Path,
): Path? {
  val resourceRoots = entity.resourceRoots
  if (entity.resourceRoots.isEmpty()) return null
  val moduleRoot = entity.baseDirContentRoot?.path ?: return null

  fun Path.reversedPaths() = allSubdirectoriesSequence().toList().reversed().asSequence()

  fun Sequence<Path>.findCommonParentsWith(list: Sequence<Path>): Sequence<Path> {
    val lastCommon = zip(list) { a, b -> a == b }.lastIndexOf(true)
    return take(lastCommon + 1)
  }

  fun <T> List<T>.foldPathsToCommonParent(list: Sequence<Path>, operation: (T) -> Sequence<Path>) =
    fold(list) { acc, element -> acc.findCommonParentsWith(operation(element)) }

  fun Path.isDescendantOf(ancestor: Path) = toAbsolutePath().startsWith(ancestor.toAbsolutePath())

  fun getImmediateChildFromAncestorOrNull(path: Path, ancestor: Path): Path? =
    if (path == ancestor || !path.isDescendantOf(ancestor)) {
      null
    } else {
      ancestor.resolve(path.toAbsolutePath().getName(ancestor.toAbsolutePath().nameCount))
    }

  val firstResourcePaths = resourceRoots.first().resourcePath.reversedPaths()
  // We calculate common parent among all module resources
  val commonResourcesPaths = resourceRoots.foldPathsToCommonParent(firstResourcePaths) { it.resourcePath.reversedPaths() }
  val resourcesRootPath =
    if (dummySources.isNotEmpty()) {
      // We try to calculate common paths parent between sources and resources and checks if it's still inside module root
      val firstSourcePaths = dummySources.first().sourcePath.reversedPaths()
      val commonSourcePaths = dummySources.foldPathsToCommonParent(firstSourcePaths) { it.sourcePath.reversedPaths() }
      val commonRoot = commonResourcesPaths.findCommonParentsWith(commonSourcePaths).last()
      if (commonRoot.isDescendantOf(moduleRoot)) {
        getImmediateChildFromAncestorOrNull(commonResourcesPaths.last(), commonRoot)
      } else {
        null
      }
    } else {
      // If they are no sources present, we just take the immediate child of module root with all the resources inside
      if (commonResourcesPaths.last() == moduleRoot) {
        moduleRoot
      } else {
        getImmediateChildFromAncestorOrNull(commonResourcesPaths.last(), moduleRoot)
      }
    }
  // We will take the path if it's inside projectBasePath
  return resourcesRootPath?.takeIf { path ->
    resourceRoots.none { it.resourcePath == path } && path.isDescendantOf(projectBasePath)
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
  if (sourceRoot.sourcePath.notExists() || sourceRoot.sourcePath.isDirectory()) return null
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
    .addIntelliJDummySuffix()
    .shortenTargetPath()
}

private fun String.addIntelliJDummySuffix() = "$this-intellij-generated"
