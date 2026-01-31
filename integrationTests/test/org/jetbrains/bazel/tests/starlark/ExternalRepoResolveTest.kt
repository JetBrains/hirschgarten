package org.jetbrains.bazel.tests.starlark

import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.tools.ide.performanceTesting.commands.openFile
import org.jetbrains.bazel.data.IdeaBazelCases
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.ideStarter.execute
import org.jetbrains.bazel.ideStarter.navigateToFile
import org.jetbrains.bazel.ideStarter.syncBazelProject
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

/**
 * ```sh
 * bazel test //plugin-bazel/src/test/kotlin/org/jetbrains/bazel/languages/starlark/references:ExternalRepoResolveTest --jvmopt="-Dbazel.ide.starter.test.cache.directory=$HOME/IdeaProjects/hirschgarten" --sandbox_writable_path=/ --action_env=PATH --java_debug --test_arg=--wrapper_script_flag=--debug=8000
 * ```
 */
class ExternalRepoResolveTest : IdeStarterBaseProjectTest() {

  @Test
  fun openBazelProject() {
    createContext("externalRepoResolve", IdeaBazelCases.ExternalRepoResolve)
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        ideFrame {
          syncBazelProject()
          waitForIndicators(5.minutes)

          step("Navigate from kt_test to junit_test.bzl") {
            execute { openFile("src/BUILD") }
            // load(":junit_test.bzl", "kt_<caret>test") -> def <caret>kt_test
            execute { navigateToFile(2, 29, expectedFilename = "junit_test.bzl", 3, 5) }
            takeScreenshot("afterNavigateToJunitTestBzl")
          }

          step("Navigate from java_library to java_library.bzl") {
            execute { openFile("src/BUILD") }
            // java_<caret>library( -> def <caret>java_library
            execute { navigateToFile(11, 6, expectedFilename = "java_library.bzl", 18, 5) }
            takeScreenshot("afterNavigateToJavaLibraryBzl")
          }

          step("Navigate from kt_test to junit_test.bzl again") {
            execute { openFile("src/BUILD") }
            // kt_<caret>test( -> def <caret>kt_test
            execute { navigateToFile(4, 4, expectedFilename = "junit_test.bzl", 3, 5) }
            takeScreenshot("afterNavigateToJunitTestBzlAgain")
          }

          step("Navigate from kt_jvm_test to jvm.bzl") {
            // load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm<caret>_test") -> <caret>kt_jvm_test
            execute { navigateToFile(1, 46, expectedFilename = "jvm.bzl", 17, 1) }
            takeScreenshot("afterNavigateToJvmBzl")
          }

          step("Navigate to opts.bzl") {
            // "//kotlin/internal:opts.<caret>bzl" -> <caret>kt_javac_options
            execute { navigateToFile(2, 29, expectedFilename = "opts.bzl", 1, 1) }
            takeScreenshot("afterNavigateToOptsBzl")
          }

          step("Navigate to Hello.java from nested_src") {
            execute { openFile("src/BUILD") }
            // srcs = ["nested<caret>_src/Hello.java"],
            execute { navigateToFile(13, 20, expectedFilename = "Hello.java", 1, 1) }
            takeScreenshot("afterNavigateToHelloJavaFromNestedSrc")
          }

          step("Navigate to BUILD file from junit_test.bzl reference") {
            execute { openFile("src/BUILD") }
            // When not inside a `load` statement, prefer the target with the same name as opposed to a file
            // ":junit<caret>_test.bzl",
            execute { navigateToFile(15, 16, expectedFilename = "BUILD", 21, 12) }
            takeScreenshot("afterNavigateToBuildFileFromJunitTestBzlReference")
          }

          step("Navigate to BUILD file from package_with_macros") {
            execute { openFile("src/BUILD") }
            // When a rule with the exact label doesn't exist in the file (in this test because of a macro), fall back to the BUILD file
            // "//src/package<caret>_with_macros:my_java_library",
            execute { navigateToFile(16, 23, expectedFilename = "BUILD", 1, 1) }
            takeScreenshot("afterNavigateToBuildFileFromPackageWithMacros")
          }

          step("Navigate to Hello.java from package_with_macros") {
            execute { openFile("src/BUILD") }
            // srcs = ["//src/package<caret>_with_macros:Hello.java"],
            execute { navigateToFile(22, 27, expectedFilename = "Hello.java", 1, 1) }
            takeScreenshot("afterNavigateToHelloJavaFromPackageWithMacros")
          }
        }
      }
  }
}
