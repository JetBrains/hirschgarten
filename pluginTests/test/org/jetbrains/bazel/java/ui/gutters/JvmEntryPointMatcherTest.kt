package org.jetbrains.bazel.java.ui.gutters

import com.intellij.openapi.application.runReadActionBlocking
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.testFramework.DumbModeTestUtils
import com.intellij.util.ThreeState
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.JavaLanguageClass
import org.jetbrains.bazel.sync.workspace.languages.jvm.JAVA_BINARY_KIND
import org.jetbrains.bazel.sync.workspace.languages.jvm.JAVA_TEST_KIND
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget
import org.jetbrains.bazel.sync.workspace.languages.jvm.KT_JVM_TEST_KIND
import org.jetbrains.bazel.sync.workspace.snapshot.OutputLocationCollectionBuilder
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.jetbrains.bazel.test.framework.target.TestBuildTarget
import org.jetbrains.bazel.test.framework.testBazel
import org.jetbrains.bazel.test.framework.testproject.buildTarget
import org.jetbrains.bazel.test.framework.testproject.javaLibrary
import org.jetbrains.bazel.test.framework.testproject.sync
import org.jetbrains.bazel.ui.gutters.BazelEntryPointMatcher
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.OutputLocation
import org.jetbrains.bsp.protocol.id
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.junit.jupiter.api.Test

@BazelTestApplication
class JvmEntryPointMatcherTest {
  private val myFixture by bazelCodeInsightFixture()

  @Test
  fun `should match the target whose test class holds the element`() {
    val file = myFixture.addFileToProject(
      "AddTest.java",
      """
      package com.foo;

      public class AddTest {
        public void add() {}
      }
      """.trimIndent(),
    )
    val addTest = javaTest("//java/com/foo:AddTest", testClass = "com.foo.AddTest")
    val subtractTest = javaTest("//java/com/foo:SubtractTest", testClass = "com.foo.SubtractTest")

    val matches = matchAll(method(file, "AddTest", "add"), addTest, subtractTest)

    matches shouldBe mapOf(addTest.id to ThreeState.YES, subtractTest.id to ThreeState.NO)
  }

  @Test
  fun `should match only the class that holds the element when the file has two classes`() {
    val file = myFixture.addFileToProject(
      "Tests.java",
      """
      package com.foo;

      class FirstTest {}

      class SecondTest {}
      """.trimIndent(),
    )
    val first = javaTest("//java/com/foo:First", testClass = "com.foo.FirstTest")
    val second = javaTest("//java/com/foo:Second", testClass = "com.foo.SecondTest")

    matchAll(psiClass(file, "SecondTest"), first, second) shouldBe mapOf(first.id to ThreeState.NO, second.id to ThreeState.YES)
    matchAll(file, first, second) shouldBe mapOf(first.id to ThreeState.YES, second.id to ThreeState.YES)
  }

  @Test
  fun `should match a nested test class by its binary name`() {
    val file = myFixture.addFileToProject(
      "OuterTest.java",
      """
      package com.foo;

      public class OuterTest {
        public void outer() {}

        public static class InnerTest {}
      }
      """.trimIndent(),
    )
    val outer = javaTest("//java/com/foo:Outer", testClass = "com.foo.OuterTest")
    val inner = javaTest("//java/com/foo:Inner", testClass = "com.foo.OuterTest\$InnerTest")
    val innerClass = runReadActionBlocking { psiClass(file, "OuterTest").innerClasses.single() }

    // the outer class holds the nested class, so both targets run an element in the nested class
    matchAll(innerClass, outer, inner) shouldBe mapOf(outer.id to ThreeState.YES, inner.id to ThreeState.YES)
    matchAll(method(file, "OuterTest", "outer"), outer, inner) shouldBe mapOf(outer.id to ThreeState.YES, inner.id to ThreeState.NO)
  }

  @Test
  fun `should not match a base class of the test classes`() {
    val file = myFixture.addFileToProject(
      "BaseTest.java",
      """
      package com.foo;

      public abstract class BaseTest {}
      """.trimIndent(),
    )
    val addTest = javaTest("//java/com/foo:AddTest", testClass = "com.foo.AddTest")
    val subtractTest = javaTest("//java/com/foo:SubtractTest", testClass = "com.foo.SubtractTest")

    matchAll(psiClass(file, "BaseTest"), addTest, subtractTest) shouldBe
      mapOf(addTest.id to ThreeState.NO, subtractTest.id to ThreeState.NO)
  }

  @Test
  fun `should derive the test class of a java_test from the label`() {
    val file = myFixture.addFileToProject(
      "SubtractTest.java",
      """
      package com.foo;

      public class SubtractTest {}
      """.trimIndent(),
    )
    val subtractTest = javaTest("//java/com/foo:SubtractTest", testClass = null)

    matchAll(psiClass(file, "SubtractTest"), subtractTest) shouldBe mapOf(subtractTest.id to ThreeState.YES)
  }

