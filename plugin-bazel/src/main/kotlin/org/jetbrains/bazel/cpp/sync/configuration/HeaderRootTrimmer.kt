package org.jetbrains.bazel.cpp.sync.configuration

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.bazelrunner.utils.BazelInfo
import org.jetbrains.bazel.commons.ExecutionRootPath
import org.jetbrains.bazel.commons.TargetKey
import org.jetbrains.bazel.cpp.sync.CFileExtensions
import org.jetbrains.bazel.cpp.sync.ExecutionRootPathResolver
import org.jetbrains.bazel.cpp.sync.VfsUtils
import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.sync.task.bazelProject
import java.io.File
import java.util.ArrayDeque
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.MutableSet
import kotlin.text.isEmpty

/**
 * Collects possible -I, -isystem, -iquote search roots and determines which are actually viable.
 *
 *
 * Namely, some of the roots are optimistically in output directories (genfiles, bin), and exist,
 * but may contain no more than aspect files or .cppmaps (does not actually contain headers). In
 * such cases, there is no reason to actually search those roots, and they won't change until the
 * next build/sync (unlike source directories).
 *
 * See com.google.idea.blaze.cpp.HeaderRootTrimmer
 */
internal object HeaderRootTrimmer {
  private val logger: Logger = Logger.getInstance(HeaderRootTrimmer::class.java)

  // Don't recursively check too many directories, in case the root is just too big.
  // Sometimes genfiles/java is considered a header search root.
  private const val GEN_HEADER_ROOT_SEARCH_LIMIT = 50

  fun getValidRoots(
    project: Project,
    toolchainLookupMap: Map<TargetKey, BspTargetInfo.CToolchainInfo>,
    executionRootPathResolver: ExecutionRootPathResolver,
  ): Set<File> {
    // Type specification needed to avoid incorrect type inference during command line build.

    val paths =
      collectExecutionRootPaths(
        project.targetMap(),
        toolchainLookupMap,
      )
    return doCollectHeaderRoots(
      project,
      paths,
      executionRootPathResolver,
    )
  }

  private fun doCollectHeaderRoots(
    project: Project,
    rootPaths: Set<ExecutionRootPath>,
    pathResolver: ExecutionRootPathResolver,
  ): Set<File> {
    val genRootsWithHeaders = AtomicInteger()
    val genRootsWithoutHeaders = AtomicInteger()
    val validRoots =
      runBlocking {
        rootPaths
          .map { path ->
            async {
              val possibleDirectories: List<File> =
                pathResolver.resolveToIncludeDirectories(path)
              if (possibleDirectories.isEmpty()) {
                logger.info(String.format("Couldn't resolve include root: %s", path))
              }
              return@async possibleDirectories.map { file ->
                val vf: VirtualFile? = VfsUtils.resolveVirtualFile(file, true)
                if (vf != null) {
                  // Check gen directories to see if they actually contain headers and not just
                  // other random generated files (like .s, .cc, or module maps).
                  // Also checks bin directories to see if they actually contain headers vs
                  // just aspect files.
                  if (!isOutputArtifact(project.bazelProject.bazelInfo, path)) {
                    return@map file
                  } else if (genRootMayContainHeaders(vf)) {
                    genRootsWithHeaders.incrementAndGet()
                    return@map file
                  } else {
                    genRootsWithoutHeaders.incrementAndGet()
                  }
                } else if (!isOutputArtifact(project.bazelProject.bazelInfo, path) &&
                  file.exists()
                ) {
                  // If it's not a Bazel output file, we expect it to always resolve.
                  logger.info(String.format("Unresolved header root %s", file.absolutePath))
                }
                return@map null
              }
            }
          }.awaitAll()
      }.flatten().filterNotNull().toSet()

    logger.info(
      String.format(
        "CollectHeaderRoots: %s roots, (%s, %s) genroots with/without headers",
        validRoots.size,
        genRootsWithHeaders.get(),
        genRootsWithoutHeaders.get(),
      ),
    )
    return validRoots
  }

  private fun genRootMayContainHeaders(directory: VirtualFile): Boolean {
    var totalDirectoriesChecked = 0
    val worklist = ArrayDeque<VirtualFile>()
    worklist.add(directory)
    while (!worklist.isEmpty()) {
      totalDirectoriesChecked++
      if (totalDirectoriesChecked > GEN_HEADER_ROOT_SEARCH_LIMIT) {
        return true
      }
      val dir: VirtualFile = worklist.poll()
      for (child in dir.getChildren()) {
        if (child.isDirectory()) {
          worklist.add(child)
          continue
        }
        val fileExtension = child.getExtension()
        if (fileExtension == null || fileExtension.isEmpty()) {
          // Conservatively allow extension-less headers (though hopefully rare for generated srcs
          // vs the standard library). Could count extension-less binaries in bin/ directory.
          return true
        }
        if (CFileExtensions.HEADER_EXTENSIONS.contains(fileExtension)) {
          return true
        }
      }
    }
    return false
  }

  private fun isOutputArtifact(bazelInfo: BazelInfo, path: ExecutionRootPath): Boolean {
    return ExecutionRootPath.isAncestor(ExecutionRootPath(bazelInfo.bazelBin), path, false)
    // ExecutionRootPath.isAncestor(bazelInfo. path, false) ||
  }

  private fun collectExecutionRootPaths(
    targetMap: Map<TargetKey, BspTargetInfo.TargetInfo>,
    toolchainLookupMap: Map<TargetKey, BspTargetInfo.CToolchainInfo>,
  ): Set<ExecutionRootPath> {
    val paths: MutableSet<ExecutionRootPath> = mutableSetOf()
    for (target in targetMap.values) {
      if (target.cppTargetInfo != null) {
        paths.addAll(target.cppTargetInfo.transitiveSystemIncludeDirectoryList.map { ExecutionRootPath(it) })
        paths.addAll(target.cppTargetInfo.transitiveIncludeDirectoryList.map { ExecutionRootPath(it) })
        paths.addAll(target.cppTargetInfo.transitiveQuoteIncludeDirectoryList.map { ExecutionRootPath(it) })
      }
    }

    // Builtin includes should not be added to the switch builder, because CLion discovers builtin include paths during
    // the compiler info collection, and therefore it would be safe to filter these header roots. But this would make
    // the filter stricter, and it is unclear if this would affect any users.
    // NOTE: if the toolchain uses an external sysroot, CLion might not be able to discover the all builtin include paths.
    val toolchains: MutableSet<BspTargetInfo.CToolchainInfo> = toolchainLookupMap.values.toMutableSet()
    for (toolchain in toolchains) {
      paths.addAll(toolchain.builtInIncludeDirectoryList.map { ExecutionRootPath(it) })
    }
    return paths
  }
}
