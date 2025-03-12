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

import com.google.common.collect.ImmutableList
import com.google.idea.blaze.base.command.BlazeCommandName
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiQualifiedNamedElement
import java.util.*
import java.util.function.Function

/** A context used to configure a blaze run configuration, possibly asynchronously.  */
interface RunConfigurationContext {
    /** The [PsiElement] most relevant to this context (e.g. a method, class, file, etc.).  */
    @JvmField
    val sourceElement: PsiElement?

    val sourceElementString: String?
        /** Convert a [.getSourceElement] into an uniquely identifiable string.  */
        get() {
            if (!ApplicationManager.getApplication().isReadAccessAllowed()) {
                return ReadAction.compute<String?, RuntimeException?>(
                    ThrowableComputable { this.getSourceElementString() })
            }
            val element = this.sourceElement
            if (element is PsiFile) {
                return Optional.of<PsiFile?>(element)
                    .map<VirtualFile?>(Function { obj: PsiFile? -> obj!!.getVirtualFile() })
                    .map<String?>(Function { obj: VirtualFile? -> obj!!.getPath() })
                    .orElse(element.toString())
            }
            val path =
                (Optional.of<PsiElement?>(element!!)
                    .map<PsiFile?>(Function { obj: PsiElement? -> obj!!.getContainingFile() })
                    .map<VirtualFile?>(Function { obj: PsiFile? -> obj!!.getVirtualFile() })
                    .map<String>(Function { obj: VirtualFile? -> obj!!.getPath() })
                    .orElse("")
                        + '#')
            if (element is PsiQualifiedNamedElement) {
                return path + element.getQualifiedName()
            } else if (element is PsiNamedElement) {
                return path + element.getName()
            } else {
                return path + element.toString()
            }
        }

    /** Returns true if the run configuration was successfully configured.  */
    fun setupRunConfiguration(config: BlazeCommandRunConfiguration?): Boolean

    /** Returns true if the run configuration matches this [RunConfigurationContext].  */
    fun matchesRunConfiguration(config: BlazeCommandRunConfiguration?): Boolean

    companion object {
        fun fromKnownTarget(
            target: TargetExpression, command: BlazeCommandName, sourceElement: PsiElement
        ): RunConfigurationContext {
            return object : RunConfigurationContext {
                override fun getSourceElement(): PsiElement {
                    return sourceElement
                }

                override fun setupRunConfiguration(config: BlazeCommandRunConfiguration): Boolean {
                    config.setTarget(target)
                    val handlerState: BlazeCommandRunConfigurationCommonState? =
                        config.getHandlerStateIfType<BlazeCommandRunConfigurationCommonState?>(
                            BlazeCommandRunConfigurationCommonState::class.java
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
                        config.getHandlerStateIfType<BlazeCommandRunConfigurationCommonState?>(
                            BlazeCommandRunConfigurationCommonState::class.java
                        )
                    if (handlerState == null) {
                        return false
                    }
                    return handlerState.commandState.getCommand() == command
                            && config.targets == ImmutableList.of<Any?>(target)
                            && handlerState.testFilterFlag == null
                }
            }
        }
    }
}
