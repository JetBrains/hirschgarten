package org.jetbrains.plugins.bsp.runConfig

import com.intellij.execution.configurations.LocatableRunConfigurationOptions
import com.intellij.openapi.components.StoredProperty

public class BspRunConfigurationOptions : LocatableRunConfigurationOptions() {
  private val targetProperty: StoredProperty<String?> = string("").provideDelegate(this, "target")
  public var target: String? // TODO: Will this always be a URI?
    get() = targetProperty.getValue(this)
    set(value) = targetProperty.setValue(this, value)
}
