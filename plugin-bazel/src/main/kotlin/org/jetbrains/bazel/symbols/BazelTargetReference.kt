package org.jetbrains.bazel.symbols

import com.intellij.model.Symbol
import com.intellij.model.psi.PsiSymbolReference
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.IncorrectOperationException
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.starlark.psi.StarlarkStringLiteralExpression

/**
 * Reference from a string literal to a Bazel target symbol
 */
class BazelTargetReference(
  element: PsiElement,
  private val targetLabel: String,
  private val rangeInElement: TextRange,
  private val project: Project
) : PsiReferenceBase<PsiElement>(element, rangeInElement), PsiSymbolReference {

  private val resolvedSymbols: Collection<BazelTargetSymbol> by lazy {
    resolveToSymbols()
  }

  override fun resolve(): PsiElement? {
    // For traditional resolve() method, return the first declaration found
    val symbols = resolveToSymbols()
    return symbols.firstOrNull()?.let { findDeclarationElement(it) }
  }

  override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
    val symbols = resolveToSymbols()
    return symbols.mapNotNull { symbol ->
      findDeclarationElement(symbol)?.let { element ->
        PsiElementResolveResult(element)
      }
    }.toTypedArray()
  }

  override fun resolveReference(): Collection<Symbol> {
    return resolveToSymbols()
  }

  override fun resolvesTo(symbol: Symbol): Boolean {
    return symbol is BazelTargetSymbol && resolvedSymbols.contains(symbol)
  }

  override fun getVariants(): Array<Any> {
    // Provide completion variants - all available targets
    val allTargetNames = BazelTargetIndex.getAllTargetNames(project)
    return allTargetNames.map { targetName ->
      LookupElementBuilder.create(targetName)
        .withIcon(org.jetbrains.bazel.assets.BazelPluginIcons.bazel)
        .withTypeText("Bazel Target")
    }.toTypedArray()
  }

  override fun handleElementRename(newElementName: String): PsiElement {
    if (element is StarlarkStringLiteralExpression) {
      // Parse the current label and replace just the target name part
      try {
        val label = Label.parse(targetLabel)
        val newLabel = when (label) {
          is org.jetbrains.bazel.label.ResolvedLabel -> 
            label.copy(target = org.jetbrains.bazel.label.SingleTarget(newElementName))
          is org.jetbrains.bazel.label.RelativeLabel ->
            label.copy(target = org.jetbrains.bazel.label.SingleTarget(newElementName))
          else -> label
        }
        
        // Replace the string content
        return element.updateText("\"${newLabel}\"")
      } catch (e: Exception) {
        throw IncorrectOperationException("Cannot rename target reference", e)
      }
    }
    
    return super.handleElementRename(newElementName)
  }

  private fun resolveToSymbols(): Collection<BazelTargetSymbol> {
    try {
      val label = Label.parse(targetLabel)
      
      // If it's a simple target name, look it up in the index
      if (label is org.jetbrains.bazel.label.RelativeLabel || 
          (label is org.jetbrains.bazel.label.ResolvedLabel && label.repo is org.jetbrains.bazel.label.Main)) {
        
        val targetName = label.targetName
        val targetInfos = BazelTargetIndex.getTargetsByName(targetName, project)
        
        return targetInfos.map { it.toSymbol() }
      }
      
      // For more complex labels, we'd need additional resolution logic
      // TODO: Implement full label resolution with repository mapping
      
    } catch (e: Exception) {
      // Invalid label format
      return emptyList()
    }
    
    return emptyList()
  }

  private fun findDeclarationElement(symbol: BazelTargetSymbol): PsiElement? {
    // Find the PSI element that declares this target
    val file = PsiManager.getInstance(project).findFile(
      com.intellij.openapi.vfs.VfsUtil.findFileByIoFile(
        java.io.File(symbol.buildFilePath), false
      ) ?: return null
    ) ?: return null
    
    // Find the target declaration in the file
    // This is a simplified implementation - in production, we'd use the symbol declaration provider
    if (file is org.jetbrains.bazel.languages.starlark.psi.StarlarkFile) {
      val targetCalls = com.intellij.psi.util.PsiTreeUtil.findChildrenOfType(
        file, 
        org.jetbrains.bazel.languages.starlark.psi.StarlarkCallExpression::class.java
      )
      
      for (call in targetCalls) {
        val nameArg = call.argumentList?.findArgumentByName("name")
        val nameValue = nameArg?.value as? StarlarkStringLiteralExpression
        if (nameValue?.stringValue == symbol.targetName) {
          return nameValue
        }
      }
    }
    
    return null
  }
}

/**
 * Provides references from string literals to Bazel targets
 */
class BazelTargetReferenceProvider : PsiReferenceProvider() {

  override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
    if (element !is StarlarkStringLiteralExpression) {
      return PsiReference.EMPTY_ARRAY
    }
    
    val stringValue = element.stringValue
    if (!looksLikeTargetLabel(stringValue)) {
      return PsiReference.EMPTY_ARRAY
    }
    
    val project = element.project
    val range = TextRange(1, element.textLength - 1) // Exclude quotes
    
    return arrayOf(BazelTargetReference(element, stringValue, range, project))
  }

  private fun looksLikeTargetLabel(value: String): Boolean {
    // Simple heuristics to identify target labels
    return value.startsWith("//") || 
           value.startsWith(":") || 
           (value.contains(":") && !value.contains(" ")) ||
           value.matches(Regex("^[a-zA-Z0-9_-]+$")) // Simple target name
  }
}

/**
 * Contributor that registers the Bazel target reference provider
 */
class BazelTargetReferenceContributor : PsiReferenceContributor() {

  override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
    // Register for string literals in Starlark files
    registrar.registerReferenceProvider(
      com.intellij.patterns.PlatformPatterns.psiElement(StarlarkStringLiteralExpression::class.java)
        .inFile(com.intellij.patterns.PlatformPatterns.psiFile(org.jetbrains.bazel.languages.starlark.psi.StarlarkFile::class.java)),
      BazelTargetReferenceProvider()
    )
  }
}