  @Test
  fun `should not derive the test class of a test with a custom runner`() {
    val file = myFixture.addFileToProject(
      "SubtractTest.java",
      """
      package com.foo;

      public class SubtractTest {}
      """.trimIndent(),
    )
    val runnerTest = target("//java/com/foo:SubtractTest", JAVA_TEST_KIND, JvmBuildTarget(mainClass = "com.foo.Runner"))
    val explicitTest = target(
      "//java/com/foo:Explicit",
      JAVA_TEST_KIND,
      JvmBuildTarget(mainClass = "com.foo.Runner", testClass = "com.foo.SubtractTest"),
    )

    matchAll(psiClass(file, "SubtractTest"), runnerTest, explicitTest) shouldBe
      mapOf(runnerTest.id to ThreeState.UNSURE, explicitTest.id to ThreeState.YES)
  }

  @Test
  fun `should derive the test class of a kt_jvm_test with a main class`() {
    val file = myFixture.addFileToProject(
      "DivideTest.kt",
      """
      package com.foo

      class DivideTest
      """.trimIndent(),
    )
    val divideTest = TestBuildTarget(
      key = WorkspaceTargetKey(label = Label.parse("//java/com/foo:DivideTest")),
      kind = KT_JVM_TEST_KIND,
      sources = OutputLocationCollectionBuilder.ofLocations(listOf(OutputLocation.Workspace("java/com/foo/DivideTest.kt"))),
      // rules_kotlin gives the test class to each runner, also to a custom one
      data = listOf(JvmBuildTarget(mainClass = "com.foo.Runner")),
    )

    val divideClass = runReadActionBlocking { (file as KtFile).declarations.filterIsInstance<KtClass>().single() }

    matchAll(divideClass, divideTest) shouldBe mapOf(divideTest.id to ThreeState.YES)
  }

  @Test
  fun `should derive the main class of a java_binary from the label`() {
    val file = myFixture.addFileToProject(
      "BarApp.java",
      """
      package com.foo;

      public class BarApp {}
      """.trimIndent(),
    )
    val barApp = target("//java/com/foo:BarApp", JAVA_BINARY_KIND, JvmBuildTarget())

    matchAll(psiClass(file, "BarApp"), barApp) shouldBe mapOf(barApp.id to ThreeState.YES)
  }

  @Test
  fun `should give UNSURE for a target with an unknown entry point`() {
    val file = myFixture.addFileToProject(
      "DivideTest.java",
      """
      package com.foo;

      public class DivideTest {}
      """.trimIndent(),
    )
    // rules_kotlin derives no class without a source with the target name
    val divideTest = target("//java/com/foo:DivideTest", KT_JVM_TEST_KIND, JvmBuildTarget())
    val library = javaLibrary("//java/com/foo:lib")
    val known = javaTest("//java/com/foo:Known", testClass = "com.foo.DivideTest")

    matchAll(psiClass(file, "DivideTest"), divideTest, library, known) shouldBe
      mapOf(divideTest.id to ThreeState.UNSURE, library.id to ThreeState.UNSURE, known.id to ThreeState.YES)
  }

  @Test
  fun `should match the file facade class only for top-level declarations of a Kotlin file`() {
    val file = myFixture.addFileToProject(
      "FooApp.kt",
      """
      package com.foo

      fun main() {}

      class OtherTest {
        fun check() {}
      }
      """.trimIndent(),
    )
    val fooApp = target("//java/com/foo:FooApp", KOTLIN_BINARY_KIND, JvmBuildTarget(mainClass = "com.foo.FooAppKt"))
    val otherTest = target("//java/com/foo:OtherTest", KT_JVM_TEST_KIND, JvmBuildTarget(testClass = "com.foo.OtherTest"))
    val declarations = runReadActionBlocking { (file as KtFile).declarations }
    val main = declarations.filterIsInstance<KtNamedFunction>().single()
    val other = declarations.filterIsInstance<KtClass>().single()
    val check = runReadActionBlocking { other.declarations.filterIsInstance<KtNamedFunction>().single() }

    matchAll(main, fooApp, otherTest) shouldBe mapOf(fooApp.id to ThreeState.YES, otherTest.id to ThreeState.NO)
    // the facade holds each element of the file, but the declared class `OtherTest` is closer to these elements
    matchAll(other, fooApp, otherTest) shouldBe mapOf(fooApp.id to ThreeState.NO, otherTest.id to ThreeState.YES)
    matchAll(check, fooApp, otherTest) shouldBe mapOf(fooApp.id to ThreeState.NO, otherTest.id to ThreeState.YES)
    matchAll(file, fooApp, otherTest) shouldBe mapOf(fooApp.id to ThreeState.YES, otherTest.id to ThreeState.YES)
  }

