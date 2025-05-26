package org.jetbrains.bazel.languages.starlark.references

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.isFile
import org.jetbrains.bazel.languages.starlark.repomapping.canonicalRepoNameToPath
import org.jetbrains.bazel.sync.status.SyncStatusListener
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator

@Service(Service.Level.PROJECT)
class BazelFileService(private val project: Project) {
  private var cacheInvalid = true
  private val canonicalRepoNameToBzlFiles = mutableMapOf<String, List<String>>()

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

  private fun updateCache() {
    cacheInvalid = false
    canonicalRepoNameToBzlFiles.clear()

    val projectFileIndex = ProjectFileIndex.getInstance(project)

    for ((canonicalName, repoPath) in project.canonicalRepoNameToPath) {
      val root = LocalFileSystem.getInstance().findFileByNioFile(repoPath) ?: continue
      VfsUtilCore.processFilesRecursively(root) { file ->
        if (file.isFile && !projectFileIndex.isExcluded(file) && file.name.endsWith(".bzl")) {
          val relativeFilePath = file.path.substring(root.path.length + 1)
          canonicalRepoNameToBzlFiles[canonicalName] =
            canonicalRepoNameToBzlFiles.getOrDefault(canonicalName, emptyList()) + relativeFilePath
        }
        true
      }
    }
  }

  fun getApparentRepoNameToFiles(): Map<String, List<String>> {
    if (cacheInvalid) {
      updateCache()
    }
    return canonicalRepoNameToBzlFiles
  }
}
