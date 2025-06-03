package org.jetbrains.bazel.python.sync.ideStarter

import com.intellij.openapi.ui.playback.PlaybackContext
import com.intellij.openapi.ui.playback.commands.PlaybackCommandCoroutineAdapter
import com.intellij.platform.backend.workspace.workspaceModel
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.jetbrains.performancePlugin.CommandProvider
import com.jetbrains.performancePlugin.CreateCommand

class PyCharmCheckImportedModules(text: String, line: Int) : PlaybackCommandCoroutineAdapter(text, line) {
  override suspend fun doExecute(context: PlaybackContext) {
    val project = context.project
    val workspaceModel = project.workspaceModel
    val modules = workspaceModel.currentSnapshot.entities(ModuleEntity::class.java).toList()

    val expectedModulesNames =
      setOf(
        "python.binary",
        "python.libs.my_lib2.my_lib2",
        "python.library",
        "python.libs.my_lib.my_lib",
        "python.test",
        "python.main.main",
      )
    val actualModulesNames = modules.map { it.name }.toSet()
    check(expectedModulesNames.all { actualModulesNames.contains(it) }) { "Expected modules: $expectedModulesNames, actual: $actualModulesNames" }
  }

  companion object {
    const val PREFIX = "${CMD_PREFIX}PyCharmCheckImportedModules"
  }
}

class PyCharmCheckImportedModulesProvider : CommandProvider {
  override fun getCommands(): Map<String, CreateCommand?> =
    mapOf(
      PyCharmCheckImportedModules.PREFIX to CreateCommand(::PyCharmCheckImportedModules),
    )
}
