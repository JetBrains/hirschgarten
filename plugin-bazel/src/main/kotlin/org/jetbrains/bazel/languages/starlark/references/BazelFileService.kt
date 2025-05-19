package org.jetbrains.bazel.languages.starlark.references

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.bazel.languages.starlark.repomapping.canonicalRepoNameToPath
import org.jetbrains.bazel.sync.ProjectSyncHook
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator

@Service(Service.Level.PROJECT)
class BazelFileService(private val project: Project) : ProjectSyncHook {
  private var cacheInvalid = true
  private val canonicalRepoNameToBzlFiles = mutableMapOf<String, List<String>>()

  override suspend fun onSync(environment: ProjectSyncHook.ProjectSyncHookEnvironment) {
    cacheInvalid = true
  }

  private fun collectBzlFiles(
    directory: VirtualFile,
    result: MutableList<String>,
    projectFileIndex: ProjectFileIndex,
  ) {
    if (projectFileIndex.isExcluded(directory)) return

    directory.children.forEach { file ->
      when {
        file.isDirectory -> {
          collectBzlFiles(file, result, projectFileIndex)
        }
        file.name.endsWith(".bzl") -> {
          result.add(file.path)
        }
      }
    }
  }

  private fun updateCache() {
    cacheInvalid = false
    canonicalRepoNameToBzlFiles.clear()

    val projectFileIndex = ProjectFileIndex.getInstance(project)

    for ((canonicalName, repoPath) in project.canonicalRepoNameToPath) {
      val virtualFile = LocalFileSystem.getInstance().findFileByNioFile(repoPath) ?: continue
      val result = mutableListOf<String>()
      collectBzlFiles(virtualFile, result, projectFileIndex)
      result.forEach { file ->
        val relativeFilePath = file.substring(virtualFile.path.length + 1)
        canonicalRepoNameToBzlFiles[canonicalName] =
          canonicalRepoNameToBzlFiles.getOrDefault(canonicalName, emptyList()) + relativeFilePath
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
