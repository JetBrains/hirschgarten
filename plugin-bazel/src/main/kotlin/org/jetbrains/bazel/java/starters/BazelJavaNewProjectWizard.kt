package org.jetbrains.bazel.java.starters

import com.intellij.ide.projectWizard.generators.AssetsNewProjectWizardStep
import com.intellij.ide.projectWizard.generators.BuildSystemJavaNewProjectWizard
import com.intellij.ide.projectWizard.generators.JavaNewProjectWizard.Step
import com.intellij.ide.starters.local.GeneratorAsset
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.config.BazelPluginConstants
import org.jetbrains.bazel.sdkcompat.StarterWizardCompat
import org.jetbrains.bazel.sdkcompat.StarterWizardCompat.generatorFile
import org.jetbrains.bazel.ui.starters.NewProjectWizardConstants.BAZEL_VERSION
import org.jetbrains.bazel.ui.starters.NewProjectWizardConstants.JUNIT_VERSION
import org.jetbrains.bazel.ui.starters.NewProjectWizardConstants.RULES_JAVA_VERSION
import org.jetbrains.bazel.ui.starters.NewProjectWizardConstants.RULES_JVM_EXTERNAL_VERSION

class BazelJavaNewProjectWizard : BuildSystemJavaNewProjectWizard {
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
        generatorFile(Constants.MODULE_BAZEL_FILE_NAME, moduleBazel(context)),
        generatorFile("src/main/org/example/${Constants.defaultBuildFileName()}", buildBazelMain()),
        generatorFile("src/main/org/example/Main.java", mainJava()),
        generatorFile("src/test/org/example/${Constants.defaultBuildFileName()}", buildBazelTest()),
        generatorFile("src/test/org/example/MainTest.java", testJava()),
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
        
        bazel_dep(name = "rules_java", version = "$RULES_JAVA_VERSION")
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
      load("@rules_java//java:defs.bzl", "java_binary")
      
      package(default_visibility = ["//visibility:public"])
      
      java_binary(
          name = "Main",
          main_class = "org.example.Main",
          srcs = glob(["**/*.java"]),
      )
      """.trimIndent()

    private fun mainJava() =
      """
      package org.example;
      
      public class Main {
        public static void main(String[] args) {
          System.out.println("Hello World!");
        }
      
        public static int constant4() {
          return 4;
        }
      }
      """.trimIndent()

    private fun buildBazelTest() =
      """
      # JUnit 4 test target
      java_test(
          name = "tests",
          srcs = glob(["**/*.java"]),
          test_class = "org.example.MainTest",  # Optional: specify a test suite class
          deps = [
              "//src/main/org/example:Main",
              "@maven//:junit_junit",  # JUnit 4 dependency from Maven
          ],
      )
      """.trimIndent()

    private fun testJava() =
      """
      package org.example;
      
      import org.junit.Test;
      import static org.junit.Assert.*;
      
      public class MainTest {
          @Test
          public void testConstant4() {
              assertEquals("constant function is 4", 4, Main.constant4());
          }
      }
      """.trimIndent()
  }
}
