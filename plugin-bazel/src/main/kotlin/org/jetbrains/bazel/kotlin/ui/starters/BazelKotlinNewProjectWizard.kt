package org.jetbrains.bazel.kotlin.ui.starters

import com.intellij.ide.projectWizard.generators.AssetsNewProjectWizardStep
import com.intellij.ide.starters.local.GeneratorAsset
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import org.jetbrains.bazel.config.BazelPluginConstants
import org.jetbrains.bazel.sdkcompat.StarterWizardCompat
import org.jetbrains.bazel.sdkcompat.StarterWizardCompat.generatorFile
import org.jetbrains.bazel.ui.starters.NewProjectWizardConstants.BAZEL_VERSION
import org.jetbrains.bazel.ui.starters.NewProjectWizardConstants.JUNIT_VERSION
import org.jetbrains.bazel.ui.starters.NewProjectWizardConstants.RULES_JVM_EXTERNAL_VERSION
import org.jetbrains.bazel.ui.starters.NewProjectWizardConstants.RULES_KOTLIN_VERSION
import org.jetbrains.kotlin.tools.projectWizard.BuildSystemKotlinNewProjectWizard
import org.jetbrains.kotlin.tools.projectWizard.KotlinNewProjectWizard.Step

class BazelKotlinNewProjectWizard : BuildSystemKotlinNewProjectWizard {
  override val name: @NlsContexts.Label String
    get() = BazelPluginConstants.BAZEL_DISPLAY_NAME

  override fun isEnabled(context: WizardContext): Boolean = StarterWizardCompat.startersEnabled() && super.isEnabled(context)

  override fun createStep(parent: Step): NewProjectWizardStep = AssetsStep(parent)

  private class AssetsStep(parent: Step) : AssetsNewProjectWizardStep(parent) {
    // we could dynamically fetch versions and whatnot,
    // but it's probably easier and safer to just update and test everything together once in a while
    val generatorAssets: List<GeneratorAsset> =
      listOf(
        generatorFile(".gitignore", ".bazelbsp/\n.idea/"),
        generatorFile(".bazelversion", BAZEL_VERSION),
        generatorFile(BazelPluginConstants.MODULE_BAZEL_FILE_NAME, moduleBazel(context)),
        generatorFile("src/main/org/example/${BazelPluginConstants.defaultBuildFileName()}", buildBazelMain()),
        generatorFile("src/main/org/example/Main.kt", mainKotlin()),
        generatorFile("src/test/org/example/${BazelPluginConstants.defaultBuildFileName()}", buildBazelTest()),
        generatorFile("src/test/org/example/TestMain.kt", testKotlin()),
      )

    override fun setupAssets(project: Project) {
      if (context.isCreatingNewProject) {
        addAssets(generatorAssets)
      }
    }

    companion object {
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
      
      fun constant4() = 4
      """.trimIndent()

    private fun buildBazelTest() =
      """
      load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_test")
      
      kt_jvm_test(
          name = "tests",
          srcs = glob(["**/*.kt"]),
          test_class = "org.example.TestMain",
          deps = [
              "//src/main/org/example:Main",
              "@maven//:junit_junit",
          ],
      )
      """.trimIndent()

    private fun testKotlin() =
      """
      package org.example
      
      import org.junit.Assert.assertEquals
      import org.junit.Test
      
      class TestMain {
          @Test
          fun testConstant4() {
              assertEquals("constant function is 4", 4, constant4())
          }
      }
      """.trimIndent()
  }
}
