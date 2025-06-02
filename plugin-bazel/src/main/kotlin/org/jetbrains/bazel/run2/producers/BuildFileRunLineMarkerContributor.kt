/*
 * Copyright 2020 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.bazel.run2.producers

import com.google.common.collect.ImmutableSet
import com.google.idea.blaze.base.lang.buildfile.psi.BuildFile
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.source.tree.LeafElement
import com.intellij.util.Function
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.bazel.commons.RuleType

/** Generates run/debug gutter icons for BUILD files.  */
class BuildFileRunLineMarkerContributor : RunLineMarkerContributor() {
  override fun producesAllPossibleConfigurations(file: PsiFile): Boolean = false

  override fun getInfo(element: PsiElement): Info? {
    if (!isRunContext(element)) {
      return null
    }
    val actions: Array<AnAction> = getActions.getActions()
    return Info(
      AllIcons.RunConfigurations.TestState.Run,
      actions,
      java.util.function.Function { psiElement: PsiElement? ->
        StringUtil.join(
          ContainerUtil.mapNotNull<AnAction?, String?>(
            actions,
            Function { action: AnAction? -> RunLineMarkerContributor.getText(action!!, psiElement!!) },
          ),
          "\n",
        )
      },
    )
  }

  companion object {
    private val HANDLED_RULE_TYPES: ImmutableSet<RuleType> = ImmutableSet.of<RuleType>(RuleType.TEST, RuleType.BINARY)

    private fun isRunContext(element: PsiElement): Boolean {
      val rule: FuncallExpression = getRuleFuncallExpression(element) ?: return false
      val data: BuildTarget = BlazeBuildFileRunConfigurationProducer.getBuildTarget(rule) ?: return false
      return true // We want to put a gutter icon next to each target to provide a starlark debugger action
    }

    private fun getRuleFuncallExpression(element: PsiElement): FuncallExpression? {
      val parentFile = element.getContainingFile()
      if (parentFile !is BuildFile || (parentFile as BuildFile).getBlazeFileType() !== BlazeFileType.BuildPackage) {
        return null
      }
      if ((element !is LeafElement) ||
        element is PsiWhiteSpace ||
        element is PsiComment
      ) {
        return null
      }
      if (element.getParent() !is ReferenceExpression) {
        return null
      }
      val grandParent = element.getParent().getParent()
      return if (grandParent is FuncallExpression &&
        (grandParent as FuncallExpression).isTopLevel()
      ) {
        grandParent as FuncallExpression
      } else {
        null
      }
    }
  }
}
