package org.jetbrains.bazel.tests.ui

import com.intellij.driver.sdk.setRegistry
import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.components.common.toolwindows.projectView
import com.intellij.driver.sdk.ui.components.elements.JTreeUiComponent
import com.intellij.driver.sdk.ui.components.elements.popupMenu
import com.intellij.driver.sdk.ui.should
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.data.IdeaBazelCases
import org.jetbrains.bazel.data.BazelProjectConfigurer
import org.jetbrains.bazel.data.simpleBazelProject
import org.jetbrains.bazel.data.preCacheBazelisk
import org.jetbrains.bazel.base.IdeStarterBaseProjectTest
import org.jetbrains.bazel.base.syncBazelProject
import org.jetbrains.bazel.base.waitForSyncSucceeded
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

private val BAZEL_PROJECT_TREE_APPEARANCE_PROJECT = simpleBazelProject(
  path = "projectViewAppearanceTest",
  configureProject = { context ->
    BazelProjectConfigurer.configureProjectBeforeUseWithoutBazelClean(
      context,
      createProjectView = false,
    )
    preCacheBazelisk(context)
  },
)

private const val COMMON_COLLAPSED = "src/main/java/com/example/common"
private const val COMMON_COLLAPSED_ROOT = "src/main/java"
private const val COMMON_COLLAPSED_PACKAGE = "com/example/common"
private val COMMON_NESTED = arrayOf("src", "main", "java", "com", "example", "common")
private val COMMON_NESTED_ROOT = arrayOf("src", "main", "java")
private val COMMON_FLAT_PACKAGES = listOf("com", "com.example", "com.example.common")

class BazelProjectTreeAppearanceTest : IdeStarterBaseProjectTest() {

