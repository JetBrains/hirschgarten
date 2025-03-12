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
package org.jetbrains.bazel.ogRun.smrunner

import com.google.common.base.Strings
import com.google.idea.blaze.base.model.primitives.GenericBlazeRules.RuleTypes
import com.intellij.execution.Location
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.io.URLUtil
import java.util.*

/**
 * Encodes everything directly into the URL, will be decoded and re-encoded for the appropriate
 * underlying [BlazeTestEventsHandler].
 */
class BlazeWebTestEventsHandler : BlazeTestEventsHandler {
    override fun handlesKind(kind: Kind?): Boolean {
        return kind === RuleTypes.WEB_TEST.getKind()
    }

    val testLocator: SMTestLocator?
        get() = BlazeWebTestLocator.INSTANCE

    /** Uses the [BlazeTestEventsHandler] that handles the language of the test location.  */
    override fun getTestFilter(project: Project?, testLocations: MutableList<Location<*>?>): String? {
        return testLocations.stream()
            .map<VirtualFile?> { obj: Location<*>? -> obj!!.getVirtualFile() }
            .filter { obj: VirtualFile? -> Objects.nonNull(obj) }
            .map<String?> { obj: VirtualFile? -> obj!!.getExtension() }
            .filter { obj: String? -> Objects.nonNull(obj) }
            .map<Any?>(LanguageClass::fromExtension)
            .filter { obj: Any? -> Objects.nonNull(obj) }
            .map<Any?>(Kind::getKindsForLanguage)
            .flatMap<Any?> { obj: Any? -> obj.stream() }
            .filter { kind: Any? -> kind.getRuleType() === RuleType.TEST }
            .map<Any?>(BlazeTestEventsHandler::getHandlerForTargetKind)
            .filter { obj: Any? -> obj.isPresent() }
            .map<Any?> { obj: Any? -> obj.get() }
            .filter { handler: Any? -> handler !is BlazeWebTestEventsHandler }
            .map<Any?> { handler: Any? -> handler.getTestFilter(project, testLocations) }
            .filter { obj: Any? -> Objects.nonNull(obj) }
            .filter { filter: Any? -> !filter.isEmpty() }
            .findFirst()
            .orElse(null)
    }

    override fun testLocationUrl(
        label: Label?,
        kind: Kind?,
        parentSuite: String?,
        name: String?,
        className: String?
    ): String? {
        return (WEB_TEST_PROTOCOL
                + URLUtil.SCHEME_SEPARATOR
                + label
                + SmRunnerUtils.TEST_NAME_PARTS_SPLITTER
                + parentSuite
                + SmRunnerUtils.TEST_NAME_PARTS_SPLITTER
                + name
                + SmRunnerUtils.TEST_NAME_PARTS_SPLITTER
                + Strings.nullToEmpty(className))
    }

    override fun suiteLocationUrl(label: Label?, kind: Kind?, name: String?): String? {
        return (WEB_TEST_PROTOCOL
                + URLUtil.SCHEME_SEPARATOR
                + label
                + SmRunnerUtils.TEST_NAME_PARTS_SPLITTER
                + name)
    }

    override fun ignoreSuite(label: Label, kind: Kind?, suite: BlazeXmlSchema.TestSuite): Boolean {
        return super.ignoreSuite(label, kind, suite)
                && suite.testCases.stream()
            .allMatch { test: BlazeXmlSchema.TestCase? -> BlazeXmlToTestEventsConverter.isIgnored(test) }
    }

    companion object {
        const val WEB_TEST_PROTOCOL: String = "blaze:web:test"
    }
}
