package org.jetbrains.bazel.languages.starlark.lexer

import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.ints.IntList
import java.util.BitSet

/**
 * The indent levels from 0 up to the current level.
 * [changeTo] moves to the indent of the next line with a significant token:
 * - a deeper indent adds a level and gives [Change.Indented],
 * - a smaller indent removes the deeper levels and gives [Change.Dedented],
 * - the same indent, or an indent that closes only inconsistent levels and lands on a level, gives null.
 *
 * For the indents 0, 4, 8, 4, 0 the changes are null, indented 4, indented 8, dedented 8, dedented 4.
 * An indent of 2 after the levels 0 and 4 gives dedented 4, and the change is inconsistent, because 2 is not a level.
 *
 * After an inconsistent change, the indent becomes an inconsistent level. It had no indent, so it gives no dedent when it closes.
 * So in `if a:⏎    b⏎  c⏎  d⏎e` only the line `c` is inconsistent. The lines `d` and `e` give no change.
 */
internal class IndentStack {
  private val indents = IntArrayList().apply { push(0) }

  /** The positions in [indents] of the inconsistent levels. */
  private val inconsistentLevels = BitSet()

  fun changeTo(indent: Int): Change? {
    if (indent > indents.topInt()) {
      indents.push(indent)
      return Change.Indented(indent)
    }
    if (indent == indents.topInt()) return null
    val closed = IntArrayList()
    while (indent < indents.topInt()) {
      val position = indents.size - 1
      val level = indents.popInt()
      if (!inconsistentLevels.get(position)) closed.add(level)
      inconsistentLevels.clear(position)
    }
    val isInconsistent = indent > indents.topInt()
    if (isInconsistent) {
      inconsistentLevels.set(indents.size)
      indents.push(indent)
    }
    return if (closed.isEmpty && !isInconsistent) null else Change.Dedented(closed, isInconsistent)
  }

  sealed interface Change {
    class Indented(val indent: Int) : Change

    /**
     * The levels that the indent closes, deepest first, without the inconsistent levels.
     * [isInconsistent] is true if the indent is not one of the remaining levels. Then [closedLevels] can be empty.
     */
    class Dedented(val closedLevels: IntList, val isInconsistent: Boolean) : Change
  }
}
