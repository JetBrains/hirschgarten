/*
 * Copyright 2018 The Bazel Authors. All rights reserved.
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
package org.jetbrains.bazel.ogRun.producers

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiQualifiedNamedElement
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.ogRun.BlazeCommandRunConfiguration
import org.jetbrains.bazel.ogRun.other.BlazeCommandName
import org.jetbrains.bazel.ogRun.state.BlazeCommandRunConfigurationCommonState

/** A context used to configure a blaze run configuration, possibly asynchronously.  */
interface RunConfigurationContext {
  /** The [PsiElement] most relevant to this context (e.g. a method, class, file, etc.).  */
  val sourceElement: PsiElement

  val sourceElementString: String
    /** Convert a [.getSourceElement] into an uniquely identifiable string.  */
    get() {
      if (!ApplicationManager.getApplication().isReadAccessAllowed) {
        return ReadAction.compute<String, RuntimeException> { this.sourceElementString }
      }
      val element = this.sourceElement
      if (element is PsiFile) {
        return element.virtualFile?.path ?: element.toString()
      }
      val path =
        element.containingFile
          ?.virtualFile
          ?.path
          .orEmpty() + '#'
      return when (element) {
        is PsiQualifiedNamedElement -> {
          path + element.qualifiedName
        }
        is PsiNamedElement -> {
          path + element.name
        }
        else -> {
          path + element.toString()
        }
      }
    }

  /** Returns true if the run configuration was successfully configured.  */
  fun setupRunConfiguration(config: BlazeCommandRunConfiguration): Boolean

  /** Returns true if the run configuration matches this [RunConfigurationContext].  */
  fun matchesRunConfiguration(config: BlazeCommandRunConfiguration): Boolean

  companion object {
    fun fromKnownTarget(
      target: Label,
      command: BlazeCommandName,
      sourceElement: PsiElement,
    ): RunConfigurationContext {
      return object : RunConfigurationContext {
        override val sourceElement: PsiElement = sourceElement

        override fun setupRunConfiguration(config: BlazeCommandRunConfiguration): Boolean {
          config.setTarget(target)
          val handlerState: BlazeCommandRunConfigurationCommonState? =
            config.getHandlerStateIfType(
              BlazeCommandRunConfigurationCommonState::class.java,
            )
          if (handlerState == null) {
            return false
          }
          handlerState.commandState.setCommand(command)
          config.setGeneratedName()
          return true
        }

        override fun matchesRunConfiguration(config: BlazeCommandRunConfiguration): Boolean {
          val handlerState: BlazeCommandRunConfigurationCommonState? =
            config.getHandlerStateIfType(
              BlazeCommandRunConfigurationCommonState::class.java,
            )
          if (handlerState == null) {
            return false
          }
          return handlerState.commandState.getCommand() == command &&
            config.targets == listOf<Any?>(target) &&
            handlerState.testFilterFlag == null
        }
      }
    }
  }
}
