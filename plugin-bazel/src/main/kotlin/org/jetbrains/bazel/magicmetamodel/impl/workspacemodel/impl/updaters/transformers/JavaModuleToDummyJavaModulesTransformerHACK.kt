package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import com.intellij.openapi.project.Project
import com.intellij.platform.workspace.jps.entities.ModuleTypeId
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.BazelJavaSourceRootEntityUpdater
import org.jetbrains.bazel.magicmetamodel.sanitizeName
import org.jetbrains.bazel.magicmetamodel.shortenTargetPath
import org.jetbrains.bazel.utils.allAncestorsSequence
import org.jetbrains.bazel.utils.commonAncestor
import org.jetbrains.bazel.utils.filterPathsThatDontContainEachOther
import org.jetbrains.bazel.utils.isUnder
import org.jetbrains.bazel.workspacemodel.entities.ContentRoot
import org.jetbrains.bazel.workspacemodel.entities.GenericModuleInfo
import org.jetbrains.bazel.workspacemodel.entities.JavaModule
import org.jetbrains.bazel.workspacemodel.entities.JavaSourceRoot
import org.jetbrains.bazel.workspacemodel.entities.ResourceRoot
import java.io.File
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.io.path.pathString

/**
 * This is a HACK for letting single source Java files to be resolved normally
 * Should remove soon and replace with a more robust solution
 */
