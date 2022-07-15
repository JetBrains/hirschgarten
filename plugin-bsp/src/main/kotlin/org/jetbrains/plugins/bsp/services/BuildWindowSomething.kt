package org.jetbrains.plugins.bsp.services

import com.intellij.build.BuildViewManager
import com.intellij.build.DefaultBuildDescriptor
import com.intellij.build.events.MessageEvent
import com.intellij.build.events.impl.MessageEventImpl
import com.intellij.build.events.impl.ProgressBuildEventImpl
import com.intellij.build.events.impl.StartBuildEventImpl
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import kotlinx.coroutines.runBlocking
import org.jetbrains.concurrency.runAsync


public class BuildWindowSomething(project: Project) {
  init {
    val buildView = project.getService(BuildViewManager::class.java)
    val buildId = "buildId"
    val title = "Title 2"
    val basePath = project.basePath!!
    val buildDescriptor = DefaultBuildDescriptor(buildId, title, basePath, System.currentTimeMillis())
    val startEvent = StartBuildEventImpl(buildDescriptor, "message")
    buildView.onEvent(buildId, startEvent)

    val task = object : Task.Backgroundable(project, "Loading changes 1 ", false) {
      override fun run(indicator: ProgressIndicator) {

        indicator.text = "Loading changes 2"
        indicator.isIndeterminate = true
//        indicator.fraction = 0.0;
        for (i in 1..10) {
          Thread.sleep(1000)
//          indicator.fraction = i / 10.0
          val progressEvent = ProgressBuildEventImpl(
            "nowe id $i", buildId, System.currentTimeMillis(), "message$i", -1,
            -1, "kłykcie"
          )

          val a = MessageEventImpl("nowe id $i", MessageEvent.Kind.SIMPLE, "daas", "loloo 1", "loll2")

//          progressEvent.description = "XDDDDD"
//          a.description = "XDDDD"
          buildView.onEvent(buildId, progressEvent)
          buildView.onEvent(buildId, a)
        }
//        indicator.fraction = 1.0;
      }
    }

    ProgressManager.getInstance().run(task)
  }
}