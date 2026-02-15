package org.jetbrains.bazel.tests.hotswap

import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.codeEditor
import com.intellij.driver.sdk.ui.components.common.editorTabs
import com.intellij.driver.sdk.ui.components.common.gutter
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.components.elements.popup
import com.intellij.driver.sdk.wait
import com.intellij.ide.starter.driver.engine.BackgroundRun
import com.intellij.tools.ide.performanceTesting.commands.DebugStepTypes
import com.intellij.tools.ide.performanceTesting.commands.Keys
import com.intellij.tools.ide.performanceTesting.commands.build
import com.intellij.tools.ide.performanceTesting.commands.debugStep
import com.intellij.tools.ide.performanceTesting.commands.delayType
import com.intellij.tools.ide.performanceTesting.commands.goto
import com.intellij.tools.ide.performanceTesting.commands.openFile
import com.intellij.tools.ide.performanceTesting.commands.pressKey
import com.intellij.tools.ide.performanceTesting.commands.reloadFiles
import com.intellij.tools.ide.performanceTesting.commands.setBreakpoint
import com.intellij.tools.ide.performanceTesting.commands.sleep
import com.intellij.tools.ide.performanceTesting.commands.takeScreenshot
import org.jetbrains.bazel.ideStarter.execute
import kotlin.time.Duration.Companion.seconds

fun hotswap(bgRun: BackgroundRun) {
  bgRun.driver.withContext {
    ideFrame {
      step("Set breakpoints and start debug") {
        execute { openFile("SimpleKotlinTest.kt") }
        execute { setBreakpoint(line = 7, "SimpleKotlinTest.kt") }
        execute { setBreakpoint(line = 11, "SimpleKotlinTest.kt") }

        wait(10.seconds)
        editorTabs()
          .gutter()
          .getGutterIcons()
          .first { it.getIconPath().contains("run") }
          .click()
        popup().waitOneContainsText("Debug test").click()

        wait(30.seconds)

        takeScreenshot("afterSetBreakpointsAndStartDebugSession")
      }

      step("Modify code during debug session") {
        execute { openFile("SimpleKotlinTest.kt") }
        codeEditor().click()
        execute { goto(10, 35) }
        execute { pressKey(Keys.ENTER) }
        execute { delayType(delayMs = 150, text = "val a = 10") }
        execute { pressKey(Keys.ENTER) }
        execute { delayType(delayMs = 150, text = "val b = 20") }
        execute { pressKey(Keys.ENTER) }
        execute { delayType(delayMs = 150, text = "print(a + b)") }
        takeScreenshot("afterModifyCodeDuringDebugSession")
      }

      step("Apply hotswap and continue debugging") {
        execute { reloadFiles() }
        execute { build(listOf("SimpleKotlinTest")) }
        execute { sleep(5000) }
        execute { takeScreenshot("finishBuildAction") }
        execute { debugStep(DebugStepTypes.OVER) }
        execute { debugStep(DebugStepTypes.OVER) }
        execute { sleep(2000) }
        execute { takeScreenshot("afterHotSwapDebugStep") }
      }
    }
  }
}
