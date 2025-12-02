package org.jetbrains.bazel.languages.starlark.psi.expressions

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.bazelversion.psi.toSemVer
import org.jetbrains.bazel.languages.bazelversion.service.BazelVersionCheckerService
import org.jetbrains.bazel.languages.starlark.StarlarkConstants.ALLOW_EMPTY_KEYWORD
import org.jetbrains.bazel.languages.starlark.elements.StarlarkElementTypes
import org.jetbrains.bazel.languages.starlark.globbing.StarlarkGlob
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkArgumentElement
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkArgumentExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkNamedArgumentExpression
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkArgumentList
import org.jetbrains.bazel.languages.starlark.references.StarlarkGlobReference
import kotlin.concurrent.Volatile

class StarlarkGlobExpression(node: ASTNode) : StarlarkBaseElement(node) {

  val bazelVersion: Int?
    get() = node.psi.project.getService(BazelVersionCheckerService::class.java).currentBazelVersion?.toSemVer()?.major

  override fun acceptVisitor(visitor: StarlarkElementVisitor) = visitor.visitGlobExpression(this)

  fun getArgList(): StarlarkArgumentList? =
    node.treeParent.findChildByType(StarlarkElementTypes.ARGUMENT_LIST)?.psi as StarlarkArgumentList?

  fun getArguments(): Array<StarlarkArgumentElement> {
    val argList = getArgList()
    if (argList == null) return arrayOf()
    return argList.getArguments()
  }

  fun getKeywordArgument(name: String): StarlarkNamedArgumentExpression? {
    val list = getArgList()
    return list?.getKeywordArgument(name)
  }

  fun getIncludes(): PsiElement? {
    var arg: StarlarkArgumentElement? = getKeywordArgument("include")
    if (arg == null) {
      val allArgs = getArguments()
      if (allArgs.size != 0 && allArgs[0] is StarlarkArgumentExpression) {
        arg = allArgs[0]
      }
    }
    return getArgValue(arg)
  }

  fun getExcludes(): PsiElement? = getArgValue(getKeywordArgument("exclude"))

  private fun getArgValue(arg: StarlarkArgumentElement?): PsiElement? = arg?.getValue()

  fun areDirectoriesExcluded(): Boolean {
    val arg = getKeywordArgument("exclude_directories")
    if (arg != null) {
      // '0' and '1' are the only accepted values
      val value = getArgValue(arg)
      return value == null || !value.text.equals("0")
    }
    return true
  }

  @Volatile
  private var reference: StarlarkGlobReference? = null

  override fun getReference(): StarlarkGlobReference? {
    val ref: StarlarkGlobReference? = reference
    if (ref != null) {
      return ref
    }
    synchronized(this) {
      if (reference == null) {
        reference = StarlarkGlobReference(this)
      }
      return reference
    }
  }

  /**
   * The glob is valid if it resolves to a non-empty set of files,
   * or the 'allow_empty' argument is set to true.
   */
  fun isGlobValid(): Boolean {
    if (isAllowedEmpty()) return true

    val matchedFiles = StarlarkGlobReference(this).multiResolve(false)
    return matchedFiles.isNotEmpty()
  }

  /**
   * The pattern is valid if matches at least one file
   * or the 'allow_empty' argument is set to true.
   */
  fun isPatternValid(element: StarlarkStringLiteralExpression): Boolean {
    if (isAllowedEmpty()) return true
    val pattern = element.getStringContents()
    val containingDirectory = containingFile.parent?.virtualFile ?: return true
    return try {
      StarlarkGlob
        .forPath(containingDirectory)
        .addPattern(pattern)
        .glob()
        .isNotEmpty()
    } catch (e: IllegalArgumentException) {
      true
    }
  }

  private fun isAllowedEmpty(): Boolean {
    val defaultAllowEmptyTrue =  bazelVersion?.let { it < 8 } ?: false
    val allowEmpty: String? = getArgValue(getKeywordArgument(ALLOW_EMPTY_KEYWORD))?.text
    return allowEmpty?.lowercase()?.toBooleanStrictOrNull() ?: defaultAllowEmptyTrue
  }
}
