package org.jetbrains.plugins.bsp.services

import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.MyBundle

class MyProjectService(project: Project) {

  init {
    println(MyBundle.message("projectService", project.name))
  }
}
