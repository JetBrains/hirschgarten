package org.jetbrains.bazel.testing

import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
data class BazelTestDetails(
  val displayName: String,
  val isSuite: Boolean,
  val classname: String?,
  val file: String?,
  val oneBasedLine: Int?,
  val parentSuiteNames: List<String>,
) {
  companion object {
    fun testCase(name: String): Builder =
      Builder(name, isSuite = false)

    fun testSuite(name: String): Builder =
      Builder(name, isSuite = true)
  }

  class Builder(val name: String, val isSuite: Boolean) {
    var classname: String? = null
    var file: String? = null
    var oneBasedLine: Int? = null
    var parentSuiteNames: List<String> = emptyList()

    fun withClassname(classname: String): Builder =
      apply { this.classname = classname }

    fun withFileAndZeroBasedLine(file: String?, line: Int?): Builder =
      apply {
        this.file = file
        this.oneBasedLine = line?.let { it + 1 }
      }

    fun withParentSuites(parentSuiteNames: List<String>): Builder =
      apply { this.parentSuiteNames = parentSuiteNames }

    fun withParentSuite(parentSuiteName: String): Builder =
      apply { this.parentSuiteNames = listOf(parentSuiteName) }

    fun build(): BazelTestDetails =
      BazelTestDetails(
        displayName = name,
        isSuite = isSuite,
        classname = classname,
        file = file,
        oneBasedLine = oneBasedLine,
        parentSuiteNames = parentSuiteNames,
      )
  }
}
