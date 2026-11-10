package org.jetbrains.bazel.languages.starlark.lexer

import com.intellij.psi.tree.IElementType
import it.unimi.dsi.fastutil.ints.IntList
import org.jetbrains.bazel.languages.starlark.parser.generated.StarlarkTokenTypes

/**
 * A queue of new tokens: statement breaks, whitespace, comments, indents and dedents.
 * An indent or a dedent can go between comment lines that are already in the queue.
 * This is necessary because comment lines can come before the line that opens or closes a block.
 *
 * A comment line belongs to a block if its indent is the indent of the block or more:
 * - an indent goes before the first comment line of the new block, after the last comment line with a smaller indent,
 * - a dedent goes after the last comment line of the closed block, before the first comment line with a smaller indent.
 *
 * For example:
 * ```
 * if a:
 * # c      <- not in the new block: the indent goes after it
 *     # d  <- in the new block: the indent goes before it
 *     b
 *     # e  <- in the closed block: the dedent goes after it
 * # f      <- not in the closed block: the dedent goes before it
 * g
 * ```
 *
 * The queue holds the tokens of one run of line breaks and comment lines, so it is empty before each run.
 * The lexer adds the statement break first, then the line breaks and the comment lines.
 * At the last line break, it adds the dedents, then the whitespace, then the indent.
 */
internal class PendingTokens {
  private val queue = ArrayDeque<PendingToken>()

  val first: PendingToken? get() = queue.firstOrNull()

  /** Removes the first token. Returns false if the queue is empty. */
  fun tryAdvance(): Boolean {
    if (queue.isEmpty()) return false
    queue.removeFirst()
    return true
  }

  fun add(token: PendingToken) {
    queue.add(token)
  }

  fun addAll(tokens: Collection<PendingToken>) {
    queue.addAll(tokens)
  }

  /** Adds the indent of a new block with [blockIndent]. If the queue has no comments of the new block, the indent goes at [end]. */
  fun addIndent(blockIndent: Int, end: Int) {
    insertMarker(indentPosition(blockIndent), StarlarkTokenTypes.INDENT, end)
  }

  /**
   * Adds one dedent for each of the [closedLevels], innermost first.
   * If [isInconsistent], an inconsistent dedent goes before the last dedent, or alone when no level closes.
   */
  fun addDedents(closedLevels: IntList, isInconsistent: Boolean, offset: Int) {
    var position = dedentStart()
    for (i in closedLevels.indices) {
      position = positionAfterBlockComments(closedLevels.getInt(i), position)
      if (isInconsistent && i == closedLevels.lastIndex) {
        insertMarker(position++, StarlarkTokenTypes.INCONSISTENT_DEDENT, offset)
      }
      insertMarker(position++, StarlarkTokenTypes.DEDENT, offset)
    }
    if (isInconsistent && closedLevels.isEmpty()) {
      insertMarker(position, StarlarkTokenTypes.INCONSISTENT_DEDENT, offset)
    }
  }

  /**
   * Returns the position of the line break before the first comment line, or the end of the queue if it has no comment line.
   * A comment is first in the queue only at the start of the text, where no dedent is possible.
   */
  private fun dedentStart(): Int {
    val firstComment = queue.indexOfFirst { it is PendingCommentToken }
    return if (firstComment > 0) firstComment - 1 else queue.size
  }

  /** Inserts a token with no length at [position]. It starts where the next token starts, or at [endOffset] if there is no next token. */
  private fun insertMarker(position: Int, type: IElementType, endOffset: Int) {
    val offset = if (position == queue.size) endOffset else queue[position].start
    queue.add(position, PendingToken(type, offset, offset))
  }

  /**
   * Returns the position of the first comment of the comment lines at the end of the queue that have [blockIndent] or more.
   * Returns the end of the queue if there is no such comment.
   */
  private fun indentPosition(blockIndent: Int): Int {
    var position = queue.size
    for (i in queue.indices.reversed()) {
      val token = queue[i]
      when {
        token is PendingCommentToken -> if (token.indent >= blockIndent) position = i else break
        token.type !== StarlarkTokenTypes.LINE_BREAK -> break
      }
    }
    return position
  }

  /**
   * Returns the position after the comments of the closed block, from [from] on. Their indent is [blockIndent] or more.
   * The scan skips the tokens that are not comments.
   */
  private fun positionAfterBlockComments(blockIndent: Int, from: Int): Int {
    var result = from
    for (i in from until queue.size) {
      val token = queue[i]
      if (token is PendingCommentToken) {
        if (token.indent < blockIndent) {
          break
        }
        result = i + 1
      }
    }
    return result
  }
}

internal open class PendingToken(
  val type: IElementType,
  val start: Int,
  val end: Int,
) {
  override fun toString(): String = "$type:$start-$end"
}

internal class PendingCommentToken(
  start: Int,
  end: Int,
  val indent: Int,
) : PendingToken(StarlarkTokenTypes.COMMENT, start, end)
