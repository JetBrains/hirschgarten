package org.jetbrains.bsp.protocol

import com.intellij.util.containers.Interner
import org.jetbrains.annotations.ApiStatus

/**
 * Immutable list of strings that is interned as a whole, e.g. compiler options or program arguments.
 *
 * Most targets share the same lists, so equal collections share one instance in memory,
 * and the workspace snapshot stores each distinct collection once per generation.
 */
@ApiStatus.Internal
class StringsCollection private constructor(private val strings: Array<String>) {
  private val contentHash: Int = strings.contentHashCode()

  val size: Int
    get() = strings.size

  fun isEmpty(): Boolean = strings.isEmpty()

  operator fun get(index: Int): String = strings[index]

  fun asList(): List<String> = strings.asList()

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    return other is StringsCollection && contentHash == other.contentHash && strings.contentEquals(other.strings)
  }

  override fun hashCode(): Int = contentHash

  override fun toString(): String = strings.contentToString()

  companion object {
    private val arrInterner = Interner.createWeakInterner<StringsCollection>()
    private val strInterner = Interner.createWeakInterner<String>()

    val EMPTY: StringsCollection = StringsCollection(emptyArray())

    fun of(strings: Collection<String>): StringsCollection {
      if (strings.isEmpty())
        return EMPTY

      return arrInterner.intern(StringsCollection(strings.map { strInterner.intern(it) }.toTypedArray()))
    }
  }
}
