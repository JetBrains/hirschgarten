package org.jetbrains.bazel.languages.starlark.lexer

import com.intellij.lexer.FlexAdapter
import com.intellij.psi.tree.IElementType
import it.unimi.dsi.fastutil.ints.IntArrayList
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenSets
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes

private open class PendingToken(var type: IElementType?, val start: Int, val end: Int) {
  override fun toString(): String = "$type:$start-$end"
}

private class PendingCommentToken(type: IElementType?, start: Int, end: Int, val indent: Int) :
  PendingToken(type, start, end)

class StarlarkIndentingLexer : FlexAdapter(_StarlarkLexer(null)) {
  private val indentStack = IntArrayList()
  private var braceLevel = 0
  private var lineHasSignificantTokens = false
  private var lastNewLineIndent = -1
  private var tokenQueue: MutableList<PendingToken> = ArrayList()
  private var lineBreakBeforeFirstCommentIndex = -1
  private var processSpecialTokensPending = false

  override fun getTokenType(): IElementType? = tokenQueue.getOrNull(0)?.type ?: super.getTokenType()

  override fun getTokenStart(): Int = tokenQueue.getOrNull(0)?.start ?: super.getTokenStart()

  override fun getTokenEnd(): Int = tokenQueue.getOrNull(0)?.end ?: super.getTokenEnd()

