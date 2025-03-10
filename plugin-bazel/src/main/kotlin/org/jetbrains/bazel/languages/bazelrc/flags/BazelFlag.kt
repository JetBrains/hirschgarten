package org.jetbrains.bazel.languages.bazelrc.flags

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.toPersistentMap
import java.lang.reflect.Field
import kotlin.reflect.KProperty

sealed class Flag {
  abstract val name: String

  data class Unknown(override val name: String) : Flag()

  data class Boolean(override val name: String) : Flag()

  data class TriState(override val name: String) : Flag()

  data class Integer(override val name: String) : Flag()

  data class Path(override val name: String) : Flag()

  data class Double(override val name: String) : Flag()

  data class Duration(override val name: String) : Flag()

  data class Str(override val name: String) : Flag()

  data class Label(override val name: String) : Flag()

  data class OneOf(override val name: String) : Flag()

  /** Lazily load and return the @Option() annotation associated with this value, if any. */
  val option: Option by object : LazyExtension<Option, Flag>() {
    override fun initValue(o: Flag): Option =
      KnownFlags.declaredFieldsMap[o.name]?.getDeclaredAnnotation(Option::class.java) ?: Option(o.name)
  }

  companion object {
    internal val KnownFlags.declaredFieldsMap by object : LazyExtension<PersistentMap<String, Field>, KnownFlags>() {
      override fun initValue(o: KnownFlags): PersistentMap<String, Field> =
        KnownFlags::class.java.declaredFields
          .filter { it.getDeclaredAnnotation(Option::class.java) != null }
          .associateBy { it.name }
          .toPersistentMap()
    }

    private val KnownFlags.allFlags by object : LazyExtension<PersistentMap<String, Flag>, KnownFlags>() {
      override fun initValue(o: KnownFlags): PersistentMap<String, Flag> =
        KnownFlags.declaredFieldsMap
          .values
          .mapNotNull {
            it.getDeclaredAnnotation(Option::class.java)?.let { op -> Pair(it, op) }
          }.mapNotNull { (f, op) -> (f.get(o) as? Flag)?.let { flag -> Pair(flag, op) } }
          .flatMap { knownFlagNames(it) }
          .toMap()
          .toPersistentMap()
    }

    private val KnownFlags.allCommands by object : LazyExtension<Set<String>, KnownFlags>() {
      override fun initValue(o: KnownFlags): Set<String> =
        KnownFlags.allFlags
          .flatMap { x ->
            x.value.option.commands
              .asIterable()
          }.toSet()
    }

    fun commands() = KnownFlags.allCommands

    fun byName(name: String) = KnownFlags.allFlags[name]

    fun all() = KnownFlags.allFlags
  }
}

fun knownFlagNames(pair: Pair<Flag, Option>): List<Pair<String, Flag>> {
  val (flag, option) = pair
  var names = listOf(option.name, option.oldName).filter { it.isNotEmpty() }

  names =
    when (flag) {
      is Flag.Boolean -> names.flatMap { listOf("--$it", "--no$it") }
      is Flag.TriState -> names.flatMap { listOf("--$it", "--no$it") }
      else -> names.map { "--$it" }
    }

  return names.map { it to flag }
}

abstract class LazyExtension<T, This> {
  private lateinit var value: T & Any

  operator fun getValue(o: This, p: KProperty<*>): T {
    if (!::value.isInitialized) {
      value = initValue(o)
    }
    return value
  }

  abstract fun initValue(o: This): T & Any
}
