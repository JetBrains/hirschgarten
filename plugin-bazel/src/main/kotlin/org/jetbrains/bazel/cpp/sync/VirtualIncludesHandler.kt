package org.jetbrains.bazel.cpp.sync

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.registry.Registry
import org.jetbrains.bazel.commons.ExecutionRootPath
import org.jetbrains.bazel.commons.TargetKey
import org.jetbrains.bazel.commons.WorkspacePath
import org.jetbrains.bazel.commons.WorkspaceRoot
import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.SingleTarget
import org.jetbrains.bazel.label.TargetType
import java.io.File
import java.nio.file.Path

/**
 * Utility class designed to convert execroot `_virtual_includes` references to
 * either external or local workspace.
 *
 *
 * Virtual includes are generated for targets with strip_include_prefix attribute
 * and are stored for external workspaces in
 *
 *
 * `bazel-out/.../bin/external/.../_virtual_includes/...`
 *
 *
 * or for local workspace in
 *
 *
 * `bazel-out/.../bin/.../_virtual_includes/...`
 *
 * See com.google.idea.blaze.base.sync.workspace.VirtualIncludesHandler
 */
object VirtualIncludesHandler {
  val VIRTUAL_INCLUDES_DIRECTORY: Path? = Path.of("_virtual_includes")

  private val logger: Logger = Logger.getInstance(VirtualIncludesHandler::class.java)
  private val EXTERNAL_DIRECTORY_IDX = 3
  private val EXTERNAL_WORKSPACE_NAME_IDX = 4
  private val WORKSPACE_PATH_START_FOR_EXTERNAL_WORKSPACE = 5
  private val WORKSPACE_PATH_START_FOR_LOCAL_WORKSPACE = 3

  fun useHeuristic(): Boolean = Registry.`is`("bazel.sync.resolve.virtual.includes")

  private fun splitExecutionPath(executionRootPath: ExecutionRootPath): List<Path> = executionRootPath.absoluteOrRelativePath.toList()

  fun containsVirtualInclude(executionRootPath: ExecutionRootPath): Boolean =
    splitExecutionPath(executionRootPath).contains(VIRTUAL_INCLUDES_DIRECTORY)

  fun Label.getBazelPackage(): WorkspacePath {
    val labelStr = toString()
    val startIndex = labelStr.indexOf("//") + "//".length
    val colonIndex = labelStr.lastIndexOf(':')
    return WorkspacePath(labelStr.substring(startIndex, colonIndex))
  }

  fun Label.externalWorkspaceName(): String? {
    val label = toString()
    if (!label.startsWith("@")) {
      return null
    }
    val slashesIndex = label.indexOf("//")
    return label.substring(0, slashesIndex).replaceFirst("@@?".toRegex(), "")
  }

  fun createLabel(
    externalWorkspaceName: String?,
    packagePath: WorkspacePath,
    targetName: TargetType,
  ): Label {
    val fullLabel =
      String.format(
        "%s//%s:%s",
        if (externalWorkspaceName != null) "@$externalWorkspaceName" else "",
        packagePath,
        targetName,
      )
    return Label.parse(fullLabel)
  }

  /**
   * Resolves execution root path to `_virtual_includes` directory to the matching workspace
   * location
   *
   * @return list of resolved paths if required information is obtained from execution root path and
   * target data or empty list if resolution has failed
   */

  fun resolveVirtualInclude(
    executionRootPath: ExecutionRootPath,
    externalWorkspacePath: File?,
    workspaceRoot: WorkspaceRoot,
    targetMap: Map<TargetKey, BspTargetInfo.TargetInfo>,
  ): List<File> {
    val key: TargetKey =
      try {
        guessTargetKey(executionRootPath)
      } catch (exception: IndexOutOfBoundsException) {
        // report to intellij EA
        logger.error(
          "Failed to detect target from execution root path: " + executionRootPath,
          exception,
        )
        null
      } ?: return emptyList()

    val info = targetMap[key] ?: return emptyList()

    if (info.sourcesList.any { !it.isSource }) {
      // target contains generated sources which cannot be found in the project root, fallback to virtual include directory
      return emptyList()
    }
    if (!info.hasCppTargetInfo()) return emptyList()
    val cIdeInfo = info.cppTargetInfo

    if (cIdeInfo.includePrefix.isNotEmpty()) {
      // it is not possible to handle include prefixes here, fallback to virtual include directory
      return emptyList()
    }
    // strip prefix is a path not a label, `//something` is invalid
    // remove trailing slash
    val stripPrefix = cIdeInfo.stripIncludePrefix.replace("/+", "/").removeSuffix("/")
    if (stripPrefix.isBlank()) {
      return emptyList()
    }

    val workspacePath =
      if (stripPrefix.startsWith("/")) {
        WorkspacePath(stripPrefix.substring(1))
      } else {
        WorkspacePath(key.label.getBazelPackage(), stripPrefix)
      }
    // inlined from `return workspacePathResolver.resolveToIncludeDirectories(workspacePath)`
    val externalWorkspace = key.label.externalWorkspaceName() ?: return listOf(workspaceRoot.fileForPath(workspacePath).toFile())

    val externalRoot =
      ExecutionRootPathResolver.externalPath
        .toPath()
        .resolve(externalWorkspace)
        .resolve(workspacePath.toString())
        .toString()

    return listOf(ExecutionRootPath(externalRoot).getFileRootedAt(externalWorkspacePath))
  }

  /**
   * @throws IndexOutOfBoundsException if executionRootPath has _virtual_includes but its content is
   * unexpected
   */
  fun guessTargetKey(executionRootPath: ExecutionRootPath): TargetKey? {
    val split: List<Path> = splitExecutionPath(executionRootPath)
    val virtualIncludesIdx = split.indexOf(VIRTUAL_INCLUDES_DIRECTORY)

    if (virtualIncludesIdx > -1) {
      val externalWorkspaceName =
        if (split[EXTERNAL_DIRECTORY_IDX] == ExecutionRootPathResolver.externalPath.toPath()) {
          split[EXTERNAL_WORKSPACE_NAME_IDX].toString()
        } else {
          null
        }

      val workspacePathStart =
        if (externalWorkspaceName != null) {
          WORKSPACE_PATH_START_FOR_EXTERNAL_WORKSPACE
        } else {
          WORKSPACE_PATH_START_FOR_LOCAL_WORKSPACE
        }

      val workspacePaths: List<Path> =
        if (workspacePathStart <= virtualIncludesIdx) {
          split.subList(workspacePathStart, virtualIncludesIdx)
        } else {
          emptyList()
        }

      val workspacePathString: String =
        FileUtil.toSystemIndependentName(
          workspacePaths.stream().reduce(Path.of(""), Path::resolve).toString(),
        )

      val target = SingleTarget(split[virtualIncludesIdx + 1].toString())
      val workspacePath = WorkspacePath.createIfValid(workspacePathString) ?: return null

      // inlined from TargetKey.forPlainTarget(Label.create(externalWorkspaceName, workspacePath, target), )
      return TargetKey(createLabel(externalWorkspaceName, workspacePath, target), listOf())
    } else {
      return null
    }
  }
}
