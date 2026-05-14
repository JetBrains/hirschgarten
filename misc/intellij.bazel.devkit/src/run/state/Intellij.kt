package org.jetbrains.bazel.run.state

import com.intellij.execution.ui.SettingsEditorFragment
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.ui.components.JBList
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.idea.devkit.projectRoots.IdeaJdk

internal interface HasIntellijSdkName {
  var intellijSdkName: String?
}

private val BazelRunConfiguration.intellijSdkNameState: HasIntellijSdkName?
  get() = handler?.state as? HasIntellijSdkName

internal fun intellijSdkFragment(): SettingsEditorFragment<BazelRunConfiguration, JBList<String>> {
  val jdkType = IdeaJdk.getInstance()
  val jdks = ProjectJdkTable.getInstance().getSdksOfType(jdkType).map { it.name }

  val component = JBList(JBList.createDefaultListModel(jdks))

  val workingDirectorySettings: SettingsEditorFragment<BazelRunConfiguration, JBList<String>> =
    SettingsEditorFragment(
      "intellijSdkName",
      BazelPluginBundle.message("state.bazel.intellij.sdk"),
      null,
      component,
      { configuration, component ->
        component.selectedIndex = jdks.indexOf(configuration.intellijSdkNameState?.intellijSdkName).takeIf { it >= 0 } ?: 0
      },
      { configuration, component ->
        configuration.intellijSdkNameState?.intellijSdkName = jdks.getOrNull(component.selectedIndex)
      },
      { true },
    )

  return workingDirectorySettings
}
