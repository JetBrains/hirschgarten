package org.jetbrains.bazel.languages.projectview

import com.google.common.hash.HashCode
import com.google.common.hash.Hasher
import com.google.common.hash.Hashing
import org.jetbrains.bazel.commons.ExcludableValue
import java.nio.file.Path
import kotlin.io.path.absolutePathString

@Suppress("UnstableApiUsage")
object ProjectViewHasher {
  fun hash(projectView: ProjectView): HashCode {
    val hasher = Hashing.murmur3_128().newHasher()
    for ((key, value) in projectView.sections) {
      hasher.putString(key.name, Charsets.UTF_8)
      hashSectionValue(hasher, value)
    }
    return hasher.hash()
  }

  private fun hashSectionValue(hasher: Hasher, value: Any?) {
    when (value) {
      is Int -> hasher.putInt(value)
      is Boolean -> hasher.putBoolean(value)
      is String -> hasher.putString(value, Charsets.UTF_8)
      is Path -> hasher.putString(value.absolutePathString(), Charsets.UTF_8)
      is Collection<*> -> value.filterNotNull().forEach { hashSectionValue(hasher, it) }
      is ExcludableValue<*> ->
        when (value) {
          is ExcludableValue.Excluded<*> -> {
            hasher.putInt(1)
            hashSectionValue(hasher, value.value)
          }
          is ExcludableValue.Included<*> -> {
            hasher.putInt(2)
            hashSectionValue(hasher, value.value)
          }
        }
      is Map<*, *> -> {
        for ((key, value) in value.entries) {
          hashSectionValue(hasher, key)
          hashSectionValue(hasher, value)
        }
      }
      else -> {
        if (value != null) {
          hasher.putInt(value.hashCode())
        }
      }
    }
  }
}
