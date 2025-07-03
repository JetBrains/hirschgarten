package org.jetbrains.bazel.languages.starlark.references

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.isFile
import org.jetbrains.annotations.TestOnly
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
  @Volatile
  var canonicalRepoNameToBzlFiles: Map<String, List<ResolvedLabel>> = emptyMap()

  init {
    project.messageBus.connect().subscribe(
      SyncStatusListener.TOPIC,
      object : SyncStatusListener {
        override fun syncStarted() {}

        override fun syncFinished(canceled: Boolean) {
          if (canceled) return
          ApplicationManager.getApplication().executeOnPooledThread { updateCache() }
        }
      },
    )
  }

  private inner class FileVisitor(
    private val canonicalName: String,
    private val repoPath: Path,
    private val map: MutableMap<String, List<ResolvedLabel>>,
  ) {
    fun visitFile(file: VirtualFile): Boolean {
      if (file.isFile && file.name.endsWith(".bzl")) {
        val targetName = file.name
        val targetBaseDirectory = file.parent ?: return true
        val relativeTargetBaseDirectory = targetBaseDirectory.toNioPath().relativeToOrNull(repoPath) ?: return true

        val label =
          ResolvedLabel(
            repo = Canonical.createCanonicalOrMain(canonicalName),
            packagePath = Package(relativeTargetBaseDirectory.toString().split("/")),
            target = SingleTarget(targetName),
          )
        map[canonicalName] = map.getOrDefault(canonicalName, emptyList()) + label
      }
      return true
    }
  }

  private fun updateCache() {
    val newMap = mutableMapOf<String, List<ResolvedLabel>>()
    for ((canonicalName, repoPath) in project.canonicalRepoNameToPath) {
      val root = VirtualFileManager.getInstance().findFileByNioPath(repoPath) ?: continue
      val visitor = FileVisitor(canonicalName, repoPath, newMap)
      VfsUtilCore.processFilesRecursively(root, visitor::visitFile)
    }
    canonicalRepoNameToBzlFiles = newMap
  }

  fun getApparentRepoNameToFiles(): Map<String, List<Label>> = canonicalRepoNameToBzlFiles

  @TestOnly
  fun forceUpdateCache() {
    updateCache()
  }

  companion object {
    fun getInstance(project: Project): BazelFileService = project.getService(BazelFileService::class.java)
  }
}
