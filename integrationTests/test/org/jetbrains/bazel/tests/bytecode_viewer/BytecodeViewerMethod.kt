package org.jetbrains.bazel.tests.bytecode_viewer

import com.intellij.driver.sdk.invokeAction
import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.waitFor
import com.intellij.ide.starter.driver.engine.BackgroundRun
import com.intellij.tools.ide.performanceTesting.commands.build
import com.intellij.tools.ide.performanceTesting.commands.goto
import com.intellij.tools.ide.performanceTesting.commands.openFile
import com.intellij.tools.ide.performanceTesting.commands.reloadFiles
import com.intellij.tools.ide.performanceTesting.commands.sleep
import org.jetbrains.bazel.ideStarter.buildAndSync
import org.jetbrains.bazel.ideStarter.execute
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

fun bytecodeViewer(bgRun: BackgroundRun) {
  bgRun.driver.withContext {
    ideFrame {
      execute { buildAndSync() }
      waitForIndicators(10.minutes)

      step("Build and view bytecode") {
        execute { reloadFiles() }
        execute { build() }
        execute { waitForIndicators(2.minutes) }
        execute { openFile("SimpleTest.java") }
        execute { sleep(5_000) }
        execute { build(listOf("SimpleJavaTest")) }
        execute { waitForIndicators(2.minutes) }
        execute { goto(5, 17) }
        execute { sleep(5_000) }
        invokeAction("BytecodeViewer")
        val buildView = x("//div[@class='BytecodeToolWindowPanel']")
        val bytecodeKeywords = setOf("ICONST_3", "ICONST_2", "INVOKESTATIC")
        waitFor(message = "Bytecode viewer to display expected keywords", timeout = 30.seconds, interval = 2.seconds) {
          val text = buildView.getAllTexts().joinToString { it.text }
          bytecodeKeywords.all { text.contains(it) }
        }
      }
    }
  }
}
