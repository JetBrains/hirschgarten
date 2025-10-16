package org.jetbrains.bazel.commons

sealed class ExcludableValue<T> {
  abstract val value: T

  data class Included<T>(override val value: T) : ExcludableValue<T>() {
    override fun toString(): String = value.toString()
  }

  data class Excluded<T>(override val value: T) : ExcludableValue<T>() {
    override fun toString(): String = "-$value"
  }

  fun isIncluded(): Boolean = this is Included

  fun isExcluded(): Boolean = this is Excluded

  companion object {
    fun <T> included(value: T): ExcludableValue<T> = Included(value)

    fun <T> excluded(value: T): ExcludableValue<T> = Excluded(value)
  }
}
