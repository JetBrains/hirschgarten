package org.jetbrains.bazel.languages.starlark.references

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.isFile
import org.jetbrains.bazel.label.Canonical
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.Package
import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bazel.label.SingleTarget
import org.jetbrains.bazel.languages.starlark.repomapping.canonicalRepoNameToPath
import org.jetbrains.bazel.sync.status.SyncStatusListener
import java.nio.file.Path
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator
import kotlin.io.path.relativeToOrNull

@Service(Service.Level.PROJECT)
class BazelFileService(private val project: Project) {
  private var cacheInvalid = true
  private val canonicalRepoNameToBzlFiles = mutableMapOf<String, List<Label>>()

  init {
    project.messageBus.connect().subscribe(
      SyncStatusListener.TOPIC,
      object : SyncStatusListener {
        override fun syncStarted() {}

        override fun syncFinished(canceled: Boolean) {
          cacheInvalid = true
        }
      },
    )
  }


  private inner class FileVisitor(
    private val canonicalName: String,
    private val repoPath: Path,
    private val projectFileIndex: ProjectFileIndex,
    private val map: MutableMap<String, List<Label>>
  ) {
    fun visitFile(file: VirtualFile): Boolean {
      if (file.isFile && !projectFileIndex.isExcluded(file) && file.name.endsWith(".bzl")) {
        val targetName = file.name
        val targetBaseDirectory = file.parent ?: return true
        val relativeTargetBaseDirectory = targetBaseDirectory.toNioPath().relativeToOrNull(repoPath) ?: return true

        val label = ResolvedLabel(
          repo = Canonical.createCanonicalOrMain(canonicalName),
          packagePath = Package(relativeTargetBaseDirectory.toString().split("/")),
          target = SingleTarget(targetName),
        )
        map[canonicalName] = map.getOrDefault(canonicalName, emptyList<Label>()) + label
      }
      return true
    }
  }

  private fun updateCache() {
    cacheInvalid = false
    canonicalRepoNameToBzlFiles.clear()

    val projectFileIndex = ProjectFileIndex.getInstance(project)

    for ((canonicalName, repoPath) in project.canonicalRepoNameToPath) {
      val root = VirtualFileManager.getInstance().findFileByNioPath(repoPath) ?: continue
      val visitor = FileVisitor(canonicalName, repoPath, projectFileIndex, canonicalRepoNameToBzlFiles)
      VfsUtilCore.processFilesRecursively(root, visitor::visitFile)
    }
  }

  fun getApparentRepoNameToFiles(): Map<String, List<Label>> {
    if (cacheInvalid) {
      updateCache()
    }
    return canonicalRepoNameToBzlFiles
  }
}
