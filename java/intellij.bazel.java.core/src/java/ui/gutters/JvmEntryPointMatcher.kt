package org.jetbrains.bazel.java.ui.gutters

import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.ClassUtil
import com.intellij.psi.util.isAncestor
import com.intellij.util.ThreeState
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.languages.jvm.JAVA_BINARY_KIND
import org.jetbrains.bazel.sync.workspace.languages.jvm.JAVA_TEST_KIND
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget
import org.jetbrains.bazel.sync.workspace.languages.jvm.KT_JVM_TEST_KIND
import org.jetbrains.bazel.sync.workspace.languages.jvm.rulesJavaPrimaryClass
import org.jetbrains.bazel.sync.workspace.languages.jvm.rulesKotlinTestClass
import org.jetbrains.bazel.target.getBuildTarget
import org.jetbrains.bazel.target.getTargetDataForLabel
import org.jetbrains.bazel.target.targetStorage
import org.jetbrains.bazel.ui.gutters.BazelEntryPointMatcher
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetInfo

/**
 * Matches the test class of a JVM test, or the main class of a JVM binary, with the classes that hold a PSI element.
 * The matcher compares the class that Bazel runs. It takes a derived class from the path, as Bazel does, and not from the PSI of the file.
 */
internal class JvmEntryPointMatcher : BazelEntryPointMatcher {

  override fun match(element: PsiElement, targets: List<BuildTargetInfo>): Map<Label, ThreeState> {
    val file = element.containingFile as? PsiClassOwner
    if (file == null || DumbService.isDumb(file.project)) return targets.associate { it.id to ThreeState.UNSURE }
    val classNames by lazy { collectClassNames(file.classes, element) }
    return targets.map { target ->
      val entryPoint = entryPointClass(file.project, target) ?: return@map target.id to ThreeState.UNSURE
      target.id to when (entryPoint) {
        in classNames -> ThreeState.YES
        else -> ThreeState.NO
      }
    }.toMap()
  }

  /**
   * Returns the binary names of the [classes] and their inner classes that hold [element] or that are in [element].
   * Bazel gives `test_class` and `main_class` to `Class.forName`, so they are binary names, such as `com.foo.Outer$Inner`.
   *
   * A Kotlin file facade class counts only when no declared class holds [element], as for a top-level function.
   *
   * It handles Java and Kotlin. For a Kotlin file, [PsiClassOwner.getClasses] returns light classes, which are not in the PSI tree of the file.
   * [PsiClass.getNavigationElement] gives the actual Kotlin PSI, so the matcher can compare it with [element].
   */
  private fun collectClassNames(classes: Array<PsiClass>, element: PsiElement): Set<String> {
    val (facadeClasses, declaredClasses) = classes.asSequence()
      .flatMap { it.withInnerClasses() }
      .partition { it.navigationElement is PsiFile }
    val relatedClasses = declaredClasses.filter { element.isAncestor(it.navigationElement) || it.navigationElement.isAncestor(element) }
    val isInDeclaredClass = relatedClasses.any { it.navigationElement.isAncestor(element) }
    val matchingClasses = if (isInDeclaredClass) relatedClasses else relatedClasses + facadeClasses
    return matchingClasses.mapNotNullTo(HashSet()) { ClassUtil.getBinaryClassName(it) }
  }

  private fun PsiClass.withInnerClasses(): Sequence<PsiClass> =
    sequenceOf(this) + innerClasses.asSequence().flatMap { it.withInnerClasses() }

  private fun entryPointClass(project: Project, target: BuildTargetInfo): String? {
    val jvmTarget = project.targetStorage.getTargetDataForLabel<JvmBuildTarget>(target.id) ?: return null
    return when (target.kind.ruleType) {
      RuleType.TEST -> jvmTarget.testClass ?: derivedClass(project, target, jvmTarget)
      RuleType.BINARY -> jvmTarget.mainClass ?: derivedClass(project, target, jvmTarget)
      else -> null
    }
  }

  private fun derivedClass(project: Project, target: BuildTargetInfo, jvmTarget: JvmBuildTarget): String? = when (target.kind.kind) {
    // For a `java_test` with a custom runner in `main_class` and no `test_class`, the matcher does not guess the test class.
    // `rules_kotlin` sets a default `main_class` on each `kt_jvm_test`, so the check is only for `java_test`.
    JAVA_TEST_KIND.kind if jvmTarget.mainClass == null -> loadTarget(project, target)?.let { rulesJavaPrimaryClass(it) }
    JAVA_BINARY_KIND.kind -> loadTarget(project, target)?.let { rulesJavaPrimaryClass(it) }
    KT_JVM_TEST_KIND.kind -> loadTarget(project, target)?.let { rulesKotlinTestClass(it) }
    else -> null
  }

  private fun loadTarget(project: Project, target: BuildTargetInfo): BuildTarget? = project.targetStorage.getBuildTarget(target.id)
}
