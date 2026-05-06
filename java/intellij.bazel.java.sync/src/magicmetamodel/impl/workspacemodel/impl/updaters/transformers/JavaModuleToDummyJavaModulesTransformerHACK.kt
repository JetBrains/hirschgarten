package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.openapi.vfs.refreshAndFindVirtualFileOrDirectory
import com.intellij.platform.workspace.jps.entities.ModuleTypeId
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.magicmetamodel.sanitizeName
import org.jetbrains.bazel.magicmetamodel.shortenTargetPath
import org.jetbrains.bazel.utils.findVirtualFile
import org.jetbrains.bazel.workspacemodel.entities.ContentRoot
import org.jetbrains.bazel.workspacemodel.entities.GenericModuleInfo
import org.jetbrains.bazel.workspacemodel.entities.JavaModule
import org.jetbrains.bazel.workspacemodel.entities.JavaSourceRoot
import org.jetbrains.bsp.protocol.StrictDependencyCheckedType
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.io.path.pathString

private val log = logger<JavaModuleToDummyJavaModulesTransformerHACK>()

/**
 * This is a HACK for letting single source Java files to be resolved normally
 * Should remove soon and replace with a more robust solution
 */
@ApiStatus.Internal
class JavaModuleToDummyJavaModulesTransformerHACK(
  private val projectBasePath: Path,
  private val fileToTargets: Map<Path, List<Label>>,
) {
  sealed interface Result

  data class DummyModulesToAdd(val dummyModules: List<JavaModule>) : Result

  data class MergedRoots(val mergedSourceRoots: List<JavaSourceRoot>) : Result

  fun transform(inputEntity: JavaModule): Result {
    val buildFileDirectory = inputEntity.baseDirContentRoot?.path
    val (relevantSourceRoots, irrelevantSourceRoots) = inputEntity.sourceRoots.partition { it.isRelevant() }
    val sourceRootsForParentDirs = calculateSourceRootsForParentDirs(relevantSourceRoots)
    val relevantSourceRootFiles = relevantSourceRoots.mapNotNullTo(mutableSetOf()) { it.sourcePath.findOrRefreshVirtualFile() }
    val mergedSourceRootVotes = sourceRootsForParentDirs
      .restoreSourceRootFromPackagePrefix(
        relevantExtensions = Constants.JVM_LANGUAGES_EXTENSIONS,
        limit = buildFileDirectory,
        knownFiles = relevantSourceRootFiles,
      ).preferShorterPrefix()

    if (BazelFeatureFlags.mergeSourceRoots) {
      val mergedSourceRoots =
        tryMergeSources(
          sourceRootFiles = relevantSourceRootFiles,
          mergeSourceRootVotes = mergedSourceRootVotes,
          sourceRootsForParentDirsVotes = sourceRootsForParentDirs,
        )
      if (mergedSourceRoots != null) {
        return MergedRoots(mergedSourceRoots = mergedSourceRoots + irrelevantSourceRoots)
      }
    }
    val dummySourceRoots =
      if (buildFileDirectory == null) {
        mergedSourceRootVotes
      }
      else {
        mergedSourceRootVotes.restoreSourceRootFromPackagePrefix(
          relevantExtensions = Constants.JVM_LANGUAGES_EXTENSIONS,
          knownFiles = relevantSourceRootFiles,
        )
      }.keys.toList()
    return DummyModulesToAdd(
      dummySourceRoots
        .map { root ->
          calculateDummyJavaSourceModule(
            name = calculateDummyJavaModuleName(root.sourcePath, projectBasePath),
            sourceRoot = root,
            javaModule = inputEntity,
          )
        }.distinctBy { it.genericModuleInfo.name },
    )
  }

  private fun JavaSourceRoot.isRelevant(): Boolean =
    this.sourcePath.extension in Constants.JVM_LANGUAGES_EXTENSIONS || this.sourcePath.isDirectory()

  private fun tryMergeSources(
    sourceRootFiles: Set<VirtualFile>,
    mergeSourceRootVotes: Map<JavaSourceRoot, Int>,
    sourceRootsForParentDirsVotes: Map<JavaSourceRoot, Int>,
  ): List<JavaSourceRoot>? {
    if (sourceRootFiles.any { it.isSharedBetweenSeveralTargets() }) {
      return null
    }

    return tryMergeSources(sourceRootFiles, mergeSourceRootVotes) ?: tryMergeSources(sourceRootFiles, sourceRootsForParentDirsVotes)
  }

  private fun tryMergeSources(originalSourceRoots: Set<VirtualFile>, mergeSourceRootVotes: Map<JavaSourceRoot, Int>): List<JavaSourceRoot>? {
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
    val mergedSourceRootFiles = mutableSetOf<VirtualFile>()
    val parentsOfMergedSourceRoots = mutableSetOf<VirtualFile>()

    for (sourceRoot in sourceRootsSortedByVotes) {
      val sourceRootFile = sourceRoot.sourcePath.findOrRefreshVirtualFile() ?: continue
      // Make sure no source path is a parent of another one
      if (VfsUtilCore.isUnder(sourceRootFile, mergedSourceRootFiles)) continue
      if (sourceRootFile in parentsOfMergedSourceRoots) continue

      mergedSourceRoots.add(sourceRoot)
      mergedSourceRootFiles.add(sourceRootFile)
      parentsOfMergedSourceRoots.addAll(sourceRootFile.allAncestorsSequence())
    }

    if (originalSourceRoots.any { !VfsUtilCore.isUnder(it, mergedSourceRootFiles) }) return null

    if (mergedRootsCoverNewFiles(
        mergedRoots = mergedSourceRootFiles,
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
  private fun VirtualFile.isSharedBetweenSeveralTargets(): Boolean = (fileToTargets[toNioPath()]?.size ?: 0) > 1

  /**
   * @param relevantExtensions consider new files only with the specified extensions, or `null` to consider all new files
   */
  private fun mergedRootsCoverNewFiles(
    mergedRoots: Collection<VirtualFile>,
    originalRoots: Set<VirtualFile>,
    relevantExtensions: List<String>,
  ): Boolean = mergedRoots.any { it.containsUnknownRelevantFile(originalRoots, relevantExtensions) }

  private fun calculateDummyJavaSourceModule(
    name: String,
    sourceRoot: JavaSourceRoot,
    javaModule: JavaModule,
  ) = JavaModule(
    genericModuleInfo =
      GenericModuleInfo(
        label = javaModule.genericModuleInfo.label,
        name = name,
        type = ModuleTypeId(BazelDummyModuleType.ID),
        kind =
          TargetKind(
            kind = "java_library",
            ruleType = RuleType.LIBRARY,
            languageClasses = setOf(LanguageClass.JAVA, LanguageClass.SCALA, LanguageClass.KOTLIN),
          ),
        strictDependenciesCheck = StrictDependencyCheckedType.OFF,
        dependencies = emptyList(),
        strictDependencies = emptyList(),
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

  /**
   * Returns a map from a restored source root to the number of "votes", i.e., the number of original source files that "voted" for that root.
   */
  private fun calculateSourceRootsForParentDirs(sourceRoots: List<JavaSourceRoot>): Map<JavaSourceRoot, Int> =
    sourceRoots
      .asSequence()
      .filter { root -> !root.generated && root.sourcePath.startsWith(projectBasePath) }
      .mapNotNull {
        sourceRootForParentDir(it)
      }.groupingBy { it }
      .eachCount()
}

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

private fun Map<JavaSourceRoot, Int>.restoreSourceRootFromPackagePrefix(
  relevantExtensions: List<String>,
  limit: Path? = null,
  knownFiles: Set<VirtualFile> = emptySet(),
): Map<JavaSourceRoot, Int> = this
    .map { (sourceRoot, votes) -> sourceRoot.restoreSourceRootFromPackagePrefix(relevantExtensions, limit, knownFiles) to votes }
    .sumUpVotes()

private fun JavaSourceRoot.restoreSourceRootFromPackagePrefix(
  relevantExtensions: List<String>,
  limit: Path? = null,
  knownFiles: Set<VirtualFile> = emptySet(),
): JavaSourceRoot {
  val segments = this.packagePrefix.split('.').toMutableList()
  var sourcePath: Path = this.sourcePath
  while (sourcePath != limit && segments.lastOrNull() == sourcePath.name) {
    sourcePath.parent ?: break
    if (sourcePath.siblingsContainUnknownRelevantFiles(relevantExtensions, knownFiles)) break
    sourcePath = sourcePath.parent
    segments.removeLast()
  }
  return copy(sourcePath = sourcePath, packagePrefix = segments.joinToString("."))
}

private fun Path.siblingsContainUnknownRelevantFiles(
  relevantExtensions: List<String>,
  knownFiles: Set<VirtualFile>,
): Boolean {
  val dir = parent.findOrRefreshVirtualFile() ?: return false
  val thisFile = this.findOrRefreshVirtualFile()
  return dir.children.any { it != thisFile && it.containsUnknownRelevantFile(knownFiles, relevantExtensions) }
}

private fun VirtualFile.containsUnknownRelevantFile(knownFiles: Set<VirtualFile>, relevantExtensions: List<String>): Boolean {
  var found = false
  VfsUtilCore.visitChildrenRecursively(this, object : VirtualFileVisitor<Unit>(NO_FOLLOW_SYMLINKS) {
    override fun visitFile(file: VirtualFile): Boolean {
      if (file.isDirectory || file.extension !in relevantExtensions || file in knownFiles) return true
      found = true
      return false
    }
  })
  return found
}

private fun Path.findOrRefreshVirtualFile():  VirtualFile? {
  val file = this.findVirtualFile() ?: this.refreshAndFindVirtualFileOrDirectory()
  if (file == null) log.warn("Failed to find or refresh virtual file for path: $this!")
  return file
}

private fun VirtualFile.allAncestorsSequence(): Sequence<VirtualFile> = generateSequence(this) { it.parent }

private fun Iterable<Pair<JavaSourceRoot, Int>>.sumUpVotes(): Map<JavaSourceRoot, Int> {
  val result = mutableMapOf<JavaSourceRoot, Int>()
  for ((sourceRoot, votes) in this) {
    result[sourceRoot] = votes + result.getOrDefault(sourceRoot, 0)
  }
  return result
}

/**
 * Motivated by BAZEL-3050
 * It's only relevant when there is a mix of Kotlin and Java classes - like in the issue.
 * For example, when Kotlin sources are inside `com.example.foo` pacakge and Java sources are inside `foo` package, we prefer `foo`.
 * Without this fix, we would prefer `com.example.foo` which leads to red references to Java code from `foo` package.
 */
private fun Map<JavaSourceRoot, Int>.preferShorterPrefix(): Map<JavaSourceRoot, Int> {
  val grouped = entries.groupBy { (root, _) -> root.sourcePath to root.rootType }
  return grouped.flatMap { (_, group) ->
    if (group.size <= 1) return@flatMap group.map { it.toPair() }

    val sorted = group.sortedByDescending { (_, votes) -> votes }
    val first = sorted[0].key.packagePrefix
    val second = sorted[1].key.packagePrefix

    if (second.isEmpty() || first.endsWith(".$second")) {
      listOf(sorted[1].key to group.sumOf { (_, votes) -> votes })
    } else {
      group.map { it.toPair() }
    }
  }.toMap()
}

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
  }
  else {
    "$IJ_DUMMY_MODULE_PREFIX.$this"
  }
