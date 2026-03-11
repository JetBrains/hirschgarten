package org.jetbrains.bazel.languages.projectview.sections.presets

import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
abstract class BooleanScalarSection : VariantsScalarSection<Boolean>(listOf("true", "false")) {
  final override fun fromRawValue(rawValue: String): Boolean? = rawValue.toBooleanStrictOrNull()
}
