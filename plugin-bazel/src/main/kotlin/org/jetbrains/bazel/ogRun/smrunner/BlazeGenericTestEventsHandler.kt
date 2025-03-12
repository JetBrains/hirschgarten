/*
 * Copyright 2017 The Bazel Authors. All rights reserved.
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
package org.jetbrains.bazel.ogRun.smrunner

import com.google.idea.blaze.base.model.primitives.Kind
import com.intellij.execution.Location
import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.project.Project

/**
 * Fallback handler for otherwise unsupported targets. Normally it's undesirable to have a test UI
 * for such targets, but if they're part of a test_suite or multi-target Blaze invocation, we handle
 * them in a best-effort way.
 */
class BlazeGenericTestEventsHandler : BlazeTestEventsHandler {
    override fun handlesKind(kind: Kind?): Boolean {
        // Generic handler specifically exists to handle test-suites and multi-target blaze
        // invocations, so must handle any targets without a (known) kind.
        return kind == null || kind.getKindString().equals("test_suite")
    }

    override fun getTestLocator(): SMTestLocator? {
        return null
    }

    override fun getTestFilter(project: Project?, testLocations: MutableList<Location<*>?>?): String? {
        // Test filters are language-specific, and don't work properly for multi-target invocations.
        return null
    }

    override fun createRerunFailedTestsAction(consoleView: ConsoleView?): AbstractRerunFailedTestsAction? {
        return null
    }
}
