package org.jetbrains.plugins.bsp.run

import com.intellij.execution.configurations.LocatableRunConfigurationOptions
import com.intellij.openapi.components.StoredProperty

public class BspRunConfigurationOptions : LocatableRunConfigurationOptions() {
  private val targetProperty: StoredProperty<String?> = string("").provideDelegate(this, "target")
  private val runTypeProperty: StoredProperty<BspRunType> = enum(BspRunType.BUILD).provideDelegate(this, "runType")
  public var target: String?
    get() = targetProperty.getValue(this)
    set(value) = targetProperty.setValue(this, value)

  public var runType: BspRunType
    get() = runTypeProperty.getValue(this)
    set(value) = runTypeProperty.setValue(this, value)
}
