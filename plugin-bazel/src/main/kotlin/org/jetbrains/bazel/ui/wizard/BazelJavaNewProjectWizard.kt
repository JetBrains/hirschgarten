package org.jetbrains.bazel.ui.wizard

import com.intellij.ide.projectWizard.generators.AssetsNewProjectWizardStep
import com.intellij.ide.projectWizard.generators.BuildSystemJavaNewProjectWizard
import com.intellij.ide.projectWizard.generators.JavaNewProjectWizard.Step
import com.intellij.ide.starters.local.GeneratorAsset
import com.intellij.ide.starters.local.GeneratorFile
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import org.jetbrains.bazel.config.BazelPluginConstants

class BazelJavaNewProjectWizard : BuildSystemJavaNewProjectWizard {

  override val name: @NlsContexts.Label String
    get() = BazelPluginConstants.BAZEL_DISPLAY_NAME

  override fun createStep(parent: Step): NewProjectWizardStep =
    AssetsStep(parent)


  private class AssetsStep(parent: Step) : AssetsNewProjectWizardStep(parent) {

    // we could dynamically fetch versions and whatnot,
    // but it's probably easier and safer to just update and test everything together once in a while
    val gitignoreAssets: List<GeneratorAsset> = listOf(
      GeneratorFile(".gitignore", ".bazelbsp/\n.idea/"),
      GeneratorFile(".bazelversion", BAZEL_VERSION),
      GeneratorFile("MODULE.bazel", moduleBazel(context)),
      GeneratorFile("src/main/org/example/BUILD.bazel", buildBazelMain()),
      GeneratorFile("src/main/org/example/Main.java", mainJava()),
      GeneratorFile("src/test/org/example/BUILD.bazel", buildBazelTest()),
      GeneratorFile("src/test/org/example/Tests.java", testJava())
    )

    override fun setupAssets(project: Project) {
      if (context.isCreatingNewProject) {
        addAssets(gitignoreAssets)
      }
    }

    companion object {

      const val BAZEL_VERSION = "8.1.1"
      const val RULES_JAVA_VERSION = "8.10.0"
      const val RULES_JVM_EXTERNAL_VERSION = "6.7"
      const val JUNIT_VERSION = "4.13.2"

      private fun moduleBazel(context: WizardContext): String = """
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

    private fun buildBazelMain() = """
      load("@rules_java//java:defs.bzl", "java_binary")
      
      package(default_visibility = ["//visibility:public"])
      
      java_binary(
          name = "Main",
          srcs = glob(["**/*.java"]),
      )
    """.trimIndent()

    private fun mainJava() = """
      package org.example;
      
      public class Main {
        public static void main(String[] args) {
          System.out.println("Hello World!");
        }
      }
      """.trimIndent()

    private fun buildBazelTest() = """
      # JUnit 4 test target
      java_test(
          name = "tests",
          srcs = glob(["**/*.java"]),
          test_class = "org.example.Tests",  # Optional: specify a test suite class
          deps = [
              ":lib",
              "@maven//:junit_junit",  # JUnit 4 dependency from Maven
          ],
      )
    """.trimIndent()

    private fun testJava() = """
      package com.example;

      import org.junit.Tests;
      import static org.junit.Assert.*;

      public class Test {
          @Test
          public void testSimple() {
              assertEquals("Basic addition should work", 4, 2 + 2);
          }
      }
    """.trimIndent()
  }
}
