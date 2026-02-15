package org.jetbrains.bazel.tests.python

import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.ide.starter.driver.engine.BackgroundRun
import org.jetbrains.bazel.ideStarter.execute
import org.jetbrains.bazel.ideStarter.navigateToFile
import com.intellij.tools.ide.performanceTesting.commands.openFile

fun pyCharmImportStatements(bgRun: BackgroundRun) {
  bgRun.driver.withContext {
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
