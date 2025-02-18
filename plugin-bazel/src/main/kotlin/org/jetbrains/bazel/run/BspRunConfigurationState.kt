package org.jetbrains.bazel.run

import com.intellij.configurationStore.deserializeInto
import com.intellij.configurationStore.serializeObjectInto
import com.intellij.execution.ui.FragmentedSettings
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.util.InvalidDataException
import com.intellij.openapi.util.WriteExternalException
import com.intellij.util.xmlb.annotations.XCollection
import org.jdom.Element
import org.jetbrains.bazel.run.config.BspRunConfiguration

abstract class BspRunConfigurationState<T : BspRunConfigurationState<T>> :
  BaseState(),
  FragmentedSettings {
  /** Loads this handler's state from the external data.  */
  @Throws(InvalidDataException::class)
  fun readExternal(element: Element) {
    element.deserializeInto(this)
  }

  /** Updates the element with the handler's state.  */
  @Throws(WriteExternalException::class)
  fun writeExternal(element: Element) {
    serializeObjectInto(this, element)
  }

  @get:XCollection(propertyElementName = "selectedOptions")
  override var selectedOptions: MutableList<FragmentedSettings.Option> by list()

  abstract fun getEditor(configuration: BspRunConfiguration): SettingsEditor<T>
}
