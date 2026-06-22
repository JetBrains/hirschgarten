package org.jetbrains.bazel.ignore

import com.intellij.openapi.components.service
import org.jetbrains.annotations.ApiStatus
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.toNioPathOrNull
import java.nio.file.Path

@ApiStatus.Internal
interface BazelIgnoreService {
  fun isIgnored(path: Path): Boolean

  fun isIgnored(file: VirtualFile): Boolean = isIgnored(Path.of(file.path))

  companion object {
    @JvmStatic
    fun getInstance(project: Project): BazelIgnoreService = project.service<BazelIgnoreService>()
  }
}
