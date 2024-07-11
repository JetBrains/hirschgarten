package org.jetbrains.plugins.bsp.services

public abstract class ValueServiceWhichNeedsToBeInitialized<T>(private val defaultValue: T? = null) {
  public var value: T
    get() = valueToInitialize ?: defaultValue
      ?: error("Can't read the value! You need to initialize the service before using the value.")
    protected set(value) {
      valueToInitialize = value
    }

  private var valueToInitialize: T? = null
  private var wasInitialized = false

  public fun init(value: T) {
    check(!wasInitialized) { "Init called on initialized service! This function can be called only once." }
    wasInitialized = true

    this.valueToInitialize = value
  }
}
