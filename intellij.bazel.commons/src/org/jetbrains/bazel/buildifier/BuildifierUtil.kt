package org.jetbrains.bazel.buildifier

import com.intellij.openapi.project.Project
import kotlinx.coroutines.runBlocking
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.sync.BazelEnvironmentService
import kotlin.io.path.absolutePathString

@ApiStatus.Internal
object BuildifierUtil {
  fun detectBuildifierExecutable(project: Project): String? {
    // runBlockingCancellable is not allowed inside a write action, which causes Starlark formatting to throw exceptions
    // Also, BazelEnvironmentService starts the calculation on startup (see BazelEnvironmentService$StartupActivity),
    // so by the time this function is called it should've been finished already
    return runBlocking {
      BazelEnvironmentService.getInstance(project).findInPath("buildifier")?.absolutePathString()
    }
  }
}
