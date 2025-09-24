package org.jetbrains.bazel.build.session

import com.intellij.build.BuildProgressListener
import com.intellij.build.DefaultBuildDescriptor
import com.intellij.build.events.*
import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.build.events.impl.SuccessResultImpl

/**
 * Maintains Build View state for a single Bazel invocation.
 * For now, all messages are attached to the top build node. Target grouping can be added later.
 */
class BazelBuildSession(
  private val buildListener: BuildProgressListener,
  private val descriptor: DefaultBuildDescriptor,
) {
  private val buildId: Any = descriptor.id

  @Volatile
  private var hadErrors: Boolean = false

  private val startedTargets = linkedSetOf<String>()
  @Volatile
  private var lastActiveTarget: String? = null

  fun id(): Any = buildId

  fun start() {
    val be = BuildEvents.getInstance()
    val start: BuildEvent = be.startBuild()
      .withMessage(descriptor.title)
      .withBuildDescriptor(descriptor)
      .build()
    buildListener.onEvent(buildId, start)
    // Note: PresentableBuildEvent requires presentationData; not needed here since StartBuildEvent with descriptor sets title/icon
  }

  fun finish(exitCode: Int) {
    val result: EventResult = if (exitCode == 0 && !hadErrors) SuccessResultImpl() else FailureResultImpl(null, null)
    val finish: BuildEvent = BuildEvents.getInstance().finishBuild()
      .withStartBuildId(buildId)
      .withMessage(descriptor.title)
      .withResult(result)
      .build()
    buildListener.onEvent(buildId, finish)
  }

  fun currentParentId(): Any = lastActiveTarget ?: buildId

  fun accept(event: BuildEvent) {
    if (event is MessageEvent && event.kind == MessageEvent.Kind.ERROR) hadErrors = true
    buildListener.onEvent(buildId, event)
  }

  fun acceptText(parentId: Any, text: String) {
    val output: BuildEvent = BuildEvents.getInstance().output()
      .withParentId(parentId)
      .withMessage(text)
      .build()
    buildListener.onEvent(buildId, output)
  }

  fun onTargetConfigured(label: String) {
    ensureTargetNode(label)
    lastActiveTarget = label
  }

  fun onTargetCompleted(label: String, success: Boolean) {
    if (!startedTargets.contains(label)) {
      // start implicitly if we missed configured
      ensureTargetNode(label)
    }
    val result: EventResult = if (success) SuccessResultImpl() else FailureResultImpl(null, null)
    val finish: BuildEvent = BuildEvents.getInstance().finish()
      .withStartId(label)
      .withParentId(buildId)
      .withMessage(label)
      .withResult(result)
      .build()
    buildListener.onEvent(buildId, finish)
    startedTargets.remove(label)
    if (lastActiveTarget == label) lastActiveTarget = null
  }

  private fun ensureTargetNode(label: String) {
    if (startedTargets.add(label)) {
      val start: BuildEvent = BuildEvents.getInstance().start()
        .withId(label)
        .withParentId(buildId)
        .withMessage(label)
        .build()
      buildListener.onEvent(buildId, start)
    }
  }
}
