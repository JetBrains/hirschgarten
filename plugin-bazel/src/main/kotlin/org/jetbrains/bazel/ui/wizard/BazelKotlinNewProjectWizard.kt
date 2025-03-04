package org.jetbrains.bazel.ui.wizard

import com.intellij.ide.projectWizard.generators.AssetsNewProjectWizardStep
import com.intellij.ide.starters.local.GeneratorAsset
import com.intellij.ide.starters.local.GeneratorFile
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import org.jetbrains.bazel.config.BazelPluginConstants
import org.jetbrains.kotlin.tools.projectWizard.BuildSystemKotlinNewProjectWizard
import org.jetbrains.kotlin.tools.projectWizard.KotlinNewProjectWizard.Step

class BazelKotlinNewProjectWizard : BuildSystemKotlinNewProjectWizard {
  override val name: @NlsContexts.Label String
    get() = BazelPluginConstants.BAZEL_DISPLAY_NAME

  override fun createStep(parent: Step): NewProjectWizardStep = AssetsStep(parent)

  private class AssetsStep(parent: Step) : AssetsNewProjectWizardStep(parent) {
    // we could dynamically fetch versions and whatnot,
    // but it's probably easier and safer to just update and test everything together once in a while
    val gitignoreAssets: List<GeneratorAsset> =
      listOf(
        GeneratorFile(".gitignore", ".bazelbsp/\n.idea/"),
        GeneratorFile(".bazelversion", BAZEL_VERSION),
        GeneratorFile("MODULE.bazel", moduleBazel(context)),
        GeneratorFile("src/main/org/example/BUILD.bazel", buildBazelMain()),
        GeneratorFile("src/main/org/example/Main.kt", mainKotlin()),
        GeneratorFile("src/test/org/example/BUILD.bazel", buildBazelTest()),
        GeneratorFile("src/test/org/example/Tests.kt", testKotlin()),
      )

    override fun setupAssets(project: Project) {
      if (context.isCreatingNewProject) {
        addAssets(gitignoreAssets)
      }
    }

    companion object {
      const val BAZEL_VERSION = "8.1.1"
      const val RULES_KOTLIN_VERSION = "1.9.1"
      const val RULES_JVM_EXTERNAL_VERSION = "6.7"
      const val JUNIT_VERSION = "4.13.2"
      const val KOTLIN_VERSION = "2.1.0"

      private fun moduleBazel(context: WizardContext): String =
        """
        module(
            name = "${context.projectName}",
            version = "0.1.0",
        )

        bazel_dep(name = "rules_kotlin", version = "$RULES_KOTLIN_VERSION")
        bazel_dep(name = "rules_jvm_external", version = "$RULES_JVM_EXTERNAL_VERSION")

        # Set up Maven repositories for dependencies
        maven = use_extension("@rules_jvm_external//:extensions.bzl", "maven")
        maven.install(
            name = "maven",
            artifacts = [
                "junit:junit:$JUNIT_VERSION",
                "org.jetbrains.kotlin:kotlin-stdlib:$KOTLIN_VERSION",
                "org.jetbrains.kotlin:kotlin-test:$KOTLIN_VERSION",
            ],
            repositories = [
                "https://repo1.maven.org/maven2",
            ],
        )
        use_repo(maven, "maven")
        """.trimIndent()
    }

    private fun buildBazelMain() =
      """
      load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_binary")

      package(default_visibility = ["//visibility:public"])

      kt_jvm_binary(
          name = "Main",
          srcs = glob(["**/*.kt"]),
          main_class = "org.example.MainKt",

      )
      """.trimIndent()

    private fun mainKotlin() =
      """
      package org.example

      fun main() {
          println("Hello World!")
      }
      """.trimIndent()

    private fun buildBazelTest() =
      """
      load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_test")

      kt_jvm_test(
          name = "tests",
          srcs = glob(["**/*.kt"]),
          test_class = "org.example.TestsKt",
          deps = [
              "@maven//:org_jetbrains_kotlin_kotlin_test",
              "@maven//:junit_junit",
          ],
      )
      """.trimIndent()

    private fun testKotlin() =
      """
      package org.example

      import kotlin.test.Test
      import kotlin.test.assertEquals

      class Tests {
          @Test
          fun testSimple() {
              assertEquals(4, 2 + 2, "Basic addition should work")
          }
      }
      """.trimIndent()
  }
}
