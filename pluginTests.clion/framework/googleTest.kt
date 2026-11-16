package org.jetbrains.bazel.clion

internal fun matchesGoogleTestFilter(filter: String, testName: String): Boolean {
  val patterns = filter.split(':').filter { it.isNotEmpty() }
  return patterns.isEmpty() || patterns.any { filterMatches(it, testName) }
}

private fun filterMatches(pattern: String, name: String): Boolean {
  val regex = buildString {
    append('^')

    for (c in pattern) {
      when (c) {
        '*' -> append(".*")
        '?' -> append('.')
        else -> append(Regex.escape(c.toString()))
      }
    }

    append('$')
  }

  return Regex(regex).matches(name)
}
