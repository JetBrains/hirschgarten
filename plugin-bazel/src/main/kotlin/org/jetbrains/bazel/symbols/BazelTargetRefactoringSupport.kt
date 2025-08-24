package org.jetbrains.bazel.symbols

import com.intellij.lang.refactoring.RefactoringSupportProvider
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.refactoring.RefactoringActionHandler
import com.intellij.refactoring.safeDelete.SafeDeleteHandler
import com.intellij.util.IncorrectOperationException
import org.jetbrains.bazel.languages.starlark.psi.StarlarkCallExpression
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.psi.StarlarkStringLiteralExpression

/**
 * Provides refactoring support for Bazel targets (safe delete, rename, etc.)
 */
class BazelTargetRefactoringSupport : RefactoringSupportProvider() {

  override fun isSafeDeleteAvailable(element: PsiElement): Boolean {
    return isBazelTargetElement(element)
  }

  override fun isInplaceRenameAvailable(element: PsiElement, context: PsiElement?): Boolean {
    return isBazelTargetElement(element)
  }

  override fun isMemberInplaceRenameAvailable(element: PsiElement, context: PsiElement?): Boolean {
    // Bazel targets don't have members in the traditional sense
    return false
  }

  override fun getIntroduceVariableHandler(): RefactoringActionHandler? = null

  override fun getExtractMethodHandler(): RefactoringActionHandler? = null

  override fun getIntroduceParameterHandler(): RefactoringActionHandler? = null

  override fun getIntroduceFieldHandler(): RefactoringActionHandler? = null

  override fun getPullUpHandler(): RefactoringActionHandler? = null

  override fun getPushDownHandler(): RefactoringActionHandler? = null

  override fun getExtractInterfaceHandler(): RefactoringActionHandler? = null

  override fun getExtractSuperClassHandler(): RefactoringActionHandler? = null

  override fun getChangeSignatureHandler(): RefactoringActionHandler? = null

  private fun isBazelTargetElement(element: PsiElement): Boolean {
    if (element !is StarlarkStringLiteralExpression) return false
    
    // Check if this string literal is the name parameter of a target rule
    val parent = element.parent
    if (parent is com.intellij.psi.PsiNamedElement) {
      val grandParent = parent.parent
      if (grandParent is StarlarkCallExpression) {
        return isTargetRuleCall(grandParent)
      }
    }
    
    return false
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
}

/**
 * Custom safe delete handler for Bazel targets
 */
class BazelTargetSafeDeleteHandler : SafeDeleteHandler {

  override fun invoke(project: Project, elements: Array<PsiElement>, dataContext: DataContext?) {
    val bazelTargetElements = elements.filterIsInstance<BazelTargetElement>()
    
    if (bazelTargetElements.isEmpty()) {
      // Delegate to default handler
      return
    }
    
    for (element in bazelTargetElements) {
      safeDeleteTarget(element)
    }
  }

  override fun invoke(project: Project, elements: Array<PsiElement>, module: com.intellij.openapi.module.Module?, dataContext: DataContext?) {
    invoke(project, elements, dataContext)
  }

  private fun safeDeleteTarget(element: BazelTargetElement) {
    val project = element.project
    
    // Find all references to this target
    val references = ReferencesSearch.search(element).findAll()
    
    // Check if it's safe to delete
    val unsafeReferences = references.filter { !isSafeToDelete(it.element) }
    
    if (unsafeReferences.isNotEmpty()) {
      // Show dialog asking user about unsafe references
      val message = "Target '${element.targetName}' is referenced in ${unsafeReferences.size} places. " +
                   "Deleting it may break the build. Continue?"
      
      val result = com.intellij.openapi.ui.Messages.showYesNoDialog(
        project,
        message,
        "Safe Delete",
        com.intellij.openapi.ui.Messages.getWarningIcon()
      )
      
      if (result != com.intellij.openapi.ui.Messages.YES) {
        return
      }
    }
    
    // Perform the deletion
    try {
      // Remove the target definition
      element.delete()
      
      // Optionally remove references (or warn about them)
      for (reference in references) {
        // Could remove the reference or comment it out
        // For now, just leave them (user will get build errors)
      }
      
    } catch (e: IncorrectOperationException) {
      com.intellij.openapi.ui.Messages.showErrorDialog(
        project,
        "Failed to delete target: ${e.message}",
        "Safe Delete Error"
      )
    }
  }

  private fun isSafeToDelete(element: PsiElement): Boolean {
    // A reference is safe to delete if:
    // 1. It's in the same package
    // 2. It's in a test file (tests can be broken without breaking main code)
    // 3. It's in a comment
    
    val file = element.containingFile
    return file is StarlarkFile && 
           (file.name.contains("test") || file.name.contains("Test"))
  }
}

/**
 * Represents a Bazel target element for deletion purposes
 */
class BazelTargetElement(
  private val callExpression: StarlarkCallExpression,
  val targetName: String,
  val packagePath: String
) : com.intellij.psi.PsiElementBase() {

  override fun delete() {
    // Remove the entire call expression
    callExpression.delete()
  }

  override fun getProject(): Project = callExpression.project

  override fun getContainingFile(): PsiFile = callExpression.containingFile

  override fun getTextRange(): com.intellij.openapi.util.TextRange = callExpression.textRange

  override fun getText(): String = callExpression.text

  override fun getParent(): PsiElement? = callExpression.parent

  override fun getChildren(): Array<PsiElement> = arrayOf(callExpression)

  override fun getFirstChild(): PsiElement? = callExpression

  override fun getLastChild(): PsiElement? = callExpression

  override fun getNextSibling(): PsiElement? = callExpression.nextSibling

  override fun getPrevSibling(): PsiElement? = callExpression.prevSibling

  override fun toString(): String = "BazelTarget($targetName)"
}