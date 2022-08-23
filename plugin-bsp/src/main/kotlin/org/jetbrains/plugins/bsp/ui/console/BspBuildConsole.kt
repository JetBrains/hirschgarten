package org.jetbrains.plugins.bsp.ui.console

import com.intellij.build.BuildProgressListener
import com.intellij.build.DefaultBuildDescriptor
import com.intellij.build.events.EventResult
import com.intellij.build.events.impl.FinishBuildEventImpl
import com.intellij.build.events.impl.OutputBuildEventImpl
import com.intellij.build.events.impl.ProgressBuildEventImpl
import com.intellij.build.events.impl.StartBuildEventImpl
import com.intellij.build.events.impl.SuccessResultImpl

public class BspBuildConsole(private val buildView: BuildProgressListener, private val basePath: String) {

  private var buildsInProgress: MutableList<String> = mutableListOf()

  @Synchronized
  public fun startBuild(buildId: String, title: String, message: String): Unit = doUnlessBuildInProcess(buildId) {
    buildsInProgress.add(buildId)
    doStartBuild(buildId, title, message)
  }

  private fun doStartBuild(buildId: Any, title: String, message: String) {
    val buildDescriptor = DefaultBuildDescriptor(buildId, title, basePath, System.currentTimeMillis())
    // TODO one day
    //  .withRestartActions(restartAction)

    val startEvent = StartBuildEventImpl(buildDescriptor, message)
    buildView.onEvent(buildId, startEvent)
  }

  @Synchronized
  public fun finishBuild(message: String, buildId: String, result: EventResult = SuccessResultImpl()): Unit = doIfBuildInProcess(buildId) {
    buildsInProgress.remove(buildId)
    doFinishBuild(message, buildId, result)
  }

  private fun doFinishBuild(message: String, buildId: String, result: EventResult) {
    val event = FinishBuildEventImpl(buildId, null, System.currentTimeMillis(), message, result)
    buildView.onEvent(buildId, event)
  }

  @Synchronized
  public fun startSubtask(id: Any, message: String, buildId: String): Unit = doIfBuildInProcess(buildId) {
    val event = ProgressBuildEventImpl(id, buildId, System.currentTimeMillis(), message, -1, -1, "unit")
    buildView.onEvent(buildId, event)
  }

  @Synchronized
  public fun finishSubtask(id: Any, message: String, buildId: String): Unit = doIfBuildInProcess(buildId) {
    val event = FinishBuildEventImpl(id, null, System.currentTimeMillis(), message, SuccessResultImpl())
    buildView.onEvent(buildId, event)
  }

  @Synchronized
  public fun addMessage(id: Any?, message: String, buildId: String): Unit = doIfBuildInProcess(buildId) {
    if (message.isNotBlank()) {
      val messageToSend = prepareTextToPrint(message)

      doAddMessage(id, messageToSend, buildId)
    }
  }

  private fun doAddMessage(id: Any?, message: String, buildId: String) {
    val event = OutputBuildEventImpl(id, message, true)

    buildView.onEvent(buildId, event)
  }

  @Synchronized
  public fun addWarning() {
    // TODO
  }

  private inline fun doUnlessBuildInProcess(buildId: String, action: () -> Unit) {
    if (!buildsInProgress.contains(buildId)) {
      action()
    }
  }

  private inline fun doIfBuildInProcess(buildId: String, action: () -> Unit) {
    if (buildsInProgress.contains(buildId)) {
      action()
    }
  }

  private fun prepareTextToPrint(text: String): String =
    if (text.endsWith("\n")) text else text + "\n"
}
