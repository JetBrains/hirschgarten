package org.jetbrains.bsp.bazel.languages.starlark.build.info

import com.google.devtools.build.lib.query2.proto.proto2api.Build
import com.intellij.openapi.project.Project

class ProjectBuildLanguageInfoService {

    var info: Build.BuildLanguage? = null

    companion object {
        fun getInstance(project: Project): ProjectBuildLanguageInfoService =
            project.getService(ProjectBuildLanguageInfoService::class.java)
    }
}

fun Build.BuildLanguage.calculateRuleDefinition(name: String): Build.RuleDefinition? =
    ruleList.firstOrNull { it.name == name }
