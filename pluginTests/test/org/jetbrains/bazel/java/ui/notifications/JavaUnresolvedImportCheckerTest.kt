package org.jetbrains.bazel.java.ui.notifications

import com.google.devtools.intellij.aspect.Common.ArtifactLocation
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.TargetIdeInfo
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.TargetKey
import com.intellij.openapi.application.runReadActionBlocking
import com.intellij.psi.PsiFile
import com.intellij.testFramework.LightVirtualFile
import com.intellij.testFramework.common.timeoutRunBlocking
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmOutputs
import org.jetbrains.bazel.sync.workspace.mapper.AspectBazelProjectMapper
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceTargetMap
import org.jetbrains.bazel.sync.workspace.snapshot.MissingGeneratedSources
import org.jetbrains.bazel.sync.workspace.snapshot.OutputLocationCollectionBuilder
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bazel.sync.workspace.snapshot.findBuildData
import org.jetbrains.bazel.sync.workspace.snapshot.workspaceSnapshotOrNull
import org.jetbrains.bazel.target.targetStorage
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.jetbrains.bazel.test.framework.testBazel
import org.jetbrains.bazel.test.framework.testproject.bazelInfo
import org.jetbrains.bazel.test.framework.testproject.javaLibrary
import org.jetbrains.bazel.test.framework.testproject.publishSnapshotAndAwait
import org.jetbrains.bazel.test.framework.testproject.sync
import org.jetbrains.bazel.test.framework.testproject.writeFile
import org.jetbrains.bazel.workspace.model.test.framework.BuildServerMock
import org.jetbrains.bazel.workspace.model.test.framework.TEST_OUTPUT_ROOT
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.OutputLocation
import org.jetbrains.bsp.protocol.TaskGroupId
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@BazelTestApplication
class JavaUnresolvedImportCheckerTest {
  private val fixture by bazelCodeInsightFixture()
  private val checker = JavaUnresolvedImportChecker()
  private val mainTarget = javaLibrary("//app:main") {
    srcs("Main.java")
    deps("//lib:dependency")
  }

  @Test
  fun `does not suggest generation without generated dependencies`() {
    val dependency = javaLibrary("//lib:dependency") { srcs("Dependency.java") }

    shouldSuggestGeneration(dependency) shouldBe false
  }

  @Test
  fun `does not suggest generation for an unrelated generated target`() {
    val dependency = javaLibrary("//lib:dependency")
    val unrelated = javaLibrary("//other:generated") { srcs("Other.java", generated = true) }

    shouldSuggestGeneration(dependency, unrelated) shouldBe false
  }

  @ParameterizedTest
  @ValueSource(booleans = [false, true])
  fun `suggests generation only while a generated source is missing`(generated: Boolean) {
    val dependency = generatedDependency()
    if (generated) writeOutput("Generated.java")

    shouldSuggestGeneration(dependency) shouldBe !generated
  }

  @Test
  fun `does not check dependencies when all imports resolve`() {
    fixture.addFileToProject("sample/Existing.java", "package sample; public class Existing {}")
    val file = fixture.addFileToProject("Main.java", "import sample.Existing; class Main {}")
    fixture.testBazel.sync { targets(mainTarget, generatedDependency()) }
    val loads = countTargetLoads()

    check(file) shouldBe false
    loads() shouldBe 0
  }

  @Test
  fun `updates the check when the generated file appears`() {
    val file = fixture.addFileToProject("Main.java", "import missing.Type; class Main {}")
    fixture.testBazel.sync { targets(mainTarget, generatedDependency()) }
    check(file) shouldBe true

    writeOutput("Generated.java")

    check(file) shouldBe false
  }

  @Test
  fun `updates the check after sync`() {
    val file = fixture.addFileToProject("Main.java", "import missing.Type; class Main {}")
    fixture.testBazel.sync { targets(mainTarget, generatedDependency()) }
    check(file) shouldBe true

    fixture.testBazel.sync { targets(mainTarget, javaLibrary("//lib:dependency")) }

    check(file) shouldBe false
  }

  @Test
  fun `does not suggest generation for a transitive dependency`() {
    val dependency = javaLibrary("//lib:dependency") { deps("//lib:generated") }
    val generated = javaLibrary("//lib:generated") { srcs("Generated.java", generated = true) }

    shouldSuggestGeneration(dependency, generated) shouldBe false
  }

  @ParameterizedTest
  @ValueSource(booleans = [false, true])
  fun `suggests generation only while a generated source of the same target is missing`(generated: Boolean) {
    val file = fixture.addFileToProject("Main.java", "import missing.Type; class Main {}")
    val main = javaLibrary("//app:main") {
      srcs("Main.java")
      srcs("Generated.java", generated = true)
    }
    fixture.testBazel.sync { targets(main) }
    if (generated) writeOutput("Generated.java")

    check(file) shouldBe !generated
  }

