package org.jetbrains.bazel.run.state

import com.intellij.execution.ui.SettingsEditorFragment
import com.intellij.ui.components.JBCheckBox

interface HasUseMobileInstall {
  var useMobileInstall: Boolean
}

fun <T : HasUseMobileInstall> useMobileInstallFragment(): SettingsEditorFragment<T, JBCheckBox> =
  SettingsEditorFragment(
    "useMobileInstall",
    "Use mobile-install",
    null,
    JBCheckBox("Use mobile-install"),
    { settings, component ->
      component.isSelected = settings.useMobileInstall
    },
    { settings, component ->
      settings.useMobileInstall = component.isSelected
    },
    { true },
  )
