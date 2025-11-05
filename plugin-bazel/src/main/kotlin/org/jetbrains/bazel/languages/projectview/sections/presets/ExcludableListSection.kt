package org.jetbrains.bazel.languages.projectview.sections.presets

import org.jetbrains.bazel.commons.ExcludableValue
import org.jetbrains.bazel.languages.projectview.ListSection

abstract class ExcludableListSection<T> : ListSection<List<ExcludableValue<T>>>() {
  abstract fun parseItem(value: String): T?

  final override fun fromRawValues(rawValues: List<String>): List<ExcludableValue<T>> =
    rawValues.mapNotNull {
      if (it.startsWith("-")) {
        parseItem(it.substring(1))?.let { parsed -> ExcludableValue.excluded(parsed) }
      } else {
        parseItem(it)?.let { parsed -> ExcludableValue.included(parsed) }
      }
    }
}
