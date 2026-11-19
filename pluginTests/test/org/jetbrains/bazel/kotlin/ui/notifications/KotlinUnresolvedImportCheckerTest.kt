package org.jetbrains.bazel.kotlin.ui.notifications

import com.intellij.openapi.application.runReadActionBlocking
import com.intellij.psi.PsiFile
import com.intellij.testFramework.LightVirtualFile
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmOutputs
import org.jetbrains.bazel.sync.workspace.snapshot.OutputLocationCollectionBuilder
import org.jetbrains.bazel.target.targetStorage
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.jetbrains.bazel.test.framework.testBazel
import org.jetbrains.bazel.test.framework.testproject.javaLibrary
import org.jetbrains.bazel.test.framework.testproject.kotlinLibrary
import org.jetbrains.bazel.test.framework.testproject.sync
import org.jetbrains.bazel.test.framework.testproject.writeFile
import org.jetbrains.bazel.workspace.model.test.framework.TEST_OUTPUT_ROOT
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.OutputLocation
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@BazelTestApplication
class KotlinUnresolvedImportCheckerTest {
  private val fixture by bazelCodeInsightFixture()
  private val checker = KotlinUnresolvedImportChecker()
  private val mainTarget = kotlinLibrary("//app:main") {
    srcs("Main.kt")
    deps("//lib:dependency")
  }

  @Test
  fun `does not suggest generation without generated dependencies`() {
    val dependency = kotlinLibrary("//lib:dependency") { srcs("Dependency.kt") }

    shouldSuggestGeneration(dependency) shouldBe false
  }

  @ParameterizedTest
  @ValueSource(booleans = [false, true])
  fun `suggests generation only while a generated source is missing`(generated: Boolean) {
    val dependency = generatedDependency()
    if (generated) writeOutput()

    shouldSuggestGeneration(dependency) shouldBe !generated
  }

  @ParameterizedTest
  @ValueSource(booleans = [false, true])
  fun `suggests generation only while an annotation processor output is missing`(generated: Boolean) {
    val dependency = javaLibrary("//lib:dependency") {
      srcs("Dependency.java")
      data(JvmBuildTarget(generatedJars = listOf(JvmOutputs(binaryJars = outputLocations("generated.jar")))))
    }
    if (generated) writeOutput("generated.jar")

    shouldSuggestGeneration(dependency) shouldBe !generated
  }

  @ParameterizedTest
  @ValueSource(booleans = [false, true])
  fun `suggests generation only while a protocol buffer output is missing`(generated: Boolean) {
    val dependency = javaLibrary("//lib:dependency") {
      srcs("message.proto")
      data(JvmBuildTarget(binaryOutputs = outputLocations("message.jar")))
    }
    if (generated) writeOutput("message.jar")

    shouldSuggestGeneration(dependency) shouldBe !generated
  }

  @Test
  fun `does not suggest generation for an ordinary binary output`() {
    val dependency = kotlinLibrary("//lib:dependency") {
      srcs("Dependency.kt")
      data(JvmBuildTarget(binaryOutputs = outputLocations("dependency.jar")))
    }

    shouldSuggestGeneration(dependency) shouldBe false
  }

  @Test
  fun `updates the check when the generated file appears`() {
    val file = fixture.addFileToProject("Main.kt", "import missing.Type\nclass Main")
    fixture.testBazel.sync { targets(mainTarget, generatedDependency()) }
    check(file) shouldBe true

    writeOutput()

    check(file) shouldBe false
  }

  @ParameterizedTest
  @ValueSource(strings = ["", "import sample.Existing"])
  fun `does not suggest generation without an unresolved import`(imports: String) {
    fixture.addFileToProject("sample/Existing.kt", "package sample\nclass Existing")

    shouldSuggestGeneration(generatedDependency(), imports = imports) shouldBe false
  }

  @Test
  fun `does not suggest generation for an unmapped file`() {
    fixture.testBazel.sync { targets(generatedDependency()) }
    val file = fixture.addFileToProject("Unmapped.kt", "import missing.Type\nclass Unmapped")

    check(file) shouldBe false
  }

  @Test
  fun `applies to Kotlin files in source roots`() {
    val file = fixture.addFileToProject("Main.kt", "").virtualFile

    runReadActionBlocking { checker.isApplicable(fixture.project, file) } shouldBe true
  }

  @Test
  fun `does not apply to Kotlin files outside source roots`() {
    val file = LightVirtualFile("Outside.kt", "import missing.Type")

    runReadActionBlocking { checker.isApplicable(fixture.project, file) } shouldBe false
  }

  private fun generatedDependency(): BuildTarget =
    kotlinLibrary("//lib:dependency") { srcs("Generated.kt", generated = true) }

  private fun outputLocations(path: String) =
    OutputLocationCollectionBuilder.ofLocations(listOf(OutputLocation.Output(TEST_OUTPUT_ROOT, path)))

  private fun writeOutput(path: String = "Generated.kt") {
    val output = fixture.project.targetStorage.resolveExecrootOutputLocation(OutputLocation.Output(TEST_OUTPUT_ROOT, path))!!
    fixture.testBazel.writeFile(output)
  }

  private fun shouldSuggestGeneration(vararg dependencies: BuildTarget, imports: String = "import missing.Type"): Boolean {
    val file = fixture.addFileToProject("Main.kt", "$imports\nclass Main")
    fixture.testBazel.sync { targets(mainTarget, *dependencies) }
    return check(file)
  }

  private fun check(file: PsiFile): Boolean =
    runReadActionBlocking { checker.hasUnresolvedImport(fixture.project, file) }
}
