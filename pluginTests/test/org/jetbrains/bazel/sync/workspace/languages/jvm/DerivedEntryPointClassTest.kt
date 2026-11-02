package org.jetbrains.bazel.sync.workspace.languages.jvm

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.snapshot.OutputLocationCollectionBuilder
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bazel.test.framework.target.TestBuildTarget
import org.jetbrains.bazel.workspace.model.test.framework.TEST_OUTPUT_ROOT
import org.jetbrains.bsp.protocol.OutputLocation
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class DerivedEntryPointClassTest {
  @Nested
  inner class RulesJava {
    @Test
    fun `should derive a maven layout below a package`() {
      primaryClass("//projects/svc:src/test/java/com/example/calculator/CalculatorAddTest") shouldBe
        "com.example.calculator.CalculatorAddTest"
    }

    @Test
    fun `should derive a maven layout with a default package`() {
      primaryClass("//src/test/java:FooTest") shouldBe "FooTest"
    }

    @Test
    fun `should derive a java root`() {
      primaryClass("//java/com/foo:FooTest") shouldBe "com.foo.FooTest"
    }

    @Test
    fun `should derive a javatests root`() {
      primaryClass("//javatests/com/foo:FooTest") shouldBe "com.foo.FooTest"
    }

    @Test
    fun `should derive a nested src root before a top-level package`() {
      primaryClass("//src/foo:src/com/x/XTest") shouldBe "com.x.XTest"
    }

    @Test
    fun `should derive a nested src root below a java root`() {
      primaryClass("//java/com/x:src/com/y/YTest") shouldBe "com.y.YTest"
    }

    @Test
    fun `should keep the src root when a nested java root has no top-level package`() {
      primaryClass("//src/x/java/foo:BarTest") shouldBe "x.java.foo.BarTest"
    }

    @Test
    fun `should prefer a maven layout to a nested package root`() {
      primaryClass("//src/main/resources:java/com/x/XTest") shouldBe "java.com.x.XTest"
    }

    @Test
    fun `should derive a target in the root package`() {
      primaryClass("//:java/com/foo/FooTest") shouldBe "com.foo.FooTest"
    }

    @Test
    fun `should not derive a label without a java root`() {
      primaryClass("//foo:FooTest").shouldBeNull()
    }

    @Test
    fun `should not derive a java root without a class`() {
      primaryClass("//:java").shouldBeNull()
    }

    @Test
    fun `should derive the path of the source with the target name`() {
      primaryClass("//java/com/foo:BarTest", "java/com/foo/sub/BarTest.java") shouldBe "com.foo.sub.BarTest"
    }

    @Test
    fun `should derive a binary from the source with the target name`() {
      primaryClass("//java/com/foo:Main", "java/com/foo/sub/Main.java") shouldBe "com.foo.sub.Main"
    }

    @Test
    fun `should derive the label when no source has the target name`() {
      primaryClass("//java/com/foo:LabelOnlyTest", "java/com/foo/Other.java") shouldBe "com.foo.LabelOnlyTest"
    }

    @Test
    fun `should derive the first source with the target name`() {
      primaryClass(
        "//java/com/foo:FirstMatchTest",
        "java/com/foo/a/FirstMatchTest.java",
        "java/com/foo/b/FirstMatchTest.java",
      ) shouldBe "com.foo.a.FirstMatchTest"
    }

    @Test
    fun `should derive a maven layout in a source when the label has no java root`() {
      primaryClass("//svc:FooTest", "svc/src/test/java/com/foo/FooTest.java") shouldBe "com.foo.FooTest"
    }

    @Test
    fun `should derive a generated source`() {
      val source = OutputLocation.Output(TEST_OUTPUT_ROOT, "java/com/foo/gen/sub2/GenTest.java")
      rulesJavaPrimaryClass(target("//java/com/foo:GenTest", JAVA_TEST_KIND, source)) shouldBe "com.foo.gen.sub2.GenTest"
    }

    @Test
    fun `should derive a source in an external repository`() {
      val source = OutputLocation.External("ext+", "lib/src/test/java/com/ext/ExtTest.java")
      rulesJavaPrimaryClass(target("@ext//lib:ExtTest", JAVA_TEST_KIND, source)) shouldBe "com.ext.ExtTest"
    }

    @Test
    fun `should not derive a source without a java root`() {
      primaryClass("//:Root", "Root.java").shouldBeNull()
    }

    private fun primaryClass(label: String, vararg srcs: String): String? =
      rulesJavaPrimaryClass(target(label, JAVA_TEST_KIND, *srcs.map(OutputLocation::Workspace).toTypedArray()))
  }

  @Nested
  inner class RulesKotlin {
    @Test
    fun `should derive a source with the target name below a java root`() {
      testClass("//java/com/foo:FooTest", "java/com/foo/FooTest.kt") shouldBe "com.foo.FooTest"
    }

    @Test
    fun `should derive a java source`() {
      testClass("//java/com/foo:JTest", "java/com/foo/JTest.java") shouldBe "com.foo.JTest"
    }

    @Test
    fun `should derive a target name with slashes`() {
      testClass("//svc:src/test/kotlin/com/foo/SlashTest", "svc/src/test/kotlin/com/foo/SlashTest.kt") shouldBe "com.foo.SlashTest"
    }

    @Test
    fun `should not derive a source in a subdirectory of the package`() {
      testClass("//svc:FooTest", "svc/src/test/kotlin/com/foo/FooTest.kt").shouldBeNull()
    }

    @Test
    fun `should not derive a source without a known root`() {
      testClass("//plain:PlainTest", "plain/PlainTest.kt").shouldBeNull()
    }

    @Test
    fun `should split at the first root in the list order`() {
      testClass("//kotlin/test/x:OrderTest", "kotlin/test/x/OrderTest.kt") shouldBe "test.x.OrderTest"
    }

    @Test
    fun `should take the last matching source in the root package`() {
      testClass("//:kotlinjavaFoo", "kotlin/java/Foo.kt", "kotlinjava/Foo.kt") shouldBe "Foo"
    }

    @Test
    fun `should not derive a source in an external repository`() {
      val source = OutputLocation.External("ext+", "lib/src/test/kotlin/com/ext/ExtTest.kt")
      rulesKotlinTestClass(target("@ext//lib:ExtTest", KT_JVM_TEST_KIND, source)).shouldBeNull()
    }

    @Test
    fun `should not derive a target without srcs`() {
      testClass("//java/com/foo:FooTest").shouldBeNull()
    }

    private fun testClass(label: String, vararg srcs: String): String? =
      rulesKotlinTestClass(target(label, KT_JVM_TEST_KIND, *srcs.map(OutputLocation::Workspace).toTypedArray()))
  }
}

private fun target(label: String, kind: TargetKind, vararg srcs: OutputLocation): TestBuildTarget =
  TestBuildTarget(
    key = WorkspaceTargetKey(label = Label.parse(label)),
    kind = kind,
    sources = OutputLocationCollectionBuilder.ofLocations(srcs.toList()),
  )