class JavaModuleToDummyJavaModulesTransformerHACK(
  private val projectBasePath: Path,
  private val fileToTargetWithoutLowPrioritySharedSources: Map<Path, List<Label>>,
  private val project: Project,
) {
  sealed interface Result

  data class DummyModulesToAdd(val dummyModules: List<JavaModule>) : Result

  data class MergedRoots(val mergedSourceRoots: List<JavaSourceRoot>, val mergedResourceRoots: List<ResourceRoot>?) : Result

  fun transform(inputEntity: JavaModule): Result {
    val buildFileDirectory = inputEntity.baseDirContentRoot?.path
    val (relevantSourceRoots, irrelevantSourceRoots) = inputEntity.sourceRoots.partition { it.isRelevant() }
    val sourceRootsForParentDirs = calculateSourceRootsForParentDirs(relevantSourceRoots)
    val mergedSourceRootVotes = sourceRootsForParentDirs.restoreSourceRootFromPackagePrefix(limit = buildFileDirectory)

    if (BazelFeatureFlags.mergeSourceRoots) {
      val mergedSourceRoots =
        tryMergeSources(
          relevantSourceRoots,
          mergedSourceRootVotes,
          sourceRootsForParentDirs,
        )
      if (mergedSourceRoots != null) {
        val mergedResourceRoots = tryMergeResources(inputEntity.resourceRoots)
        return MergedRoots(
          mergedSourceRoots = mergedSourceRoots + irrelevantSourceRoots,
          mergedResourceRoots = mergedResourceRoots,
        )
      }
    }
    val dummySourceRoots =
      if (buildFileDirectory == null) {
        mergedSourceRootVotes
      } else {
        mergedSourceRootVotes.restoreSourceRootFromPackagePrefix(limit = null)
      }.keys.toList()
    return DummyModulesToAdd(
      dummySourceRoots
        .zip(calculateDummyJavaModuleNames(dummySourceRoots, projectBasePath))
        .mapNotNull {
          calculateDummyJavaSourceModule(
            name = it.second,
            sourceRoot = it.first,
            javaModule = inputEntity,
          )
        }.distinctBy { it.genericModuleInfo.name },
    )
  }

  private fun JavaSourceRoot.isRelevant(): Boolean = this.sourcePath.extension in Constants.JVM_LANGUAGES_EXTENSIONS || this.sourcePath.isDirectory()

  private fun tryMergeSources(
    sourceRoots: List<JavaSourceRoot>,
    mergeSourceRootVotes: Map<JavaSourceRoot, Int>,
    sourceRootsForParentDirsVotes: Map<JavaSourceRoot, Int>,
  ): List<JavaSourceRoot>? {
    val originalSourceRoots: Set<Path> = sourceRoots.map { it.sourcePath }.toSet()
    if (originalSourceRoots.any { it.isSharedBetweenSeveralTargets() }) {
      return null
    }

    return tryMergeSources(originalSourceRoots, mergeSourceRootVotes) ?: tryMergeSources(originalSourceRoots, sourceRootsForParentDirsVotes)
  }

  private fun tryMergeSources(originalSourceRoots: Set<Path>, mergeSourceRootVotes: Map<JavaSourceRoot, Int>): List<JavaSourceRoot>? {
    val sourceRootsSortedByVotes: List<JavaSourceRoot> =
      mergeSourceRootVotes.entries
        // Prefer test roots over production roots, e.g., if some utility target is in the same package as a java_test
        .sortedBy { (sourceRoot, _) -> sourceRoot.rootType != JAVA_TEST_SOURCE_ROOT_TYPE }
        // Sort by the number of segments in the source root path, so that if two source roots have the same number of votes,
        // then we choose the root that's higher in the tree to break the tie.
        .sortedBy { (sourceRoot, _) -> sourceRoot.sourcePath.nameCount }
        // Finally, sort by votes as the main criterion. This "reverse" order of sorts is intended:
        // See https://en.wikipedia.org/wiki/Sorting_algorithm#Stability:~:text=primary%20and%20secondary%20key
        .sortedByDescending { (_, votes) -> votes }
        .map { (sourceRoot, _) -> sourceRoot }
    val mergedSourceRoots = mutableListOf<JavaSourceRoot>()
    val mergedSourceRootPaths = mutableSetOf<Path>()
    val parentsOfMergedSourceRoots = mutableSetOf<Path>()

    for (sourceRoot in sourceRootsSortedByVotes) {
      val sourcePath = sourceRoot.sourcePath
      // Make sure no source path is a parent of another one
      if (sourcePath.isUnder(mergedSourceRootPaths)) continue
      if (sourcePath in parentsOfMergedSourceRoots) continue

      mergedSourceRoots.add(sourceRoot)
      mergedSourceRootPaths.add(sourcePath)
      parentsOfMergedSourceRoots.addAll(sourcePath.allAncestorsSequence())
    }

    if (originalSourceRoots.any { !it.isUnder(mergedSourceRootPaths) }) return null

    if (mergedRootsCoverNewFiles(
        mergedRoots = mergedSourceRootPaths,
        originalRoots = originalSourceRoots,
        relevantExtensions = Constants.JVM_LANGUAGES_EXTENSIONS,
      )
    ) {
      return null
    }

    return mergedSourceRoots
  }

  /**
   * If after merging sources, one source root becomes a parent of another one, then
   * IDEA will only consider the inner source root because of how the workspace model works.
   * This can cause red code, e.g., on https://github.com/bazelbuild/bazel
   */
  private fun Path.isSharedBetweenSeveralTargets(): Boolean = (fileToTargetWithoutLowPrioritySharedSources[this]?.size ?: 0) > 1

  private fun tryMergeResources(resourceRoots: List<ResourceRoot>): List<ResourceRoot>? {
    if (resourceRoots.isEmpty()) return emptyList()
    val rootType = resourceRoots.first().rootType

    val resourceRootPaths = resourceRoots.map { it.resourcePath }
    val resourceRootPathSet = resourceRootPaths.toSet()
    val commonAncestor = resourceRootPaths.commonAncestor()?.takeIf { it.isDirectory() }

    if (commonAncestor != null && !mergedRootsCoverNewFiles(listOf(commonAncestor), resourceRootPathSet)) {
      return listOf(ResourceRoot(commonAncestor, rootType))
    }

    val parentDirectories = resourceRootPaths.map { it.parent }.toSet().filterPathsThatDontContainEachOther()
    if (!mergedRootsCoverNewFiles(parentDirectories, resourceRootPathSet)) {
      return parentDirectories.map { ResourceRoot(it, rootType) }
    }
    return null
  }

  /**
   * @param relevantExtensions consider new files only with the specified extensions, or `null` to consider all new files
   */
  private fun mergedRootsCoverNewFiles(
    mergedRoots: Collection<Path>,
    originalRoots: Set<Path>,
    relevantExtensions: List<String>? = null,
  ): Boolean {
    for (mergedRoot in mergedRoots) {
      val childrenStream = try {
        Files.walk(mergedRoot)
      } catch (_: NoSuchFileException) {
        return true
      }
      childrenStream.use { children ->
        for (fileUnderRoot in children) {
          if (fileUnderRoot.isDirectory()) continue
          if (relevantExtensions != null) {
            val extension = fileUnderRoot.extension
            if (extension !in relevantExtensions) continue
          }
          val newSourceFileCovered = fileUnderRoot !in originalRoots
          if (newSourceFileCovered) return true
        }
      }
    }
    return false
  }

  private fun calculateDummyJavaSourceModule(
    name: String,
    sourceRoot: JavaSourceRoot,
    javaModule: JavaModule,
  ) = if (name.isEmpty()) {
    null
  } else {
    JavaModule(
      genericModuleInfo =
        GenericModuleInfo(
          name = name,
          type = ModuleTypeId(BazelDummyModuleType.ID),
          kind =
            TargetKind(
              kindString = "java_library",
              ruleType = RuleType.LIBRARY,
              languageClasses = setOf(LanguageClass.JAVA, LanguageClass.SCALA, LanguageClass.KOTLIN),
            ),
          dependencies =
            if (!BazelFeatureFlags.fbsrSupportedInPlatform) {
              javaModule.genericModuleInfo.dependencies
            } else {
              emptyList()
            },
          isDummy = true,
        ),
      baseDirContentRoot = ContentRoot(path = sourceRoot.sourcePath),
      // For some reason allowing dummy modules to be JAVA_TEST_SOURCE_ROOT_TYPE causes red code on https://github.com/bazelbuild/bazel
      sourceRoots = listOf(sourceRoot.copy(rootType = JAVA_SOURCE_ROOT_TYPE)),
      resourceRoots = emptyList(),
      jvmJdkName = javaModule.jvmJdkName,
      kotlinAddendum = javaModule.kotlinAddendum,
      javaAddendum = javaModule.javaAddendum,
    )
  }
}