  @Test
  fun `should match a file facade class with a JvmName`() {
    val file = myFixture.addFileToProject(
      "Main.kt",
      """
      @file:JvmName("Launcher")
      package com.foo

      fun main() {}
      """.trimIndent(),
    )
    val launcher = target("//java/com/foo:Launcher", KOTLIN_BINARY_KIND, JvmBuildTarget(mainClass = "com.foo.Launcher"))
    val mainKt = target("//java/com/foo:MainKt", KOTLIN_BINARY_KIND, JvmBuildTarget(mainClass = "com.foo.MainKt"))

    matchAll(topLevelFunction(file), launcher, mainKt) shouldBe mapOf(launcher.id to ThreeState.YES, mainKt.id to ThreeState.NO)
  }

  @Test
  fun `should match a multifile facade class in each of its files`() {
    val first = myFixture.addFileToProject(
      "First.kt",
      """
      @file:JvmName("Launcher")
      @file:JvmMultifileClass
      package com.foo

      fun main() {}
      """.trimIndent(),
    )
    val second = myFixture.addFileToProject(
      "Second.kt",
      """
      @file:JvmName("Launcher")
      @file:JvmMultifileClass
      package com.foo

      fun helper() {}
      """.trimIndent(),
    )
    val launcher = target("//java/com/foo:Launcher", KOTLIN_BINARY_KIND, JvmBuildTarget(mainClass = "com.foo.Launcher"))

    // the source of the multifile facade is one of its files, so the facade must match also in the other file
    matchAll(topLevelFunction(first), launcher) shouldBe mapOf(launcher.id to ThreeState.YES)
    matchAll(topLevelFunction(second), launcher) shouldBe mapOf(launcher.id to ThreeState.YES)
  }

  @Test
  fun `should match a main function in a companion object`() {
    val file = myFixture.addFileToProject(
      "FooApp.kt",
      """
      package com.foo

      class FooApp {
        companion object {
          @JvmStatic
          fun main(args: Array<String>) {}
        }
      }
      """.trimIndent(),
    )
    val fooApp = target("//java/com/foo:FooApp", KOTLIN_BINARY_KIND, JvmBuildTarget(mainClass = "com.foo.FooApp"))
    val main = runReadActionBlocking {
      val fooAppClass = (file as KtFile).declarations.filterIsInstance<KtClass>().single()
      fooAppClass.companionObjects.single().declarations.filterIsInstance<KtNamedFunction>().single()
    }

    matchAll(main, fooApp) shouldBe mapOf(fooApp.id to ThreeState.YES)
  }

  @Test
  fun `should not match in dumb mode`() {
    val file = myFixture.addFileToProject(
      "AddTest.java",
      """
      package com.foo;

      public class AddTest {}
      """.trimIndent(),
    )
    val addTest = javaTest("//java/com/foo:AddTest", testClass = "com.foo.AddTest")
    val element = psiClass(file, "AddTest")
    myFixture.testBazel.sync { targets(addTest) }

    val matches = DumbModeTestUtils.computeInDumbModeSynchronously(myFixture.project) {
      runReadActionBlocking { BazelEntryPointMatcher.matchAll(element, listOf(addTest.info)) }
    }

    matches shouldBe mapOf(addTest.id to ThreeState.UNSURE)
  }

  private fun javaTest(label: String, testClass: String?): BuildTarget =
    target(label, JAVA_TEST_KIND, JvmBuildTarget(testClass = testClass))

  private fun target(label: String, kind: TargetKind, data: JvmBuildTarget): BuildTarget =
    buildTarget(label, kind) { data(data) }

  private fun matchAll(element: PsiElement, vararg targets: BuildTarget): Map<Label, ThreeState> {
    myFixture.testBazel.sync { targets(*targets) }
    return runReadActionBlocking { BazelEntryPointMatcher.matchAll(element, targets.map { it.info }) }
  }

  private fun psiClass(file: PsiFile, name: String): PsiClass =
    runReadActionBlocking { (file as PsiClassOwner).classes.single { it.name == name } }

  private fun topLevelFunction(file: PsiFile): KtNamedFunction =
    runReadActionBlocking { (file as KtFile).declarations.filterIsInstance<KtNamedFunction>().single() }

  private fun method(file: PsiFile, className: String, methodName: String): PsiElement =
    runReadActionBlocking { psiClass(file, className).findMethodsByName(methodName, false).single() }
}

private val KOTLIN_BINARY_KIND =
  TargetKind("kt_jvm_binary", setOf(JavaLanguageClass.JAVA, JavaLanguageClass.KOTLIN), RuleType.BINARY)
