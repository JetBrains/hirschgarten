package org.jetbrains.bazel.tests.ui

import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.components.common.toolwindows.projectView
import com.intellij.driver.sdk.ui.components.elements.JTreeUiComponent
import com.intellij.driver.sdk.ui.components.elements.popupMenu
import com.intellij.driver.sdk.ui.should
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import org.jetbrains.bazel.data.IdeaBazelCases
import org.jetbrains.bazel.ideStarter.IdeStarterBaseProjectTest
import org.jetbrains.bazel.ideStarter.syncBazelProject
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes


class BazelProjectTreeAppearanceTest : IdeStarterBaseProjectTest() {

  @Test
  fun `compact middle packages works in Bazel project tree - single target`() {
    createContext("bazelProjectTreeAppearance", IdeaBazelCases.ProjectViewAppearance)
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        ideFrame {
          syncBazelProject()
          waitForIndicators(5.minutes)

          leftToolWindowToolbar.projectButton.open()

          projectView {
            // Expand tree
            step("Expand common/src/main/java") {
              expandPath(projectViewTree, "common")
            }

            // Compact middle packages: true (default)
            step("Under common/src/main/java a compacted node 'com.example.common' exists") {
              projectViewTree.should("compacted node 'com.example.common' under common/src/main/java") {
                collectExpandedPaths().any {
                  checkPathAboveLast(it.path, "com.example.common", listOf("java", "main", "src", "common"))
                }
              }
            }
          }

          // Turn off Compact Middle Packages
          step("Disable Appearance > Compact Middle Packages option") {
            switchProjectViewOption("Appearance", "Compact Middle Packages")
          }

          projectView {
            // Compact middle packages: false
            step("Under common/src/main/java separate nodes 'com', 'example', 'common' exist (no compaction)") {
              projectViewTree.should("separate package nodes under common/src/main/java") {
                val paths = collectExpandedPaths().map { it.path }
                val underCommonSrcMainJava = paths.filter {
                  checkPathAboveLast(it, "java", listOf("main", "src", "common"))
                }.map { p ->
                  val idx = p.indexOfLast { it == "java" }
                  p.drop(idx + 1)
                }

                underCommonSrcMainJava.any { it.size == 3 && it[0] == "com" && it[1] == "example" && it[2] == "common" }
              }
            }
          }
        }
      }
  }


  @Test
  fun `compact middle packages works in Bazel project tree - multiple targets`() {
    createContext("bazelProjectTreeAppearance", IdeaBazelCases.ProjectViewAppearance)
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        ideFrame {
          syncBazelProject()
          waitForIndicators(5.minutes)

          leftToolWindowToolbar.projectButton.open()

          projectView {
            // Expand tree
            step("Expand app/src/main/java") {
              expandPath(projectViewTree, "app")
            }

            // Compact middle packages: true (default)
            step("Under app/src/main/java a compacted node 'com.example.app' exists") {
              projectViewTree.should("compacted node 'com.example.app' under app/src/main/java") {
                collectExpandedPaths().any {
                  checkPathAboveLast(it.path, "com.example.app", listOf("java", "main", "src", "app"))
                }
              }
            }
            step("Under app/src/other/java a compacted node 'com.example.other' exists") {
              projectViewTree.should("compacted node 'com.example.other' under app/src/other/java") {
                collectExpandedPaths().any {
                  checkPathAboveLast(it.path, "com.example.other", listOf("java", "other", "src", "app"))
                }
              }
            }
          }

          // Turn off Compact Middle Packages
          step("Disable Appearance > Compact Middle Packages option") {
            switchProjectViewOption("Appearance", "Compact Middle Packages")
          }

          projectView {
            // Compact middle packages: false
            step("Under app/src/main/java separate nodes 'com', 'example', 'app' exist (no compaction)") {
              projectViewTree.should("separate package nodes under app/src/main/java") {
                val paths = collectExpandedPaths().map { it.path }
                val underAppSrcMainJava = paths.filter {
                  checkPathAboveLast(it, "java", listOf("main", "src", "app"))
                }.map { p ->
                  val idx = p.indexOfLast { it == "java" }
                  p.drop(idx + 1)
                }

                underAppSrcMainJava.any { it.size == 3 && it[0] == "com" && it[1] == "example" && it[2] == "app" }
              }
            }
            step("Under app/src/other/java separate nodes 'com', 'example', 'other' exist (no compaction)") {
              projectViewTree.should("separate package nodes under app/src/other/java") {
                val paths = collectExpandedPaths().map { it.path }
                val underAppSrcOtherJava = paths.filter {
                  checkPathAboveLast(it, "java", listOf("other", "src", "app"))
                }.map { p ->
                  val idx = p.indexOfLast { it == "java" }
                  p.drop(idx + 1)
                }

                underAppSrcOtherJava.any { it.size == 3 && it[0] == "com" && it[1] == "example" && it[2] == "other" }
              }
            }
          }
        }
      }
  }

  @Test
  fun `flatten packages works in Bazel project tree`() {
    createContext("bazelProjectTreeAppearance", IdeaBazelCases.ProjectViewAppearance)
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        ideFrame {
          syncBazelProject()
          waitForIndicators(5.minutes)

          leftToolWindowToolbar.projectButton.open()

          projectView {
            // Expand tree
            step("Expand common/src/main/java") {
              expandPath(projectViewTree, "common")
            }
          }

          // Turn off Compact Middle Packages
          step("Disable Appearance > Compact Middle Packages option") {
            switchProjectViewOption("Appearance", "Compact Middle Packages")
          }

          // Turn on Flatten Packages
          step("Enable Appearance > Flatten Packages option") {
            switchProjectViewOption("Appearance", "Flatten Packages")
          }

          projectView {
            // Flatten packages: true
            step("Under common/src/main/java a compacted nodes 'com', 'com.example', 'com.example.common' exist") {
              projectViewTree.should("compacted node 'com' under common/src/main/java") {
                collectExpandedPaths().any {
                  checkPathAboveLast(it.path, "com", listOf("java", "main", "src", "common"))
                }
              }
              projectViewTree.should("compacted node 'com.example' under common/src/main/java") {
                collectExpandedPaths().any {
                  checkPathAboveLast(it.path, "com.example", listOf("java", "main", "src", "common"))
                }
              }
              projectViewTree.should("compacted node 'com.example.common' under common/src/main/java") {
                collectExpandedPaths().any {
                  checkPathAboveLast(it.path, "com.example.common", listOf("java", "main", "src", "common"))
                }
              }
            }
          }
        }
      }
  }

  @Test
  fun `hide empty middle packages works in Bazel project tree`() {
    createContext("bazelProjectTreeAppearance", IdeaBazelCases.ProjectViewAppearance)
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        ideFrame {
          syncBazelProject()
          waitForIndicators(5.minutes)

          leftToolWindowToolbar.projectButton.open()

          projectView {
            // Expand tree
            step("Expand common/src/main/java") {
              expandPath(projectViewTree, "common")
            }
          }

          // Turn off Compact Middle Packages
          step("Disable Appearance > Compact Middle Packages option") {
            switchProjectViewOption("Appearance", "Compact Middle Packages")
          }

          // Turn on Flatten Packages
          step("Enable Appearance > Flatten Packages option") {
            switchProjectViewOption("Appearance", "Flatten Packages")
          }

          // Turn on Hide Empty Middle Packages
          step("Disable Appearance > Compact Middle Packages option") {
            switchProjectViewOption("Appearance", "Hide Empty Middle Packages")
          }


          projectView {
            // Flatten packages: true
            step("Under common/src/main/java exists only one compacted node 'com.example.common'") {
              projectViewTree.should("compacted node 'com.example.common' under common/src/main/java") {
                val paths = collectExpandedPaths()
                paths.any {
                  checkPathAboveLast(it.path, "com.example.common", listOf("java", "main", "src", "common"))
                }
              }
              projectViewTree.should("NOT compacted node 'com.example' under common/src/main/java") {
                val paths = collectExpandedPaths()
                paths.none {
                  checkPathAboveLast(it.path, "com.example", listOf("java", "main", "src", "common"))
                }
              }
              projectViewTree.should("NOT compacted node 'com' under common/src/main/java") {
                val paths = collectExpandedPaths()
                paths.none {
                  checkPathAboveLast(it.path, "com", listOf("java", "main", "src", "common"))
                }
              }
            }
          }
        }
      }
  }

  private fun expandPath(projectViewTree: JTreeUiComponent, path: String) {
    val commonRow = projectViewTree.collectExpandedPaths()
                   .firstOrNull { it.path.lastOrNull() == path }
                   ?.row ?: error("Cannot find '$path' row in Project View")
    projectViewTree.doubleClickRow(commonRow)

    val srcRow = projectViewTree.collectExpandedPaths()
                   .firstOrNull { info ->
                     val p = info.path
                     p.size >= 2 && p[p.size - 2] == path && p.last() == "src"
                   }?.row ?: error("Cannot find 'src' row under '$path'")
    projectViewTree.doubleClickRow(srcRow)

    val nextRows = projectViewTree.collectExpandedPaths()
                    .filter { info ->
                       val p = info.path
                       p.size >= 3 && p[p.size - 3] == path && p[p.size - 2] == "src"
                    }
    if (nextRows.size < 2) return

    for (i in nextRows.size - 1 downTo 0) {
      val row = nextRows[i].row
      projectViewTree.doubleClickRow(row)
    }
  }

  private fun checkPathAboveLast(path: List<String>, elem: String, expectedUp: List<String>): Boolean {
    val idx = path.lastIndexOf(elem)
    if (idx <= expectedUp.size) return false
    for (i in 1..expectedUp.size) {
      if (path[idx - i] != expectedUp[i - 1]) return false
    }
    return true
  }

  private fun com.intellij.driver.sdk.ui.components.common.IdeaFrameUI.switchProjectViewOption(
    category: String,
    option: String,
  ) {
    projectView {
      moveMouse()
      toolWindowHeader.optionsButton.click()
    }
    popupMenu().run {
      select(category, option)
    }
    keyboard {
      escape()
    }
  }
}