  @ParameterizedTest
  @ValueSource(booleans = [false, true])
  fun `preserves generated sources that are missing during aspect sync`(generated: Boolean): Unit = timeoutRunBlocking {
    val file = fixture.addFileToProject("Main.java", "import missing.*; class Main {}")
    if (generated) writeOutput("Generated.java")
    val layout = fixture.testBazel.bazelInfo
    val server = object : BuildServerMock() {
      override val bazelInfo = layout
    }
    val targetInfo = TargetIdeInfo.newBuilder()
      .setKey(TargetKey.newBuilder().setLabel("//app:main"))
      .setKind("java_library")
      .addSrcs(ArtifactLocation.newBuilder().setRelativePath("Main.java").setIsSource(true))
      .addSrcs(
        ArtifactLocation.newBuilder()
          .setRootPath("bazel-out/${TEST_OUTPUT_ROOT.path}")
          .setRelativePath("Generated.java"),
      )
      .build()
    val target = AspectBazelProjectMapper(fixture.project, server)
      .createWorkspaceTarget(targetInfo, build = false, taskId = TaskGroupId.EMPTY.task("generated-sources"))
    target.sources.getOutputLocations().count() shouldBe if (generated) 2 else 1
    target.findBuildData<MissingGeneratedSources>()?.sources?.getOutputLocations()?.count() shouldBe if (generated) null else 1
    fixture.testBazel.sync { targets(target) }

    check(file) shouldBe !generated
    if (!generated) {
      writeOutput("Generated.java")
      check(file) shouldBe false
    }
  }

  @Test
  fun `does not suggest generation for a missing generated resource`() {
    val dependency = javaLibrary("//lib:dependency") { resources("generated.txt", generated = true) }

    shouldSuggestGeneration(dependency) shouldBe false
  }

  @ParameterizedTest
  @ValueSource(booleans = [false, true])
  fun `suggests generation only while an annotation processor output is missing`(generated: Boolean) {
    val jars = outputLocations("generated.jar")
    val dependency = javaLibrary("//lib:dependency") {
      srcs("Dependency.java")
      data(JvmBuildTarget(generatedJars = listOf(JvmOutputs(binaryJars = jars))))
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

  @ParameterizedTest
  @ValueSource(strings = ["", "import sample.Existing;"])
  fun `does not suggest generation without an unresolved import`(imports: String) {
    fixture.addFileToProject("sample/Existing.java", "package sample; public class Existing {}")

    shouldSuggestGeneration(generatedDependency(), imports = imports) shouldBe false
  }

  @ParameterizedTest
  @ValueSource(strings = ["import missing.Type;", "import missing.*;", "import static missing.Type.member;"])
  fun `suggests generation for unresolved import directives`(imports: String) {
    shouldSuggestGeneration(generatedDependency(), imports = imports) shouldBe true
  }

  @Test
  fun `does not suggest generation for an unmapped file`() {
    val dependency = generatedDependency()
    fixture.testBazel.sync { targets(dependency) }
    val file = fixture.addFileToProject("Unmapped.java", "import missing.Type; class Unmapped {}")

    check(file) shouldBe false
  }

  @Test
  fun `applies to Java files in source roots`() {
    val file = fixture.addFileToProject("Main.java", "").virtualFile

    runReadActionBlocking { checker.isApplicable(fixture.project, file) } shouldBe true
  }

  @Test
  fun `does not apply to Java files outside source roots`() {
    val file = LightVirtualFile("Outside.java", "import missing.Type;")

    runReadActionBlocking { checker.isApplicable(fixture.project, file) } shouldBe false
  }

  private fun generatedDependency(): BuildTarget =
    javaLibrary("//lib:dependency") { srcs("Generated.java", generated = true) }

  private fun outputLocations(path: String) =
    OutputLocationCollectionBuilder.ofLocations(listOf(OutputLocation.Output(TEST_OUTPUT_ROOT, path)))

  private fun countTargetLoads(): () -> Int {
    val snapshot = fixture.project.workspaceSnapshotOrNull!!
    var loads = 0
    val targets = object : WorkspaceTargetMap by snapshot.targets {
      override fun findTargetByKey(key: WorkspaceTargetKey): BuildTarget? {
        loads++
        return snapshot.targets.findTargetByKey(key)
      }
    }
    fixture.project.publishSnapshotAndAwait(snapshot.copy(targets = targets))
    return { loads }
  }

  private fun writeOutput(path: String) {
    val output = fixture.project.targetStorage.resolveExecrootOutputLocation(OutputLocation.Output(TEST_OUTPUT_ROOT, path))!!
    fixture.testBazel.writeFile(output)
  }

  private fun shouldSuggestGeneration(vararg dependencies: BuildTarget, imports: String = "import missing.Type;"): Boolean {
    val file = fixture.addFileToProject("Main.java", "$imports\nclass Main {}")
    fixture.testBazel.sync { targets(mainTarget, *dependencies) }
    return check(file)
  }

  private fun check(file: PsiFile): Boolean =
    runReadActionBlocking { checker.hasUnresolvedImport(fixture.project, file) }
}
