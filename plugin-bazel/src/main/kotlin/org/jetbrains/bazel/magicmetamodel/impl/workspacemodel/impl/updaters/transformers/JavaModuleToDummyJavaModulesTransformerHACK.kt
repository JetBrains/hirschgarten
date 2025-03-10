package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import com.intellij.openapi.module.StdModuleTypes
import com.intellij.openapi.project.Project
import com.intellij.platform.workspace.jps.entities.ModuleTypeId
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.bazelProjectName
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
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.notExists
import kotlin.io.path.pathString

/**
 * This is a HACK for letting single source Java files to be resolved normally
 * Should remove soon and replace with a more robust solution
 */
internal class JavaModuleToDummyJavaModulesTransformerHACK(private val projectBasePath: Path, private val project: Project) {
  sealed interface Result

  class DummyModulesToAdd(val dummyModules: List<JavaModule>) : Result

  class MergedSourceRoots(val mergedSourceRoots: List<JavaSourceRoot>) : Result

  fun transform(inputEntity: JavaModule): Result {
    if (!BazelFeatureFlags.addDummyModules && !BazelFeatureFlags.mergeSourceRoots) return DummyModulesToAdd(emptyList())

    val dummyJavaModuleSourceRoots = calculateDummyJavaSourceRoots(inputEntity.sourceRoots)
    val dummyJavaModuleNames = calculateDummyJavaModuleNames(dummyJavaModuleSourceRoots, projectBasePath)
    val dummyJavaResourcePath = calculateDummyResourceRootPath(inputEntity, dummyJavaModuleSourceRoots, projectBasePath, project)

    return if (BazelFeatureFlags.mergeSourceRoots &&
      canMergeSources(inputEntity.sourceRoots, dummyJavaModuleSourceRoots, dummyJavaResourcePath)
    ) {
      MergedSourceRoots(dummyJavaModuleSourceRoots)
    } else if (!BazelFeatureFlags.addDummyModules) {
      DummyModulesToAdd(emptyList())
    } else if (dummyJavaModuleNames.isEmpty() && dummyJavaResourcePath != null) {
      val dummyModuleName = calculateDummyJavaModuleName(dummyJavaResourcePath, projectBasePath)
      DummyModulesToAdd(
        calculateDummyJavaSourceModuleWithOnlyResources(
          name = dummyModuleName,
          javaModule = inputEntity,
          dummyJavaResourcePath,
        )?.let { listOf(it) } ?: emptyList(),
      )
    } else {
      DummyModulesToAdd(
        dummyJavaModuleSourceRoots
          .zip(dummyJavaModuleNames)
          .mapNotNull {
            calculateDummyJavaSourceModule(
              name = it.second,
              sourceRoot = it.first,
              javaModule = inputEntity,
              resourceRootPath = dummyJavaResourcePath,
            )
          }.distinctBy { it.genericModuleInfo.name },
      )
    }
  }

  private fun canMergeSources(
    sourceRoots: List<JavaSourceRoot>,
    dummyJavaModuleSourceRoots: List<JavaSourceRoot>,
    dummyJavaResourcePath: Path?,
  ): Boolean {
    if (dummyJavaResourcePath != null) return false

    val mergedSourceRoots: Set<Path> = dummyJavaModuleSourceRoots.map { it.sourcePath }.toSet()
    if (mergedSourceRoots.size != dummyJavaModuleSourceRoots.size) {
      // Some of the dummy source roots have the same directory but either different package prefix or different root type (source/test)
      return false
    }
    if (mergedSourceRoots.isOnePathParentOfAnother()) return false

    val originalSourceRoots: Set<Path> = sourceRoots.map { it.sourcePath }.toSet()
    if (originalSourceRoots.any { !it.isUnder(mergedSourceRoots) }) return false

    for (mergedSourceRoot in mergedSourceRoots) {
      Files.walk(mergedSourceRoot).use { children ->
        for (fileUnderRoot in children) {
          if (fileUnderRoot.isDirectory()) continue
          val extension = fileUnderRoot.extension
          if (extension != "java" && extension != "kt" && extension != "scala") continue
          val newSourceFileAdded = fileUnderRoot !in originalSourceRoots
          if (newSourceFileAdded) return false
        }
      }
    }

    return true
  }

  private fun Set<Path>.isOnePathParentOfAnother(): Boolean = any { it.parent.isUnder(this) }

  /**
   * See [com.intellij.openapi.vfs.VfsUtilCore.isUnder]
   */
  private fun Path.isUnder(ancestors: Set<Path>): Boolean = this.allAncestorsSequence().any { it in ancestors }

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
            rootType = JAVA_RESOURCE_ROOT_TYPE,
          ),
        ),
      jvmJdkName = javaModule.jvmJdkName,
      kotlinAddendum = javaModule.kotlinAddendum,
      javaAddendum = javaModule.javaAddendum,
    )
  }

  private fun calculateDummyJavaSourceModule(
    name: String,
    sourceRoot: JavaSourceRoot,
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
      baseDirContentRoot = ContentRoot(path = sourceRoot.sourcePath),
      // For some reason allowing dummy modules to be JAVA_TEST_SOURCE_ROOT_TYPE causes red code on https://github.com/bazelbuild/bazel
      sourceRoots = listOf(sourceRoot.copy(rootType = JAVA_SOURCE_ROOT_TYPE)),
      resourceRoots =
        if (resourceRootPath != null) {
          listOf(
            ResourceRoot(
              resourcePath = resourceRootPath,
              rootType = JAVA_RESOURCE_ROOT_TYPE,
            ),
          )
        } else {
          listOf()
        },
      jvmJdkName = javaModule.jvmJdkName,
      kotlinAddendum = javaModule.kotlinAddendum,
      javaAddendum = javaModule.javaAddendum,
    )
  }
}

private fun calculateDummyResourceRootPath(
  entity: JavaModule,
  dummySources: List<JavaSourceRoot>,
  projectBasePath: Path,
  project: Project,
): Path? {
  if (!project.bazelProjectName.startsWith("hirschgarten")) return null
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

private fun calculateDummyJavaSourceRoots(sourceRoots: List<JavaSourceRoot>): List<JavaSourceRoot> =
  sourceRoots
    .asSequence()
    .filter { !BazelJavaSourceRootEntityUpdater.shouldAddBazelJavaSourceRootEntity(it) }
    .mapNotNull {
      restoreSourceRootFromPackagePrefix(it)
    }.distinct()
    .toList()

private fun restoreSourceRootFromPackagePrefix(sourceRoot: JavaSourceRoot): JavaSourceRoot? {
  if (sourceRoot.sourcePath.notExists() || sourceRoot.sourcePath.isDirectory()) return null
  val packagePrefixPath = sourceRoot.packagePrefix.replace('.', File.separatorChar)
  val sourceParent = sourceRoot.sourcePath.parent.pathString
  val sourceRootString = sourceParent.removeSuffix(packagePrefixPath)
  val sourceRootPath = Path(sourceRootString)
  val packagePrefix =
    if (sourceParent == sourceRootString) {
      sourceRoot.packagePrefix
    } else {
      ""
    }
  return JavaSourceRoot(
    sourcePath = sourceRootPath,
    generated = false,
    packagePrefix = packagePrefix,
    rootType = sourceRoot.rootType,
  )
}

private fun calculateDummyJavaModuleNames(dummyJavaModuleSourceRoots: List<JavaSourceRoot>, projectBasePath: Path): List<String> =
  dummyJavaModuleSourceRoots.map { calculateDummyJavaModuleName(it.sourcePath, projectBasePath) }

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
