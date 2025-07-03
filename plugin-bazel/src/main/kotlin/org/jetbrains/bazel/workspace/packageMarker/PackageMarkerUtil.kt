package org.jetbrains.bazel.workspace.packageMarker

fun concatenatePackages(first: String, second: String): String {
  if (first.isEmpty()) return second
  if (second.isEmpty()) return first
  return "$first.$second"
}
