package org.jetbrains.bazel.symbols

import com.intellij.model.Symbol
import com.intellij.openapi.project.DumbAware
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.IndexableFile
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.Package
import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bazel.label.Main
import org.jetbrains.bazel.label.SingleTarget
import org.jetbrains.bazel.languages.starlark.psi.StarlarkCallExpression
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.psi.StarlarkStringLiteralExpression

/**
 * Provides BazelTargetSymbol instances for indexable files.
 * This is the main entry point for the IntelliJ Symbol API to discover Bazel targets.
 */
class BazelTargetSymbolProvider : com.intellij.model.SymbolProvider, DumbAware {

  override fun getSymbols(indexableFile: IndexableFile): Collection<Symbol> {
    if (!isBazelFile(indexableFile)) {
      return emptyList()
    }

    val symbols = mutableListOf<Symbol>()
    
    try {
      // Get PsiFile for the indexable file
      val psiFile = getPsiFile(indexableFile) ?: return emptyList()
      
      if (psiFile is StarlarkFile && isBuildFile(psiFile)) {
        symbols.addAll(extractTargetsFromBuildFile(psiFile))
      }
      
    } catch (e: Exception) {
      // Log and continue - don't fail indexing for individual files
      // TODO: Add proper logging
    }
    
    return symbols
  }

  private fun isBazelFile(indexableFile: IndexableFile): Boolean {
    val fileName = indexableFile.fileName
    return fileName == "BUILD" || 
           fileName == "BUILD.bazel" ||
           fileName.endsWith(".bzl")
  }

  private fun isBuildFile(file: StarlarkFile): Boolean {
    val fileName = file.name
    return fileName == "BUILD" || fileName == "BUILD.bazel"
  }

  private fun getPsiFile(indexableFile: IndexableFile): PsiFile? {
    // This is a simplified approach - in production code, we'd need proper
    // project context and error handling
    return null // TODO: Implement proper PsiFile retrieval
  }

  private fun extractTargetsFromBuildFile(buildFile: StarlarkFile): List<BazelTargetSymbol> {
    val symbols = mutableListOf<BazelTargetSymbol>()
    val packagePath = getPackagePathFromFile(buildFile)
    val buildFilePath = buildFile.virtualFile?.path ?: return emptyList()
    
    // Find all target rule calls (e.g., java_library, cc_binary, etc.)
    val targetCalls = findTargetRuleCalls(buildFile)
    
    for (call in targetCalls) {
      val targetSymbol = createTargetSymbolFromCall(call, packagePath, buildFilePath)
      if (targetSymbol != null) {
        symbols.add(targetSymbol)
      }
    }
    
    return symbols
  }

  private fun findTargetRuleCalls(file: StarlarkFile): List<StarlarkCallExpression> {
    return PsiTreeUtil.findChildrenOfType(file, StarlarkCallExpression::class.java)
      .filter { isTargetRuleCall(it) }
  }

  private fun isTargetRuleCall(call: StarlarkCallExpression): Boolean {
    val functionName = call.callee?.text ?: return false
    
    // Common Bazel rule types that create targets
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
    
    return knownRuleTypes.contains(functionName) || 
           // Also include any call that has a 'name' parameter, which is typical for Bazel rules
           hasNameParameter(call)
  }

  private fun hasNameParameter(call: StarlarkCallExpression): Boolean {
    return call.argumentList?.findArgumentByName("name") != null
  }

  private fun createTargetSymbolFromCall(
    call: StarlarkCallExpression,
    packagePath: List<String>,
    buildFilePath: String
  ): BazelTargetSymbol? {
    val ruleName = call.callee?.text ?: return null
    val targetName = extractTargetName(call) ?: return null
    
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
    // Extract aliases from 'alias' rules or other sources
    val aliases = mutableSetOf<String>()
    
    if (call.callee?.text == "alias") {
      // For alias rules, the alias name itself becomes an alias
      val aliasName = extractTargetName(call)
      if (aliasName != null) {
        aliases.add(aliasName)
      }
    }
    
    // TODO: Add support for extracting aliases from comments or other metadata
    
    return aliases
  }

  private fun getPackagePathFromFile(file: StarlarkFile): List<String> {
    val virtualFile = file.virtualFile ?: return emptyList()
    val filePath = virtualFile.path
    
    // Find the workspace root by looking for WORKSPACE or MODULE.bazel files
    var currentDir = virtualFile.parent
    while (currentDir != null) {
      val children = currentDir.children
      if (children.any { it.name == "WORKSPACE" || it.name == "MODULE.bazel" }) {
        // Found workspace root, calculate relative package path
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