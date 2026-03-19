package org.jetbrains.bazel.run.synthetic

import com.intellij.openapi.application.ReadAction
import com.intellij.psi.PsiElement
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.ExecutableTarget

@ApiStatus.Internal
abstract class MainClassSyntheticRunTargetTemplateGenerator : SyntheticRunTargetTemplateGenerator {
  private val DEFAULT_TARGET_NAME = "synthetic_binary"

  override fun isSupported(target: ExecutableTarget): Boolean = true

  override fun getRunnerActionName(
    original: String,
    target: ExecutableTarget,
    element: PsiElement,
  ): String = "$original ${element.getMainClassInternal() ?: "unknown"}"

  override fun getSyntheticTargetLabel(original: ExecutableTarget, element: PsiElement): Label {
    val mainClass = element.getMainClassInternal() ?: error("failed to get main class")
    val pkg = getTargetPath(original, mainClass)
    return SyntheticRunTargetUtils.getSyntheticTargetLabel(packageParts = pkg, targetName = DEFAULT_TARGET_NAME)
  }

  override fun getSyntheticParams(target: ExecutableTarget, element: PsiElement): SyntheticRunTargetTemplateGenerator.Params {
    val mainClass = element.getMainClassInternal() ?: error("failed to get main class")
    return SyntheticRunTargetTemplateGenerator.Params(mainClass)
  }

  override fun createSyntheticTemplate(
    target: ExecutableTarget,
    params: SyntheticRunTargetTemplateGenerator.Params,
  ): SyntheticRunTargetTemplate {
    val mainClass = params.data ?: error("failed to get main class")
    val build = getBuildContent(target, DEFAULT_TARGET_NAME, mainClass)
    val pkg = getTargetPath(target, mainClass)
    return SyntheticRunTargetTemplate(buildFileContent = build, buildFilePath = pkg.joinToString("/"))
  }

  fun getTargetPath(original: ExecutableTarget, mainClass: String): Array<String> {
    return arrayOf(
      SyntheticRunTargetUtils.escapeTargetLabel(original.id.toString()),
      SyntheticRunTargetUtils.escapeTargetLabel(mainClass),
    )
  }

  fun PsiElement.getMainClassInternal(): String? = ReadAction.compute<String?, Throwable> {
    return@compute getMainClass(this@getMainClassInternal)
  }

  protected abstract fun getMainClass(element: PsiElement): String?
  protected abstract fun getBuildContent(target: ExecutableTarget, syntheticTarget: String, mainClass: String): String
}
