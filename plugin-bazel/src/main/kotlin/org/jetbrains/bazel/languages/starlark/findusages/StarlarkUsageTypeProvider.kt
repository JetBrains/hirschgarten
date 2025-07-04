package org.jetbrains.bazel.languages.starlark.findusages

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.usages.impl.rules.UsageType
import com.intellij.usages.impl.rules.UsageTypeProvider
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkLoadStatement

private val CODE_USAGE = UsageType { BazelPluginBundle.message("findusages.starlark.code.usages") }
private val LOAD_USAGE = UsageType { BazelPluginBundle.message("findusages.starlark.load.usages") }

class StarlarkUsageTypeProvider : UsageTypeProvider {
  override fun getUsageType(element: PsiElement): UsageType =
    if (PsiTreeUtil.getParentOfType(element, StarlarkLoadStatement::class.java) != null) {
      LOAD_USAGE
    } else {
      CODE_USAGE
    }
}