/**
 * Returns a map from a restored source root to the number of "votes", i.e., the number of original source files that "voted" for that root.
 */
private fun calculateSourceRootsForParentDirs(sourceRoots: List<JavaSourceRoot>): Map<JavaSourceRoot, Int> =
  sourceRoots
    .asSequence()
    .filter { root -> !root.generated && !BazelJavaSourceRootEntityUpdater.shouldAddBazelJavaSourceRootEntity(root) }
    .mapNotNull {
      sourceRootForParentDir(it)
    }.groupingBy { it }
    .eachCount()

private fun sourceRootForParentDir(sourceRoot: JavaSourceRoot): JavaSourceRoot? {
  if (sourceRoot.sourcePath.isDirectory()) return null
  val sourceParent = sourceRoot.sourcePath.parent.pathString
  val sourceRootPath = Path(sourceParent)
  val packagePrefix = sourceRoot.packagePrefix
  return JavaSourceRoot(
    sourcePath = sourceRootPath,
    generated = false,
    packagePrefix = packagePrefix,
    rootType = sourceRoot.rootType,
  )
}

private fun Map<JavaSourceRoot, Int>.restoreSourceRootFromPackagePrefix(limit: Path?): Map<JavaSourceRoot, Int> =
  map { (sourceRoot, votes) ->
    sourceRoot.restoreSourceRootFromPackagePrefix(limit) to votes
  }.sumUpVotes()

private fun JavaSourceRoot.restoreSourceRootFromPackagePrefix(limit: Path?): JavaSourceRoot {
  val segments = this.packagePrefix.split('.').toMutableList()
  var sourcePath: Path = this.sourcePath
  while (sourcePath != limit && segments.lastOrNull() == sourcePath.name) {
    sourcePath = sourcePath.parent ?: break
    segments.removeLast()
  }
  return copy(sourcePath = sourcePath, packagePrefix = segments.joinToString("."))
}

private fun Iterable<Pair<JavaSourceRoot, Int>>.sumUpVotes(): Map<JavaSourceRoot, Int> {
  val result = mutableMapOf<JavaSourceRoot, Int>()
  for ((sourceRoot, votes) in this) {
    result[sourceRoot] = votes + result.getOrDefault(sourceRoot, 0)
  }
  return result
}

private fun calculateDummyJavaModuleNames(dummyJavaModuleSourceRoots: List<JavaSourceRoot>, projectBasePath: Path): List<String> =
  dummyJavaModuleSourceRoots.map { calculateDummyJavaModuleName(it.sourcePath, projectBasePath) }

fun calculateDummyJavaModuleName(sourceRoot: Path, projectBasePath: Path): String {
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

fun String.addIntelliJDummyPrefix(): String =
  if (isBlank()) {
    IJ_DUMMY_MODULE_PREFIX
  } else {
    "$IJ_DUMMY_MODULE_PREFIX.$this"
  }
