package org.jetbrains.bazel.run.state

import com.intellij.execution.ui.SettingsEditorFragment
import com.intellij.ui.components.JBCheckBox
import org.jetbrains.bazel.config.BazelPluginBundle

interface HasUseMobileInstall {
  var useMobileInstall: Boolean
}

fun <T : HasUseMobileInstall> useMobileInstallFragment(): SettingsEditorFragment<T, JBCheckBox> =
  SettingsEditorFragment(
    "useMobileInstall",
    BazelPluginBundle.message("state.android.use.mobile.install.name"),
    null,
    JBCheckBox(BazelPluginBundle.message("state.android.use.mobile.install.name")),
    { settings, component ->
      component.isSelected = settings.useMobileInstall
    },
    { settings, component ->
      settings.useMobileInstall = component.isSelected
    },
    { true },
  )
