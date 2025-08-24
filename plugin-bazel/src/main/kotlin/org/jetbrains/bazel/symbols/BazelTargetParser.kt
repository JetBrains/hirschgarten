package org.jetbrains.bazel.symbols

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.bazel.languages.starlark.psi.*

/**
 * Utility for parsing Bazel target information from BUILD file content
 */
object BazelTargetParser {

  /**
   * Parse targets from BUILD file content
   */
  fun parseTargetsFromContent(content: String, file: VirtualFile): List<BazelTargetInfo> {
    val targets = mutableListOf<BazelTargetInfo>()
    
    try {
      // Get package path from file location
      val packagePath = getPackagePathFromFile(file)
      val buildFilePath = file.path
      
      // Simple regex-based parsing for now - in production, we'd use the PSI
      val rulePattern = Regex("""(\w+)\s*\(\s*name\s*=\s*["']([^"']+)["']([^)]*)\)""", RegexOption.DOT_MATCHES_ALL)
      
      val matches = rulePattern.findAll(content)
      
      for (match in matches) {
        val ruleName = match.groupValues[1]
        val targetName = match.groupValues[2]
        val params = match.groupValues[3]
        
        // Skip if this doesn't look like a target rule
        if (!isTargetRule(ruleName)) continue
        
        val targetType = BazelTargetType.fromRuleName(ruleName)
        val dependencies = extractDependencies(params)
        val aliases = extractAliases(ruleName, targetName, params)
        
        targets.add(BazelTargetInfo(
          targetName = targetName,
          packagePath = packagePath,
          buildFilePath = buildFilePath,
          targetType = targetType,
          ruleName = ruleName,
          aliases = aliases,
          dependencies = dependencies
        ))
      }
      
    } catch (e: Exception) {
      // Log error but don't fail
      // TODO: Add proper logging
    }
    
    return targets
  }

  /**
   * Parse targets from a Starlark file using PSI
   */
  fun parseTargetsFromStarlarkFile(file: StarlarkFile): List<BazelTargetInfo> {
    val targets = mutableListOf<BazelTargetInfo>()
    
    val packagePath = getPackagePathFromPsiFile(file)
    val buildFilePath = file.virtualFile?.path ?: return emptyList()
    
    val targetCalls = PsiTreeUtil.findChildrenOfType(file, StarlarkCallExpression::class.java)
      .filter { isTargetRuleCall(it) }
    
    for (call in targetCalls) {
      val target = createTargetInfoFromCall(call, packagePath, buildFilePath)
      if (target != null) {
        targets.add(target)
      }
    }
    
    return targets
  }

  private fun isTargetRule(ruleName: String): Boolean {
    val knownRuleTypes = setOf(
      "java_binary", "java_library", "java_test",
      "cc_binary", "cc_library", "cc_test", 
      "py_binary", "py_library", "py_test",
      "go_binary", "go_library", "go_test",
      "kt_jvm_binary", "kt_jvm_library", "kt_jvm_test",
      "genrule", "filegroup", "alias", "proto_library",
      "android_binary", "android_library", "android_test",
      "sh_binary", "sh_library", "sh_test",
      "scala_binary", "scala_library", "scala_test"
    )
    
    return knownRuleTypes.contains(ruleName) || 
           ruleName.endsWith("_binary") || 
           ruleName.endsWith("_library") || 
           ruleName.endsWith("_test")
  }

  private fun isTargetRuleCall(call: StarlarkCallExpression): Boolean {
    val functionName = call.callee?.text ?: return false
    return isTargetRule(functionName) || hasNameParameter(call)
  }

  private fun hasNameParameter(call: StarlarkCallExpression): Boolean {
    return call.argumentList?.findArgumentByName("name") != null
  }

  private fun createTargetInfoFromCall(
    call: StarlarkCallExpression,
    packagePath: String,
    buildFilePath: String
  ): BazelTargetInfo? {
    val ruleName = call.callee?.text ?: return null
    val targetName = extractTargetName(call) ?: return null
    
    val targetType = BazelTargetType.fromRuleName(ruleName)
    val dependencies = extractDependenciesFromCall(call)
    val aliases = extractAliasesFromCall(call, ruleName, targetName)
    
    return BazelTargetInfo(
      targetName = targetName,
      packagePath = packagePath,
      buildFilePath = buildFilePath,
      targetType = targetType,
      ruleName = ruleName,
      aliases = aliases,
      dependencies = dependencies
    )
  }

  private fun extractTargetName(call: StarlarkCallExpression): String? {
    val nameArg = call.argumentList?.findArgumentByName("name") ?: return null
    val nameValue = nameArg.value as? StarlarkStringLiteralExpression ?: return null
    return nameValue.stringValue
  }

  private fun extractDependenciesFromCall(call: StarlarkCallExpression): List<String> {
    val dependencies = mutableListOf<String>()
    
    // Look for 'deps' parameter
    val depsArg = call.argumentList?.findArgumentByName("deps")
    if (depsArg?.value is StarlarkListLiteralExpression) {
      val depsList = depsArg.value as StarlarkListLiteralExpression
      for (element in depsList.elements) {
        if (element is StarlarkStringLiteralExpression) {
          dependencies.add(element.stringValue)
        }
      }
    }
    
    return dependencies
  }

  private fun extractAliasesFromCall(call: StarlarkCallExpression, ruleName: String, targetName: String): Set<String> {
    val aliases = mutableSetOf<String>()
    
    if (ruleName == "alias") {
      // For alias rules, the name is the alias
      aliases.add(targetName)
    }
    
    // Look for additional alias information in comments or attributes
    // TODO: Add more sophisticated alias extraction
    
    return aliases
  }

  private fun extractDependencies(params: String): List<String> {
    val dependencies = mutableListOf<String>()
    
    // Simple regex to extract deps = [...] lists
    val depsPattern = Regex("""deps\s*=\s*\[(.*?)\]""", RegexOption.DOT_MATCHES_ALL)
    val depsMatch = depsPattern.find(params)
    
    if (depsMatch != null) {
      val depsContent = depsMatch.groupValues[1]
      val depPattern = Regex("""["']([^"']+)["']""")
      val depMatches = depPattern.findAll(depsContent)
      
      for (match in depMatches) {
        dependencies.add(match.groupValues[1])
      }
    }
    
    return dependencies
  }

  private fun extractAliases(ruleName: String, targetName: String, params: String): Set<String> {
    val aliases = mutableSetOf<String>()
    
    if (ruleName == "alias") {
      aliases.add(targetName)
    }
    
    return aliases
  }

  private fun getPackagePathFromFile(file: VirtualFile): String {
    val filePath = file.path
    
    // Find workspace root
    var currentDir = file.parent
    while (currentDir != null) {
      val children = currentDir.children
      if (children.any { it.name == "WORKSPACE" || it.name == "MODULE.bazel" }) {
        // Found workspace root
        val workspacePath = currentDir.path
        val relativePath = filePath.removePrefix(workspacePath).removePrefix("/")
        val packageDir = relativePath.removeSuffix("/${file.name}")
        
        return packageDir.takeIf { it.isNotEmpty() } ?: ""
      }
      currentDir = currentDir.parent
    }
    
    return ""
  }

  private fun getPackagePathFromPsiFile(file: StarlarkFile): String {
    val virtualFile = file.virtualFile ?: return ""
    return getPackagePathFromFile(virtualFile)
  }
}