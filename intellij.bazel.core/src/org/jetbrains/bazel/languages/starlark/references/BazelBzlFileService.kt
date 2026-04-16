package org.jetbrains.bazel.languages.starlark.references

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.VirtualFileVisitor
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.label.Canonical
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.Package
import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bazel.label.SingleTarget
import org.jetbrains.bazel.sync.SyncCache
import org.jetbrains.bazel.workspace.canonicalRepoNameToPath
import org.jetbrains.bazel.workspace.excludedRoots
import java.nio.file.Path
import kotlin.io.path.relativeToOrNull

@ApiStatus.Internal
fun getCanonicalRepoNameToBzlFiles(project: Project): Map<String, List<Label>> =
  SyncCache.getInstance(project).get(canonicalRepoNameToBzlFilesValue)

private val canonicalRepoNameToBzlFilesValue =
  SyncCache.SyncCacheComputable { project ->
    calculateApparentRepoNameToFiles(project)
  }

private fun calculateApparentRepoNameToFiles(project: Project): Map<String, List<ResolvedLabel>> {
  val newMap = mutableMapOf<String, List<ResolvedLabel>>()

  val canonicalRepoPaths =
    project.canonicalRepoNameToPath.values
      .mapNotNull {
        VirtualFileManager.getInstance().findFileByNioPath(it)
      }.toSet()

  val excludedRoots = project.excludedRoots() ?: return emptyMap()

  for ((canonicalName, repoPath) in project.canonicalRepoNameToPath) {
    val root = VirtualFileManager.getInstance().findFileByNioPath(repoPath) ?: continue
    val dirToPackagePath = mutableMapOf<VirtualFile, Path?>()
    VfsUtilCore.visitChildrenRecursively(
      root,
      object : VirtualFileVisitor<Unit>() {
        override fun visitFileEx(file: VirtualFile): Result {
          if (file in excludedRoots) return SKIP_CHILDREN
          if (file != root && file in canonicalRepoPaths) return SKIP_CHILDREN
          if (file.isDirectory) {
            findBuildFilePathForDirectory(file, root, dirToPackagePath)
            return CONTINUE
          }
          if (file.isDirectory || file.extension != "bzl") return CONTINUE

          val packagePath = findBuildFilePathForDirectory(file.parent, root, dirToPackagePath)?.parent ?: return CONTINUE
          val packageName = packagePath.relativeToOrNull(repoPath) ?: return CONTINUE
          val targetName = file.toNioPath().relativeToOrNull(packagePath)

          val label =
            ResolvedLabel(
              repo = Canonical.createCanonicalOrMain(canonicalName),
              packagePath = Package(packageName.toString().split("/")),
              target = SingleTarget(targetName.toString()),
            )
          newMap[canonicalName] = newMap.getOrDefault(canonicalName, emptyList()) + label
          return CONTINUE
        }
      },
    )
  }
  return newMap
}
