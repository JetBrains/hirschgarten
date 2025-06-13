package org.jetbrains.bazel.utils

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class BazelWorkingDirectoryManager {
  @Volatile
  private var workingDirectory: String = ""

  fun getWorkingDirectory(): String = workingDirectory

  fun setWorkingDirectory(directory: String) {
    workingDirectory = directory
  }

  companion object {
    @JvmStatic
    fun getInstance(project: Project): BazelWorkingDirectoryManager = project.getService(BazelWorkingDirectoryManager::class.java)
  }
}
