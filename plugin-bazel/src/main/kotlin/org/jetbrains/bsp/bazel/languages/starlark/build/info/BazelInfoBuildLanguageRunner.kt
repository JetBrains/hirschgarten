package org.jetbrains.bsp.bazel.languages.starlark.build.info

import com.google.devtools.build.lib.query2.proto.proto2api.Build
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.startup.ProjectPostStartupActivity
import com.intellij.openapi.startup.StartupActivity
import com.intellij.util.EnvironmentUtil
import java.io.File
import java.util.concurrent.TimeUnit

class BazelInfoBuildLanguageRunner : ProjectPostStartupActivity {

    override suspend fun execute(project: Project) {
        val projectDir = project.basePath!!
        val projectBuildLanguageInfoService = ProjectBuildLanguageInfoService.getInstance(project)
        callBazelInfoBuildLanguageAndSaveResultsInTheService(projectDir, projectBuildLanguageInfoService)
    }

    private fun callBazelInfoBuildLanguageAndSaveResultsInTheService(projectDir: String, service: ProjectBuildLanguageInfoService) {
        val result = callBazelInfoBuildLanguageAndCollectResults(projectDir)
        if (result != null) {
            saveBazelInfoBuildLanguageOutputInTheService(result, service)
        }
    }

    private fun callBazelInfoBuildLanguageAndCollectResults(projectDir: String): ByteArray? {
        val command = listOf("bazel", "info", "build-language")

        val proc = ProcessBuilder(command)
            .directory(File(projectDir))
            .withRealEnvs()
            .start()
        val output = proc.inputStream.readAllBytes()


        return if (proc.waitFor(20, TimeUnit.SECONDS)) {
            output
        } else {
            thisLogger().error("BazelInfoBuildLanguageRunner: Got non-zero return value from bazel (${proc.exitValue()})")
            null
        }
    }

    private fun saveBazelInfoBuildLanguageOutputInTheService(commandOutput: ByteArray, service: ProjectBuildLanguageInfoService) {
        val proto = Build.BuildLanguage.parseFrom(commandOutput)
        service.info = proto
    }
}

fun ProcessBuilder.withRealEnvs(): ProcessBuilder {
    val env = environment()
    env.clear()
    env.putAll(EnvironmentUtil.getEnvironmentMap())
    return this
}
