package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import com.intellij.openapi.module.StdModuleTypes
import com.intellij.openapi.project.Project
import com.intellij.platform.workspace.jps.entities.ModuleTypeId
import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import org.jetbrains.bazel.config.bspProjectName
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.BazelJavaSourceRootEntityUpdater
import org.jetbrains.bazel.magicmetamodel.sanitizeName
import org.jetbrains.bazel.magicmetamodel.shortenTargetPath
import org.jetbrains.bazel.utils.allAncestorsSequence
import org.jetbrains.bazel.workspacemodel.entities.ContentRoot
import org.jetbrains.bazel.workspacemodel.entities.GenericModuleInfo
import org.jetbrains.bazel.workspacemodel.entities.JavaModule
import org.jetbrains.bazel.workspacemodel.entities.JavaSourceRoot
import org.jetbrains.bazel.workspacemodel.entities.ResourceRoot
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.notExists
import kotlin.io.path.pathString

internal data class DummySourceRootWithPackagePrefix(val sourcePath: Path, val packagePrefix: String = "")

/**
 * This is a HACK for letting single source Java files to be resolved normally
 * Should remove soon and replace with a more robust solution
 */
internal class JavaModuleToDummyJavaModulesTransformerHACK(private val projectBasePath: Path, private val project: Project) :
  WorkspaceModelEntityPartitionTransformer<JavaModule, JavaModule> {
  internal companion object {
    val DUMMY_JAVA_SOURCE_MODULE_ROOT_TYPE = SourceRootTypeId("java-source")
    val DUMMY_JAVA_RESOURCE_MODULE_ROOT_TYPE = SourceRootTypeId("java-resource")
  }

  override fun transform(inputEntity: JavaModule): List<JavaModule> {
    val dummyJavaModuleSourceRoots = calculateDummyJavaSourceRoots(inputEntity.sourceRoots)
    val dummyJavaModuleNames = calculateDummyJavaModuleNames(dummyJavaModuleSourceRoots, projectBasePath)
    val dummyJavaResourcePath = calculateDummyResourceRootPath(inputEntity, dummyJavaModuleSourceRoots, projectBasePath, project)
    return if (dummyJavaModuleNames.isEmpty() && dummyJavaResourcePath != null) {
      val dummyModuleName = calculateDummyJavaModuleName(dummyJavaResourcePath, projectBasePath)
      calculateDummyJavaSourceModuleWithOnlyResources(
        name = dummyModuleName,
        javaModule = inputEntity,
        dummyJavaResourcePath,
      )?.let { listOf(it) } ?: emptyList()
    } else {
      dummyJavaModuleSourceRoots
        .zip(dummyJavaModuleNames)
        .mapNotNull {
          calculateDummyJavaSourceModule(
            name = it.second,
            sourceRootWithPackagePrefix = it.first,
            javaModule = inputEntity,
            resourceRootPath = dummyJavaResourcePath,
          )
        }.distinctBy { it.genericModuleInfo.name }
    }
  }

  private fun calculateDummyJavaSourceModuleWithOnlyResources(
    name: String,
    javaModule: JavaModule,
    resourcesPath: Path,
  ) = if (name.isEmpty()) {
    null
  } else {
    JavaModule(
      genericModuleInfo =
        GenericModuleInfo(
          name = name,
          type = ModuleTypeId(StdModuleTypes.JAVA.id),
          modulesDependencies = listOf(),
          librariesDependencies = javaModule.genericModuleInfo.librariesDependencies,
          isDummy = true,
          languageIds = listOf("java", "scala", "kotlin"),
        ),
      baseDirContentRoot = javaModule.baseDirContentRoot,
      sourceRoots = emptyList(),
      resourceRoots =
        listOf(
          ResourceRoot(
            resourcePath = resourcesPath,
            rootType = DUMMY_JAVA_RESOURCE_MODULE_ROOT_TYPE,
          ),
        ),
      moduleLevelLibraries = listOf(),
      jvmJdkName = javaModule.jvmJdkName,
      kotlinAddendum = javaModule.kotlinAddendum,
      javaAddendum = javaModule.javaAddendum,
    )
  }

  private fun calculateDummyJavaSourceModule(
    name: String,
    sourceRootWithPackagePrefix: DummySourceRootWithPackagePrefix,
    javaModule: JavaModule,
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
          librariesDependencies = javaModule.genericModuleInfo.librariesDependencies,
          isDummy = true,
          languageIds = listOf("java", "scala", "kotlin"),
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
      jvmJdkName = javaModule.jvmJdkName,
      kotlinAddendum = javaModule.kotlinAddendum,
      javaAddendum = javaModule.javaAddendum,
    )
  }
}

private fun calculateDummyResourceRootPath(
  entity: JavaModule,
  dummySources: List<DummySourceRootWithPackagePrefix>,
  projectBasePath: Path,
  project: Project,
): Path? {
  if (!project.bspProjectName.startsWith("hirschgarten")) return null
  if (entity.androidAddendum != null) return null // Resource roots are handled already for Android
  val resourceRoots = entity.resourceRoots
  if (entity.resourceRoots.isEmpty()) return null
  val moduleRoot = entity.baseDirContentRoot?.path ?: return null

  fun Path.reversedPaths() = allAncestorsSequence().toList().reversed().asSequence()

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
      // If they are no sources present, we just take the common resources root
      if (resourceRoots.size == 1) {
        commonResourcesPaths.last().parent
      } else {
        commonResourcesPaths.last()
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
    .filter { !BazelJavaSourceRootEntityUpdater.shouldAddBazelJavaSourceRootEntity(it) }
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
    .sanitizeName()
    .replace(File.separator, ".")
    .addIntelliJDummyPrefix()
    .shortenTargetPath()
}

private const val IJ_DUMMY_MODULE_PREFIX = "_aux.synthetic"

internal fun String.addIntelliJDummyPrefix(): String =
  if (isBlank()) {
    IJ_DUMMY_MODULE_PREFIX
  } else {
    "$IJ_DUMMY_MODULE_PREFIX.$this"
  }