  override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
    super.start(buffer, startOffset, endOffset, initialState)
    setStartState()
  }

  override fun advance() {
    if (tokenQueue.size > 0) {
      tokenQueue.removeAt(0)
      if (processSpecialTokensPending) {
        processSpecialTokensPending = false
        processSpecialTokens()
      }
    } else {
      advanceBase()
      processSpecialTokens()
    }
    adjustBraceLevel()
  }

  private fun advanceBase() {
    super.advance()
    checkSignificantTokens()
  }

  private fun setStartState() {
    indentStack.clear()
    indentStack.push(0)
    braceLevel = 0
    adjustBraceLevel()
    lineHasSignificantTokens = false
    checkSignificantTokens()
    if (isBaseAt(StarlarkTokenTypes.SPACE)) {
      processIndent(0, StarlarkTokenTypes.SPACE)
    }
  }

  private fun adjustBraceLevel() {
    val tokenType = tokenType
    if (StarlarkTokenSets.OPEN_BRACKETS.contains(tokenType)) {
      braceLevel++
    } else if (StarlarkTokenSets.CLOSE_BRACKETS.contains(tokenType)) {
      braceLevel--
    }
  }

  private fun checkSignificantTokens() {
    val tokenType = super.getTokenType()
    if (!StarlarkTokenSets.WHITESPACE.contains(tokenType) && tokenType !== StarlarkTokenTypes.COMMENT) {
      lineHasSignificantTokens = true
    }
  }

  private fun processSpecialTokens() {
    if (isBaseAt(StarlarkTokenTypes.LINE_BREAK)) {
      processLineBreak(super.getTokenStart())
      if (isBaseAt(StarlarkTokenTypes.COMMENT)) {
        processComment()
      }
    } else if (isBaseAt(StarlarkTokenTypes.SPACE)) {
      processSpace()
    }

    val tokenStart = super.getTokenStart()
    if (super.getTokenType() == null) {
      pushToken(StarlarkTokenTypes.STATEMENT_BREAK, tokenStart, tokenStart)
    }
  }

  private fun processSpace() {
    val start = super.getTokenStart()
    var end = super.getTokenEnd()
    while (super.getTokenType() === StarlarkTokenTypes.SPACE) {
      end = super.getTokenEnd()
      advanceBase()
    }
    if (super.getTokenType() === StarlarkTokenTypes.LINE_BREAK) {
      processLineBreak(start)
    } else {
      pushToken(StarlarkTokenTypes.SPACE, start, end)
    }
  }

  private fun processLineBreak(startPos: Int) {
    if (braceLevel == 0) {
      if (lineHasSignificantTokens) {
        pushToken(StarlarkTokenTypes.STATEMENT_BREAK, startPos, startPos)
      }
      lineHasSignificantTokens = false
      advanceBase()
      processIndent(startPos, StarlarkTokenTypes.LINE_BREAK)
    } else {
      processInsignificantLineBreak(startPos)
    }
  }

  private fun processInsignificantLineBreak(startPos: Int) {
    // merge whitespace following the line break character into the line break token
    var end = super.getTokenEnd()
    advanceBase()
    while (StarlarkTokenSets.WHITESPACE.contains(super.getTokenType())) {
      end = super.getTokenEnd()
      advanceBase()
    }
    pushToken(StarlarkTokenTypes.LINE_BREAK, startPos, end)
    processSpecialTokensPending = true
  }

  private fun processComment() {
    lineBreakBeforeFirstCommentIndex = tokenQueue.size - 1
    while (isBaseAt(StarlarkTokenTypes.COMMENT)) {
      val commentEnd = super.getTokenEnd()
      tokenQueue.add(
        PendingCommentToken(
          super.getTokenType(),
          super.getTokenStart(),
          commentEnd,
          lastNewLineIndent,
        ),
      )
      advanceBase()
      if (isBaseAt(StarlarkTokenTypes.LINE_BREAK)) {
        processLineBreak(super.getTokenStart())
      } else if (super.getTokenType() == null) {
        closeDanglingSuitesWithComments(0, commentEnd)
      } else {
        break
      }
    }
    lineBreakBeforeFirstCommentIndex = -1
  }

  private fun processIndent(whiteSpaceStart: Int, whitespaceTokenType: IElementType?) {
    val lastIndent = indentStack.topInt()
    var indent = getNextLineIndent()
    lastNewLineIndent = indent
    // don't generate indent/dedent tokens if a line contains only end-of-line comment and whitespace
    if (super.getTokenType() === StarlarkTokenTypes.COMMENT) {
      indent = lastIndent
    }
    val whiteSpaceEnd = if (super.getTokenType() == null) super.getBufferEnd() else super.getTokenStart()
    if (indent > lastIndent) {
      indentStack.push(indent)
      pushToken(whitespaceTokenType, whiteSpaceStart, whiteSpaceEnd)
      val insertIndex = skipPrecedingCommentsWithIndent(indent, tokenQueue.size - 1)
      val indentOffset = if (insertIndex == tokenQueue.size) whiteSpaceEnd else tokenQueue[insertIndex].start
      tokenQueue.add(insertIndex, PendingToken(StarlarkTokenTypes.INDENT, indentOffset, indentOffset))
    } else if (indent < lastIndent) {
      closeDanglingSuitesWithComments(indent, whiteSpaceStart)
      pushToken(whitespaceTokenType, whiteSpaceStart, whiteSpaceEnd)
    } else {
      pushToken(whitespaceTokenType, whiteSpaceStart, whiteSpaceEnd)
    }
  }

  private fun closeDanglingSuitesWithComments(indent: Int, whiteSpaceStart: Int) {
    var lastIndent = indentStack.topInt()
    var insertIndex = if (lineBreakBeforeFirstCommentIndex == -1) tokenQueue.size else lineBreakBeforeFirstCommentIndex
    var lastSuiteIndent: Int
    while (indent < lastIndent) {
      lastSuiteIndent = indentStack.popInt()
      lastIndent = indentStack.topInt()
      var dedentOffset = whiteSpaceStart
      insertIndex = if (indent > lastIndent) {
        pushToken(StarlarkTokenTypes.INCONSISTENT_DEDENT, whiteSpaceStart, whiteSpaceStart)
        tokenQueue.size
      } else {
        skipPrecedingCommentsWithSameIndentOnSuiteClose(lastSuiteIndent, insertIndex)
      }
      if (insertIndex != tokenQueue.size) {
        dedentOffset = tokenQueue[insertIndex].start
      }
      tokenQueue.add(insertIndex, PendingToken(StarlarkTokenTypes.DEDENT, dedentOffset, dedentOffset))
      insertIndex++
    }
  }

  private fun skipPrecedingCommentsWithIndent(indent: Int, index: Int): Int {
    // insert the DEDENT before previous comments that have the same indent as the current token indent
    var idx = index
    var foundComment = false
    while (idx > 0 && tokenQueue[idx - 1] is PendingCommentToken) {
      val commentToken = tokenQueue[idx - 1] as PendingCommentToken
      if (commentToken.indent != indent) {
        break
      }
      foundComment = true
      idx--
      if (isPrecededByCommentWithLineBreak(idx)) {
        idx--
      }
    }
    return if (foundComment) idx else tokenQueue.size
  }

  private fun skipPrecedingCommentsWithSameIndentOnSuiteClose(indent: Int, anchorIndex: Int): Int {
    var result = anchorIndex
    for (i in anchorIndex until tokenQueue.size) {
      val token = tokenQueue[i]
      if (token is PendingCommentToken) {
        if (token.indent < indent) {
          break
        }
        result = i + 1
      }
    }
    return result
  }

  private fun getNextLineIndent(): Int {
    var indent = 0
    while (super.getTokenType() != null && StarlarkTokenSets.WHITESPACE.contains(super.getTokenType())) {
      if (super.getTokenType() === StarlarkTokenTypes.TAB) {
        indent = (indent / 8 + 1) * 8
      } else if (super.getTokenType() === StarlarkTokenTypes.SPACE) {
        indent++
      } else if (super.getTokenType() === StarlarkTokenTypes.LINE_BREAK) {
        indent = 0
      }
      advanceBase()
    }
    return if (super.getTokenType() == null) {
      0
    } else indent
  }

  private fun isBaseAt(tokenType: IElementType): Boolean = super.getTokenType() === tokenType

  private fun pushToken(type: IElementType?, start: Int, end: Int): Boolean =
    tokenQueue.add(PendingToken(type, start, end))

  private fun isPrecededByCommentWithLineBreak(i: Int): Boolean =
    i > 1 && tokenQueue[i - 1].type === StarlarkTokenTypes.LINE_BREAK && tokenQueue[i - 2] is PendingCommentToken
}
