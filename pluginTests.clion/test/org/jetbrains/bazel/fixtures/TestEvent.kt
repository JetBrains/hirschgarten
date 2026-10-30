package org.jetbrains.bazel.fixtures

import com.intellij.build.events.MessageEvent

/** An event that an import step or a debug step reported. */
internal data class TestEvent(val severity: MessageEvent.Kind, val message: String, val description: String?)
