package org.jetbrains.bazel.testing

import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
data class BazelTestDetails(
  val displayName: String,
  val isSuite: Boolean = false,
  val classname: String? = null,
  val file: String? = null,
  val oneBasedLine: Int? = null,
  val parentSuiteNames: List<String> = emptyList(),
)
