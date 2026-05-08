package org.jetbrains.bazel.projectAware

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks BUILD file changes between syncs. When the user clicks "Load Bazel Changes",
 * this service provides the set of changed BUILD files so we can resolve affected targets
 * and perform a partial sync instead of a full sync.
 */
@ApiStatus.Internal
@Service(Service.Level.PROJECT)
class ChangedBuildFilesTracker {
  private val changedBuildFiles = ConcurrentHashMap.newKeySet<Path>()

  fun addChangedFile(path: Path) {
    changedBuildFiles.add(path)
  }

  fun consumeChangedFiles(): Set<Path> {
    val result = changedBuildFiles.toSet()
    changedBuildFiles.clear()
    return result
  }

  companion object {
    fun getInstance(project: Project): ChangedBuildFilesTracker = project.service()
  }
}
