package com.github.abrams27.intellijbsp.services

import com.github.abrams27.intellijbsp.MyBundle
import com.intellij.openapi.project.Project

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
