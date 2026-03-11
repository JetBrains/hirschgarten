package org.jetbrains.bazel.ui.console

import com.intellij.build.BuildViewManager
import com.intellij.build.SyncViewManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.progress.ConsoleService
import org.jetbrains.bazel.progress.TaskConsole

@Service(Service.Level.PROJECT)
internal class DefaultConsoleService(project: Project) : ConsoleService {
  override val buildConsole: TaskConsole

  override val syncConsole: TaskConsole

  init {
    val basePath = project.rootDir.path

    buildConsole =
      BuildTaskConsole(
        project.getService(BuildViewManager::class.java),
        basePath,
        project,
      )
    syncConsole =
      SyncTaskConsole(
        project.getService(SyncViewManager::class.java),
        basePath,
        project,
      )
  }
}
