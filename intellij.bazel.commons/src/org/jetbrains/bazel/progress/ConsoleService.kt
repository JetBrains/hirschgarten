package org.jetbrains.bazel.progress

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
interface ConsoleService {
  val buildConsole: TaskConsole
  val syncConsole: TaskConsole

  companion object {
    @JvmStatic
    fun getInstance(project: Project): ConsoleService = project.service<ConsoleService>()
  }
}
