package org.jetbrains.plugins.bsp.ui.runconfig

import com.intellij.execution.ui.SettingsEditorFragment
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.ui.components.JBList
import fleet.util.indexOfOrNull
import org.jetbrains.idea.devkit.projectRoots.IdeaJdk

interface HasIntellijSdkName {
  var intellijSdkName: String?
}

fun <T : HasIntellijSdkName> intellijSdkFragment(): SettingsEditorFragment<T, JBList<String>> {
  val jdkType = IdeaJdk.getInstance()
  val jdks = ProjectJdkTable.getInstance().getSdksOfType(jdkType).map { it.name }

  val component = JBList(JBList.createDefaultListModel(jdks))

  val workingDirectorySettings: SettingsEditorFragment<T, JBList<String>> =
    SettingsEditorFragment(
      "intellijSdkName",
      "IntelliJ SDK",
      null,
      component,
      { s, c ->
        c.selectedIndex = jdks.indexOfOrNull(s.intellijSdkName) ?: 0
      },
      { s, c ->
        s.intellijSdkName = jdks.getOrNull(c.selectedIndex)
      },
      { true },
    )

  return workingDirectorySettings
}
