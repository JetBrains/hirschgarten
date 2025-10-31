package org.jetbrains.bazel.run.synthetic

import com.intellij.openapi.application.readAction
import com.intellij.psi.PsiElement
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.BuildTarget

abstract class MainClassSyntheticRunTargetTemplateGenerator : SyntheticRunTargetTemplateGenerator {
  private val DEFAULT_TARGET_NAME = "synthetic_binary"

  override fun isSupported(target: BuildTarget): Boolean = true

  override fun getRunnerActionName(
    original: String,
    target: BuildTarget,
    element: PsiElement,
  ): String = "$original ${element.getMainClassInternal() ?: "unknown"}"

  override fun getSyntheticTargetLabel(original: BuildTarget, element: PsiElement): Label {
    // TODO: rethink fallback values
    val mainClass = element.getMainClassInternal() ?: error("failed to get main class")
    val pkg = getTargetPath(original, mainClass) ?: error("failed to get target path")
    return SyntheticRunTargetUtils.getSyntheticTargetLabel(packageParts = pkg, targetName = DEFAULT_TARGET_NAME)
  }

  override fun getSyntheticParams(target: BuildTarget, element: PsiElement): String {
    return element.getMainClassInternal() ?: error("failed to get main class")
  }

  override fun createSyntheticTemplate(
    target: BuildTarget,
    params: String,
  ): SyntheticRunTargetTemplate {
    val build = getBuildContent(target, DEFAULT_TARGET_NAME, params)
    val pkg = getTargetPath(target, params) ?: error("failed to get target path")
    return SyntheticRunTargetTemplate(buildFileContent = build, buildFilePath = pkg.joinToString("/"))
  }

  fun getTargetPath(original: BuildTarget, mainClass: String): Array<String> {
    return arrayOf(
      SyntheticRunTargetUtils.escapeTargetLabel(original.id.toString()),
      SyntheticRunTargetUtils.escapeTargetLabel(mainClass),
    )
  }

  fun PsiElement.getMainClassInternal(): String? = runBlocking {
    readAction {
      return@readAction getMainClass(this@getMainClassInternal)
    }
  }

  protected abstract fun getMainClass(element: PsiElement): String?
  protected abstract fun getBuildContent(target: BuildTarget, syntheticTarget: String, mainClass: String): String
}
