package org.jetbrains.bazel.workspace.packageMarker

internal fun concatenatePackages(first: String, second: String): String {
  if (first.isEmpty()) return second
  if (second.isEmpty()) return first
  return "$first.$second"
}
