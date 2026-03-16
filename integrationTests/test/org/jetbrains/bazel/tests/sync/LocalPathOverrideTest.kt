package org.jetbrains.bazel.tests.sync

import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.UiText.Companion.asString
import com.intellij.driver.sdk.ui.components.common.editorTabs
import com.intellij.driver.sdk.ui.components.common.gutter
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.openapi.ui.playback.commands.AbstractCommand.CMD_PREFIX
import com.intellij.tools.ide.performanceTesting.commands.CommandChain
import com.intellij.tools.ide.performanceTesting.commands.goToDeclaration
import com.intellij.tools.ide.performanceTesting.commands.goto
import org.jetbrains.bazel.data.IdeaBazelCases
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.ideStarter.execute
import org.jetbrains.bazel.ideStarter.openFile
import org.jetbrains.bazel.ideStarter.syncBazelProject
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class LocalPathOverrideTest : IdeStarterBaseProjectTest() {

  @Test
  fun `local path override should resolve navigation and build target`() {
    createContext("localPathOverride", IdeaBazelCases.LocalPathOverride)
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        ideFrame {
          step("Initial sync") {
            syncBazelProject()
            waitForIndicators(5.minutes)
            takeScreenshot("afterSync")
          }

          step("Navigate from source file") {
            execute { openFile("Main.kt")}
            execute { goto(6, 44)}
            takeScreenshot("beforeSourceNavigation")
            execute { goToDeclaration() }
            takeScreenshot("afterSourceNavigation")
            execute { assertCurrentFileDirectory("subproject/Utils.kt")}
          }

          step("Navigate from main BUILD file") {
            execute { openFile("BUILD")}
            execute { goto(7,17)}
            takeScreenshot("beforeBuildNavigation")
            execute { goToDeclaration() }
            takeScreenshot("afterBuildNavigation")
            execute { assertCurrentFileDirectory("subproject/BUILD")}
          }

          step("BUILD file should contain build target") {
            editorTabs().gutter().getGutterIcons().first { it.getIconPath().contains("build") }.click()
            takeScreenshot("After library build started")
            val buildView = x { byType("com.intellij.build.BuildView") }
            buildView.waitContainsText("Build completed successfully", timeout = 60.seconds)
            takeScreenshot("afterBuildCompleted")
            // double check we actually built the correct single target
            assert(buildView.getAllTexts().asString().contains("utils.jdeps"))
          }
        }
      }
  }


  private fun <T : CommandChain> T.assertCurrentFileDirectory(qualifiedName: String): T {
    addCommand("${CMD_PREFIX}assertCurrentFileDirectory $qualifiedName")
    return this
  }

}
