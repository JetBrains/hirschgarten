package org.jetbrains.bazel.symbols

import com.intellij.model.psi.PsiSymbolDeclaration
import com.intellij.model.psi.PsiSymbolDeclarationProvider
import com.intellij.model.Symbol
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.bazel.label.Package
import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bazel.label.Main
import org.jetbrains.bazel.label.SingleTarget
import org.jetbrains.bazel.languages.starlark.psi.StarlarkCallExpression
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.psi.StarlarkStringLiteralExpression

/**
 * Declaration of a Bazel target symbol, linking the symbol to its declaring PSI element.
 */
class BazelTargetSymbolDeclaration(
  private val declaringElement: PsiElement,
  private val symbol: BazelTargetSymbol,
  private val rangeInElement: TextRange
) : PsiSymbolDeclaration {

  override fun getSymbol(): Symbol = symbol

  override fun getDeclaringElement(): PsiElement = declaringElement

  override fun getRangeInDeclaringElement(): TextRange = rangeInElement

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as BazelTargetSymbolDeclaration
    return symbol == other.symbol && declaringElement == other.declaringElement
  }

  override fun hashCode(): Int {
    var result = symbol.hashCode()
    result = 31 * result + declaringElement.hashCode()
    return result
  }
}

/**
 * Provides symbol declarations for Bazel target elements in BUILD files.
 */
class BazelTargetSymbolDeclarationProvider : PsiSymbolDeclarationProvider {

  override fun getDeclarations(element: PsiElement, offsetInElement: Int): Collection<PsiSymbolDeclaration> {
    // Only provide declarations for elements in BUILD files
    val file = element.containingFile
    if (!isBuildFile(file)) {
      return emptyList()
    }

    // Check if this element is a target rule call or contains one
    val targetCall = findTargetCall(element) ?: return emptyList()
    
    // Check if the offset is within the target name parameter
    val nameParam = findNameParameter(targetCall) ?: return emptyList()
    val nameValue = nameParam.value as? StarlarkStringLiteralExpression ?: return emptyList()
    
    // Create symbol for this target
    val symbol = createSymbolForTarget(targetCall, file) ?: return emptyList()
    
    // Calculate range within the name string literal
    val range = TextRange(0, nameValue.textLength)
    
    return listOf(BazelTargetSymbolDeclaration(nameValue, symbol, range))
  }

  private fun isBuildFile(file: PsiElement?): Boolean {
    if (file !is StarlarkFile) return false
    val fileName = file.name
    return fileName == "BUILD" || fileName == "BUILD.bazel"
  }

  private fun findTargetCall(element: PsiElement): StarlarkCallExpression? {
    // If element is already a call expression, check if it's a target rule
    if (element is StarlarkCallExpression && isTargetRuleCall(element)) {
      return element
    }
    
    // Check if element is inside a target rule call
    val parentCall = PsiTreeUtil.getParentOfType(element, StarlarkCallExpression::class.java)
    if (parentCall != null && isTargetRuleCall(parentCall)) {
      return parentCall
    }
    
    return null
  }

  private fun isTargetRuleCall(call: StarlarkCallExpression): Boolean {
    val functionName = call.callee?.text ?: return false
    
    val knownRuleTypes = setOf(
      "java_binary", "java_library", "java_test",
      "cc_binary", "cc_library", "cc_test",
      "py_binary", "py_library", "py_test",
      "go_binary", "go_library", "go_test",
      "kt_jvm_binary", "kt_jvm_library", "kt_jvm_test",
      "genrule", "filegroup", "alias", "proto_library",
      "android_binary", "android_library", "android_test",
      "sh_binary", "sh_library", "sh_test"
    )
    
    return knownRuleTypes.contains(functionName) || hasNameParameter(call)
  }

  private fun hasNameParameter(call: StarlarkCallExpression): Boolean {
    return call.argumentList?.findArgumentByName("name") != null
  }

  private fun findNameParameter(call: StarlarkCallExpression): PsiElement? {
    return call.argumentList?.findArgumentByName("name")
  }

  private fun createSymbolForTarget(call: StarlarkCallExpression, file: PsiElement): BazelTargetSymbol? {
    val ruleName = call.callee?.text ?: return null
    val targetName = extractTargetName(call) ?: return null
    
    val packagePath = getPackagePathFromFile(file)
    val buildFilePath = file.containingFile?.virtualFile?.path ?: return null
    
    val label = ResolvedLabel(
      repo = Main,
      packagePath = Package(packagePath),
      target = SingleTarget(targetName)
    )
    
    val targetType = BazelTargetType.fromRuleName(ruleName)
    val aliases = extractAliases(call)
    
    return BazelTargetSymbol(
      label = label,
      buildFilePath = buildFilePath,
      targetType = targetType,
      aliases = aliases
    )
  }

  private fun extractTargetName(call: StarlarkCallExpression): String? {
    val nameArg = call.argumentList?.findArgumentByName("name") ?: return null
    val nameValue = nameArg.value as? StarlarkStringLiteralExpression ?: return null
    return nameValue.stringValue
  }

  private fun extractAliases(call: StarlarkCallExpression): Set<String> {
    val aliases = mutableSetOf<String>()
    
    if (call.callee?.text == "alias") {
      val aliasName = extractTargetName(call)
      if (aliasName != null) {
        aliases.add(aliasName)
      }
    }
    
    return aliases
  }

  private fun getPackagePathFromFile(element: PsiElement): List<String> {
    val file = element.containingFile
    val virtualFile = file?.virtualFile ?: return emptyList()
    val filePath = virtualFile.path
    
    var currentDir = virtualFile.parent
    while (currentDir != null) {
      val children = currentDir.children
      if (children.any { it.name == "WORKSPACE" || it.name == "MODULE.bazel" }) {
        val workspacePath = currentDir.path
        val relativePath = filePath.removePrefix(workspacePath).removePrefix("/")
        val packageDir = relativePath.removeSuffix("/${file.name}")
        
        return if (packageDir.isEmpty()) {
          emptyList()
        } else {
          packageDir.split("/").filter { it.isNotEmpty() }
        }
      }
      currentDir = currentDir.parent
    }
    
    return emptyList()
  }
}