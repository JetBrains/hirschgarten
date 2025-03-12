/*
 * Copyright 2019 The Bazel Authors. All rights reserved.
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

import com.google.common.annotations.VisibleForTesting
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import com.google.idea.blaze.base.dependencies.TargetInfo
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.application.TransactionGuard
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.psi.PsiElement
import com.intellij.util.Consumer
import java.util.*
import java.util.function.Function

/**
 * When we have two or more candidate web_tests for a given context.
 *
 *
 * Creates a popup chooser when executed to select the desired web_test to run.
 */
class PendingWebTestContext private constructor(
    wrapperTests: ImmutableList<TargetInfo?>,
    supportedExecutors: ImmutableSet<ExecutorType?>?,
    sourceElement: PsiElement?,
    blazeFlags: ImmutableList<BlazeFlagsModification?>?,
    description: String?
) : TestContext(sourceElement, blazeFlags, description), PendingRunConfigurationContext {
    private val wrapperTests: ImmutableList<TargetInfo?>
    private val supportedExecutors: ImmutableSet<ExecutorType?>?

    init {
        this.wrapperTests = wrapperTests
        this.supportedExecutors = supportedExecutors
    }

    val isDone: Boolean
        get() = false

    override fun supportedExecutors(): ImmutableSet<ExecutorType?>? {
        return supportedExecutors
    }

    override fun setupTarget(config: BlazeCommandRunConfiguration): Boolean {
        config.setPendingContext(this)
        return true
    }

    override fun matchesTarget(config: BlazeCommandRunConfiguration): Boolean {
        return getSourceElementString() == config.contextElementString
    }

    override fun resolve(
        env: ExecutionEnvironment, config: BlazeCommandRunConfiguration, rerun: Runnable
    ) {
        val dataContext = env.getDataContext()
        if (dataContext == null) {
            return
        }
        val popup =
            JBPopupFactory.getInstance()
                .createPopupChooserBuilder<TargetInfo?>(wrapperTests)
                .setTitle("Choose Web Test to Run")
                .setMovable(false)
                .setResizable(false)
                .setRequestFocus(true)
                .setCancelOnWindowDeactivation(false)
                .setItemChosenCallback(
                    Consumer { wrapperTest: TargetInfo? -> updateContextAndRerun(config, wrapperTest, rerun) })
                .createPopup()
        TransactionGuard.getInstance()
            .submitTransactionAndWait(Runnable { popup.showInBestPositionFor(dataContext) })
    }

    @VisibleForTesting
    fun updateContextAndRerun(
        config: BlazeCommandRunConfiguration, wrapperTest: TargetInfo?, rerun: Runnable
    ) {
        // Changing the description here prevents rerun,
        // due to RunnerAndConfigurationSettings being tied to description.
        val context: RunConfigurationContext =
            KnownTargetTestContext(wrapperTest, sourceElement, blazeFlags, description)
        if (context.setupRunConfiguration(config)) {
            config.clearPendingContext()
            rerun.run()
        }
    }

    companion object {
        private val findWebTestContext: BoolExperiment = BoolExperiment("find.web.test.context", true)

        /**
         * Attempt to find web_test(s) wrapping a particular target.
         *
         *
         *  * If we find no web_tests, return null. The caller will proceed with the original target.
         *  * If we find exactly one web_test, then we replace the original target with the web_test in
         * a [KnownTargetTestContext].
         *  * if we find two or more web_tests, then we return a [PendingWebTestContext], which
         * will surface a popup chooser for selecting the desired web_test when executed.
         *
         */
        fun findWebTestContext(
            project: Project?,
            supportedExecutors: ImmutableSet<ExecutorType?>?,
            target: TargetInfo,
            sourceElement: PsiElement?,
            blazeFlags: ImmutableList<BlazeFlagsModification?>?,
            description: String?
        ): RunConfigurationContext? {
            if (!findWebTestContext.getValue()) {
                return null
            }
            // TODO(b/274800785): Add query sync support
            if (Blaze.getProjectType(project) !== ProjectType.ASPECT_SYNC) {
                return null
            }
            val wrapperTests: ImmutableList<TargetInfo?> = getWebTestWrappers(project, target)
            if (wrapperTests.isEmpty()) {
                return null
            } else if (wrapperTests.size == 1) {
                return KnownTargetTestContext(
                    wrapperTests.get(0), sourceElement, blazeFlags, description
                )
            }
            return PendingWebTestContext(
                wrapperTests, supportedExecutors, sourceElement, blazeFlags, description
            )
        }

        private fun getWebTestWrappers(
            project: Project?, wrappedTest: TargetInfo
        ): ImmutableList<TargetInfo?> {
            val projectData: BlazeProjectData? =
                BlazeProjectDataManager.getInstance(project).getBlazeProjectData()
            if (projectData == null) {
                return ImmutableList.of<TargetInfo?>()
            }
            val targetMap: TargetMap = projectData.getTargetMap()
            return ReverseDependencyMap.get(project)
                .get(TargetKey.forPlainTarget(wrappedTest.label))
                .stream()
                .map(targetMap::get)
                .filter({ obj: Any? -> Objects.nonNull(obj) })
                .filter({ t -> t.getKind().isWebTest() })
                .map(TargetIdeInfo::toTargetInfo)
                .sorted(Comparator.comparing<T?, U?>(Function { t: T? -> t.label }))
                .collect(ImmutableList.toImmutableList<E?>())
        }
    }
}
