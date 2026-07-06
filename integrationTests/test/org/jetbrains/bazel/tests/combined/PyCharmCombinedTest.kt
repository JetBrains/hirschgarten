package org.jetbrains.bazel.tests.combined

import com.intellij.driver.client.Driver
import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.editorTabs
import com.intellij.driver.sdk.ui.components.common.gutter
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.components.elements.popup
import com.intellij.driver.sdk.ui.xQuery
import com.intellij.driver.sdk.wait
import com.intellij.ide.starter.ide.IDETestContext
import com.intellij.tools.ide.performanceTesting.commands.openFile
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSize
import org.jetbrains.bazel.data.PyCharmBazelCases
import org.jetbrains.bazel.ideStarter.execute
import org.jetbrains.bazel.ideStarter.navigateToFile
import org.jetbrains.bazel.ideStarter.syncBazelProjectCloseDialog
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

class PyCharmCombinedTest : IdeStarterCombinedBaseTest() {
  override fun createContext(): IDETestContext =
    createContext("pyCharmCombined", PyCharmBazelCases.PyCharm)

  override fun Driver.syncBazelProject() {
    syncBazelProjectCloseDialog()
  }

  @Test @Order(1)
  fun `Python run line markers should be available after sync`() = pyCharmRunLineMarkers()

  @Test @Order(2)
  fun `Python test line markers should be available for test files`() = pyCharmTestLineMarkers()

  @Test @Order(3)
  fun `Python import statements should resolve correctly`() = pyCharmImportStatements()

  private fun pyCharmRunLineMarkers() {
    withDriver(bgRun) {
      ideFrame {
        step("Open file") {
          execute { openFile("python/bin.py") }
          wait(5.seconds)
        }

        step("Verify run line marker text") {
          verifyRunLineMarkerText(listOf("Run '//python:binary'", "Debug '//python:binary'"))
        }
      }
    }
  }

  private fun pyCharmTestLineMarkers() {
    withDriver(bgRun) {
      ideFrame {
        step("Open test file") {
          execute { openFile("python/test.py") }
          wait(5.seconds)
        }

        step("Verify run line marker text") {
          verifyRunLineMarkerText(listOf("Run '//python:test'", "Debug '//python:test'", "Run '//python:test' with Coverage"))
        }
      }
    }
  }

  private fun pyCharmImportStatements() {
    withDriver(bgRun) {
      ideFrame {
        step("Open main.py and navigate to bbb") {
          execute { openFile("python/main/main.py") }
          execute { navigateToFile(2, 28, "util.py", 3, 5) }
          takeScreenshot("afterNavigatingToBbb")
        }

        step("Navigate to requests.__version__") {
          execute { openFile("python/main/main.py") }
          execute { navigateToFile(9, 26, "__version__.py", 8, 1) }
          takeScreenshot("afterNavigatingToRequestsVersion")
        }

        step("Navigate to np.version.version") {
          execute { openFile("python/main/main.py") }
          execute { navigateToFile(10, 26, "version.py", 5, 1) }
          takeScreenshot("afterNavigatingToNumpyVersion")
        }

        step("Navigate to aaa from my_lib2") {
          execute { openFile("python/libs/my_lib2/util.py") }
          execute { navigateToFile(1, 27, "util.py", 1, 5) }
          takeScreenshot("afterNavigatingToAaaFromMyLib2")
        }

        step("Navigate to urban.cities") {
          execute { openFile("python/main/main.py") }
          execute { navigateToFile(6, 34, "cities.py", 4, 5) }
          takeScreenshot("afterNavigatingToUrbanCities")
        }

        step("Navigate to artificial.plastics") {
          execute { openFile("python/main/main.py") }
          execute { navigateToFile(7, 37, "plastics.py", 4, 5) }
          takeScreenshot("afterNavigatingToArtificialPlastics")
        }
      }
    }
  }

  private fun Driver.verifyRunLineMarkerText(expectedTexts: List<String>) {
    ideFrame {
      val gutterIcons = editorTabs().gutter().getGutterIcons()
      val selectedGutterIcon = gutterIcons.first()
      selectedGutterIcon.click()
      val heavyWeightWindow = popup(xQuery { byClass("HeavyWeightWindow") })
      takeScreenshot("afterClickingOnRunLineMarker")
      val texts = heavyWeightWindow.getAllTexts()
      texts.shouldHaveSize(expectedTexts.size)
      expectedTexts.forEach { expected ->
        texts.any { actual -> actual.text == expected }.shouldBeTrue()
      }
    }
  }
}
