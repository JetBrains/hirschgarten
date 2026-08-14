package org.jetbrains.bazel.workspace.packageMarker

import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
fun concatenatePackages(first: String, second: String): String {
  if (first.isEmpty()) return second
  if (second.isEmpty()) return first
  return "$first.$second"
}
