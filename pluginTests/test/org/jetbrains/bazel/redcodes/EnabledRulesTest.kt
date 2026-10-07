package org.jetbrains.bazel.redcodes

import com.intellij.openapi.application.EDT
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.testFramework.common.timeoutRunBlocking
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelProjectFixture
import org.jetbrains.kotlin.idea.workspaceModel.kotlinSettings
import org.junit.jupiter.api.Test
import kotlin.io.path.Path
import kotlin.io.path.readText
import kotlin.time.Duration.Companion.minutes

// https://youtrack.jetbrains.com/issue/BAZEL-3646
@BazelTestApplication
class EnabledRulesTest {

  // The project declares `rules_java` with `repo_name = "my_java"`, and `enabled_rules` lists `my_java`.
  // `my_java` is not a name of the Java ruleset, so only the repository URL can identify the ruleset.
  private val project by bazelProjectFixture("redcodes/enabled_rules_java_only", projectView = ".bazelproject")

  @Test
  fun testKotlinRulesetIsNotUsedWhenOnlyJavaRulesetIsEnabled(): Unit = timeoutRunBlocking(timeout = 5.minutes) {
    val modules = withContext(Dispatchers.EDT) {
      WorkspaceModel.getInstance(project).currentSnapshot
        .entities(ModuleEntity::class.java)
        .associateBy { it.name }
    }

    modules.keys shouldContainAll listOf("java_lib", "kotlin_lib")
    modules.getValue("kotlin_lib").kotlinSettings.shouldBeEmpty()

    val aspectDir = Path(project.rootDir.path).resolve(".bazelbsp")
    val aspectConfig = aspectDir.resolve("config/aspect.bzl").readText()
    aspectConfig shouldContain "java_info.bzl"
    aspectConfig shouldNotContain "kotlin_info.bzl"
    // The Java aspect loads the symbols from the matched repository, not the built-in ones.
    aspectDir.resolve("modules/java_info.bzl").readText() shouldContain "load(\"@@rules_java+//java:defs.bzl\""
  }
}
