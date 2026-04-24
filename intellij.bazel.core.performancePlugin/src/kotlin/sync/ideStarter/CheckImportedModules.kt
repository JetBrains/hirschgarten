package org.jetbrains.bazel.kotlin.sync.ideStarter

import com.intellij.openapi.ui.playback.PlaybackContext
import com.intellij.openapi.ui.playback.commands.PlaybackCommandCoroutineAdapter
import com.intellij.platform.backend.workspace.workspaceModel
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.jetbrains.performancePlugin.CommandProvider
import com.jetbrains.performancePlugin.CreateCommand

internal class CheckImportedModules(text: String, line: Int) : PlaybackCommandCoroutineAdapter(text, line) {
  override suspend fun doExecute(context: PlaybackContext) {
    val project = context.project
    val workspaceModel = project.workspaceModel
    val modules = workspaceModel.currentSnapshot.entities(ModuleEntity::class.java).toList()

    val expectedModulesNames =
      setOf(
        "java.binary",
        "java.library",
        "java.test",
      )
    val actualModulesNames = modules.map { it.name }.toSet()
    check(actualModulesNames == expectedModulesNames) { "Expected modules: $expectedModulesNames, actual: $actualModulesNames" }
  }

  companion object {
    const val PREFIX = "${CMD_PREFIX}checkImportedModules"
  }
}

internal class CheckImportedModulesProvider : CommandProvider {
  override fun getCommands(): Map<String, CreateCommand?> =
    mapOf(
      CheckImportedModules.PREFIX to CreateCommand(::CheckImportedModules),
    )
}
