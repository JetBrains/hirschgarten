package org.jetbrains.bazel.buildTask

import com.intellij.openapi.project.Project
import com.intellij.task.ProjectTaskManager
import org.jetbrains.concurrency.await

suspend fun Project.buildProject() {
  ProjectTaskManager.getInstance(this).buildAllModules().await()
}
