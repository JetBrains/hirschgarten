package org.jetbrains.bazel.languages.bazelrc.flags

import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

var flagsByName = mutableMapOf<String, BazelFlag<*>>()

fun flagByName(name: String): BazelFlag<*>? {
  val x = flags.javaClass
  return flagsByName[name]
}

data class BazelFlag<T>(
  val name: String,
  val description: String,
  val default: T? = null,
  val tags: ImmutableSet<String> = persistentSetOf(),
  val variants: ImmutableSet<T> = persistentSetOf(),
  val otherNames: Set<String> = persistentSetOf(),
) {
  init {
    flagsByName["--$name"] = this
    otherNames.forEach { otherName -> flagsByName["--$otherName"] = this }
  }

  companion object {
    fun boolean(
      name: String,
      description: String,
      default: Boolean? = null,
      tags: ImmutableSet<String> = persistentSetOf(),
    ) = BazelFlag(
      name = name,
      otherNames = persistentSetOf("no$name"),
      description = description,
      default = default,
      tags = tags,
    )
  }
}
