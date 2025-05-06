package org.jetbrains.bazel.golang.sync.ideStarter

import com.intellij.openapi.ui.playback.PlaybackContext
import com.intellij.openapi.ui.playback.commands.PlaybackCommandCoroutineAdapter
import com.intellij.platform.backend.workspace.workspaceModel
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.jetbrains.performancePlugin.CommandProvider
import com.jetbrains.performancePlugin.CreateCommand
import kotlin.collections.map
import kotlin.collections.toSet
import kotlin.jvm.java
import kotlin.sequences.toList
import kotlin.to

class GoLandCheckImportedModules(text: String, line: Int) : PlaybackCommandCoroutineAdapter(text, line) {
  override suspend fun doExecute(context: PlaybackContext) {
    val project = context.project
    val workspaceModel = project.workspaceModel
    val modules = workspaceModel.currentSnapshot.entities(ModuleEntity::class.java).toList()

    val expectedModulesNames =
      setOf(
        "go.binary",
        "go.library",
        "go.test",
      )
    val actualModulesNames = modules.map { it.name }.toSet()
    check(actualModulesNames == expectedModulesNames) { "Expected modules: $expectedModulesNames, actual: $actualModulesNames" }
  }

  companion object {
    const val PREFIX = "${CMD_PREFIX}GoLandCheckImportedModules"
  }
}

class GoLandCheckImportedModulesProvider : CommandProvider {
  override fun getCommands(): Map<String, CreateCommand?> =
    mapOf(
      GoLandCheckImportedModules.PREFIX to CreateCommand(::GoLandCheckImportedModules),
    )
}
