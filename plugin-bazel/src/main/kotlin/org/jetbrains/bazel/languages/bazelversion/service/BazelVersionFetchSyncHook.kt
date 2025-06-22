package org.jetbrains.bazel.languages.bazelversion.service

import com.intellij.openapi.components.service
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.languages.bazelversion.psi.toBazelVersionLiteral
import org.jetbrains.bazel.sync.ProjectPostSyncHook
import kotlin.io.path.isReadable
import kotlin.io.path.readText

class BazelVersionFetchSyncHook : ProjectPostSyncHook {
  override suspend fun onPostSync(environment: ProjectPostSyncHook.ProjectPostSyncHookEnvironment) {
    val currentVersion = environment.project.rootDir
      .toNioPath()
      .resolve(".bazelversion")
      .takeIf { it.isReadable() }
      .let { it?.readText()?.toBazelVersionLiteral() }
    environment.project.service<BazelVersionCheckerService>()
      .refreshLatestBazelVersion(environment.project, currentVersion)
  }
}
