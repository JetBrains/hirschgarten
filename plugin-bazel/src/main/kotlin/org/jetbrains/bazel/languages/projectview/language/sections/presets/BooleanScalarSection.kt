package org.jetbrains.bazel.languages.projectview.language.sections.presets

abstract class BooleanScalarSection : VariantsScalarSection<Boolean>(listOf("true", "false")) {
  final override fun fromRawValue(rawValue: String): Boolean? = rawValue.toBooleanStrictOrNull()
}
