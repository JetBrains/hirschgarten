package org.jetbrains.bazel.languages.starlark.globbing

import com.google.common.base.Splitter

/**
 * Support for resolving globs.
 */
object GlobPatternValidator {
  /**
   * Validate a single glob pattern. If it's invalid, returns an error message. Otherwise, returns
   * null.
   */
  fun validate(pattern: String): String? {
    val error = checkPatternForError(pattern)
    if (error != null) {
      return "Invalid glob pattern: " + error
    }
    return null
  }

  private fun checkPatternForError(pattern: String): String? {
    if (pattern.isEmpty()) {
      return "pattern cannot be empty"
    }
    if (pattern.get(0) == '/') {
      return "pattern cannot be absolute"
    }
    for (i in 0..<pattern.length) {
      val c = pattern.get(i)
      when (c) {
        '(', ')', '{', '}', '[', ']' -> return "illegal character '" + c + "'"
        else -> {}
      }
    }
    val segments = Splitter.on('/').split(pattern)
    for (segment in segments) {
      if (segment.isEmpty()) {
        return "empty segment not permitted"
      }
      if (segment == "." || segment == "..") {
        return "segment '" + segment + "' not permitted"
      }
      if (segment.contains("**") && segment != "**") {
        return "recursive wildcard must be its own segment"
      }
    }
    return null
  }
}