  @Test
  fun `compact middle packages works in Bazel project tree - single target`() {
    createContext("bazelProjectTreeAppearance", IdeaBazelCases.withProject(BAZEL_PROJECT_TREE_APPEARANCE_PROJECT))
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        ideFrame {
          syncBazelProject()
          waitForIndicators(5.minutes)
          waitForSyncSucceeded()

          leftToolWindowToolbar.projectButton.open()

          projectView {
            // Compact middle packages: true (default)
            // the directories above the source root collapse into it, and the packages in the source root collapse too
            step("Under common the collapsed nodes 'src/main/java' and 'com/example/common' exist") {
              projectViewTree.expandNodeChain("common", COMMON_COLLAPSED_ROOT, COMMON_COLLAPSED_PACKAGE)
            }
          }

          // Turn off Compact Middle Packages
          step("Disable Appearance > Compact Middle Packages option") {
            switchProjectViewOption("Appearance", "Compact Middle Packages")
          }

          projectView {
            // Compact middle packages: false
            step("Under common separate nodes 'src', 'main', 'java', 'com', 'example', 'common' exist (no collapsing)") {
              projectViewTree.expandNodeChain("common", *COMMON_NESTED)
              projectViewTree.should("no collapsed node under common") {
                collectExpandedPaths().none { COMMON_COLLAPSED_ROOT in it.path || COMMON_COLLAPSED_PACKAGE in it.path }
              }
            }
          }
        }
      }
  }


  @Test
  fun `compact middle packages works in Bazel project tree - multiple targets`() {
    createContext("bazelProjectTreeAppearance", IdeaBazelCases.withProject(BAZEL_PROJECT_TREE_APPEARANCE_PROJECT))
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        ideFrame {
          syncBazelProject()
          waitForIndicators(5.minutes)
          waitForSyncSucceeded()

          leftToolWindowToolbar.projectButton.open()

          projectView {
            // Compact middle packages: true (default)
            // 'src' has two children, so it does not collapse, but each of its children collapses into a source root
            step("Under app/src the collapsed nodes 'main/java' and 'com/example/app' exist") {
              projectViewTree.expandNodeChain("app", "src", "main/java", "com/example/app")
            }
            step("Under app/src the collapsed nodes 'other/java' and 'com/example/other' exist") {
              projectViewTree.expandNodeChain("app", "src", "other/java", "com/example/other")
            }
          }

          // Turn off Compact Middle Packages
          step("Disable Appearance > Compact Middle Packages option") {
            switchProjectViewOption("Appearance", "Compact Middle Packages")
          }

          projectView {
            // Compact middle packages: false
            step("Under app/src separate nodes 'main', 'java', 'com', 'example', 'app' exist (no collapsing)") {
              projectViewTree.expandNodeChain("app", "src", "main", "java", "com", "example", "app")
            }
            step("Under app/src separate nodes 'other', 'java', 'com', 'example', 'other' exist (no collapsing)") {
              projectViewTree.expandNodeChain("app", "src", "other", "java", "com", "example", "other")
            }
          }
        }
      }
  }

  @Test
  fun `flatten packages flattens only the packages in source roots in Bazel project tree`() {
    createContext("bazelProjectTreeAppearance", IdeaBazelCases.withProject(BAZEL_PROJECT_TREE_APPEARANCE_PROJECT))
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        ideFrame {
          syncBazelProject()
          waitForIndicators(5.minutes)
          waitForSyncSucceeded()

          leftToolWindowToolbar.projectButton.open()

          // Turn off Compact Middle Packages
          step("Disable Appearance > Compact Middle Packages option") {
            switchProjectViewOption("Appearance", "Compact Middle Packages")
          }

          // Turn on Flatten Packages
          step("Enable Appearance > Flatten Packages option") {
            switchProjectViewOption("Appearance", "Flatten Packages")
          }

          projectView {
            // Flatten packages: true. The directories above the source root stay nested, and the packages in it are flat.
            step("Under common/src/main/java the package nodes 'com', 'com.example' and 'com.example.common' exist") {
              projectViewTree.expandNodeChain("common", *COMMON_NESTED_ROOT, "com.example.common")
              projectViewTree.should("all packages under common/src/main/java are flat") {
                val packages = collectExpandedPaths()
                  .filter { info -> info.path.size >= 2 && info.path[info.path.size - 2] == "java" }
                  .map { info -> info.path.last() }
                packages.containsAll(COMMON_FLAT_PACKAGES)
              }
            }
          }
        }
      }
  }

  @Test
  fun `hide empty middle packages hides only the empty packages in source roots in Bazel project tree`() {
    createContext("bazelProjectTreeAppearance", IdeaBazelCases.withProject(BAZEL_PROJECT_TREE_APPEARANCE_PROJECT))
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        ideFrame {
          syncBazelProject()
          waitForIndicators(5.minutes)
          waitForSyncSucceeded()

          leftToolWindowToolbar.projectButton.open()

          // Turn off Compact Middle Packages
          step("Disable Appearance > Compact Middle Packages option") {
            switchProjectViewOption("Appearance", "Compact Middle Packages")
          }

          // Turn on Flatten Packages
          step("Enable Appearance > Flatten Packages option") {
            switchProjectViewOption("Appearance", "Flatten Packages")
          }

          // Turn on Hide Empty Middle Packages
          step("Enable Appearance > Hide Empty Middle Packages option") {
            switchProjectViewOption("Appearance", "Hide Empty Middle Packages")
          }

          projectView {
            // Flatten packages and hide empty middle packages: true. Directories outside source roots collapse only in compact mode.
            step("Under common separate nodes 'src', 'main', 'java' and the package node 'com.example.common' exist") {
              projectViewTree.expandNodeChain("common", *COMMON_NESTED_ROOT, "com.example.common")
              projectViewTree.should("no collapsed directory and no empty package under common") {
                collectExpandedPaths().none { info ->
                  info.path.any { it == COMMON_COLLAPSED_ROOT || it == "com" || it == "com.example" }
                }
              }
            }
          }
        }
      }
  }

  @Test
  fun `module names in brackets are not shown on content roots`() {
    createContext("bazelProjectTreeAppearance", IdeaBazelCases.withProject(BAZEL_PROJECT_TREE_APPEARANCE_PROJECT))
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        setRegistry(BazelFeatureFlags.MERGE_SOURCE_ROOTS, false.toString())

        ideFrame {
          syncBazelProject()
          waitForIndicators(5.minutes)
          waitForSyncSucceeded()

          leftToolWindowToolbar.projectButton.open()

          projectView {
            // the source root is the file, so the collapsed node ends at its directory, which is the content root
            step("Expand common/src/main/java/com/example/common") {
              projectViewTree.expandNodeChain("common", COMMON_COLLAPSED)
            }

            step("Directories under common have no module info") {
              val violating = projectViewTree.collectExpandedPaths()
                .filter { it.path.contains("common") }
                .filter { info ->
                  info.path.last().contains(" ")
                }
              check(violating.isEmpty()) {
                "Module info found on directory nodes: ${violating.map { it.path }}"
              }
            }
          }
        }
      }
  }

  /** Expands the nodes along [segments], each node is a child of the previous one. */
  private fun JTreeUiComponent.expandNodeChain(vararg segments: String) {
    for (count in 1..segments.size) {
      expandNodeEndingWith(*segments.copyOfRange(0, count))
    }
  }

  private fun JTreeUiComponent.expandNodeEndingWith(vararg segments: String) {
    var lastPaths = emptyList<List<String>>()
    should(
      message = "'${segments.joinToString("/")}' is expanded in Project View",
      timeout = 4.minutes,
      errorMessage = { "Current expanded paths: $lastPaths" },
    ) {
      val visiblePaths = collectExpandedPaths()
      lastPaths = visiblePaths.map { it.path }
      val target = visiblePaths.firstOrNull { it.path.endsWith(*segments) } ?: return@should false
      val childVisible = lastPaths.any { it.size == target.path.size + 1 && it.subList(0, target.path.size) == target.path }
      if (childVisible) return@should fixture.areTreeNodesLoaded()
      fixture.expandRow(target.row)
      false
    }
  }

  private fun List<String>.endsWith(vararg segments: String): Boolean =
    size >= segments.size && subList(size - segments.size, size) == segments.asList()

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
