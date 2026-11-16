package org.jetbrains.bazel.clion

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadActionBlocking
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiFile
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.junit5.SystemPropertyClassLevel
import com.intellij.testFramework.junit5.TestDisposable
import com.intellij.testFramework.replaceService
import com.jetbrains.cidr.lang.CLanguageKind
import com.jetbrains.cidr.lang.preprocessor.OCUnresolvedIncludes
import com.jetbrains.cidr.lang.workspace.OCWorkspace
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.bazel.clion.ui.notifications.CcUnresolvedImportChecker
import org.jetbrains.bazel.clion.workspace.CC_CLIENT_KEY
import org.jetbrains.bazel.clion.workspace.CcResolveConfiguration
import org.jetbrains.bazel.clion.workspace.encode
import org.jetbrains.bazel.fixtures.CcProjectBuilder
import org.jetbrains.bazel.fixtures.CcTargetBuilder
import org.jetbrains.bazel.fixtures.CcTestApplication
import org.jetbrains.bazel.sync.workspace.snapshot.OutputLocationCollectionBuilder
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfigurationId
import org.jetbrains.bazel.target.targetStorage
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.jetbrains.bazel.test.framework.testBazel
import org.jetbrains.bazel.test.framework.testproject.sync
import org.jetbrains.bazel.test.framework.testproject.writeFile
import org.jetbrains.bazel.workspace.model.test.framework.TEST_OUTPUT_ROOT
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.OutputLocation
import org.jetbrains.bsp.protocol.copy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@CcTestApplication
@SystemPropertyClassLevel("patch.engine.backend.freeze.timeout", "-1")
class CcUnresolvedImportCheckerTest {
  private val fixture by bazelCodeInsightFixture()
  private val checker = CcUnresolvedImportChecker()
  private val unresolvedIncludes = MutableStateFlow<Set<String>?>(null)
  private val mainTarget = ccLibrary("//app:main") {
    srcs("Main.cpp")
    deps(ccLibrary("//lib:dependency"))
  }

  @BeforeEach
  fun setUp(@TestDisposable disposable: Disposable) {
    val service = object : OCUnresolvedIncludes {
      override fun unresolvedIncludes(document: Document): StateFlow<Set<String>?> = unresolvedIncludes
    }
    ApplicationManager.getApplication().replaceService(OCUnresolvedIncludes::class.java, service, disposable)
  }

  @Test
  fun `does not suggest generation without generated dependencies`() {
    val file = prepare(ccLibrary("//lib:dependency") { srcs("Dependency.cpp") })

    assertThat(check(file)).isFalse()
  }

  @Test
  fun `does not suggest generation for an unrelated generated target`() {
    val file = prepare(ccLibrary("//lib:dependency"), ccLibrary("//other:generated") { srcs("Other.cpp", generated = true) })

    assertThat(check(file)).isFalse()
  }

  @ParameterizedTest
  @ValueSource(booleans = [false, true])
  fun `suggests generation only while a generated source is missing`(generated: Boolean) {
    val file = prepare(generatedDependency())
    if (generated) writeOutput("Generated.cpp")

    assertThat(check(file)).isEqualTo(!generated)
  }

  @ParameterizedTest
  @ValueSource(strings = ["headers", "textualHeaders", "compilationHeaders"])
  fun `checks generated headers in C++ target data`(field: String) {
    val dependency = ccLibrary("//lib:dependency") {
      when (field) {
        "headers" -> hdrs("Generated.inc", generated = true)
        "textualHeaders" -> textualHdrs("Generated.inc", generated = true)
        "compilationHeaders" -> {
          hdrs("Generated.inc", generated = true)
          noRuleContext()
        }
      }
    }
    val file = prepare(dependency)

    assertThat(check(file)).isTrue()
    writeOutput("Generated.inc")
    assertThat(check(file)).isFalse()
  }

  @Test
  fun `ignores transitive dependencies`() {
    val generated = generatedDependency("//lib:generated")
    val file = prepare(ccLibrary("//lib:dependency") { deps(generated) }, generated)

    assertThat(check(file)).isFalse()
  }

  @ParameterizedTest
  @ValueSource(booleans = [false, true])
  fun `suggests generation only while a generated source of the same target is missing`(generated: Boolean) {
    val file = addMainFile()
    val main = ccLibrary("//app:main") {
      srcs("Main.cpp")
      srcs("Generated.cpp", generated = true)
    }
    fixture.testBazel.sync { targets(main) }
    unresolvedIncludes.value = setOf("missing.h")
    if (generated) writeOutput("Generated.cpp")

    assertThat(check(file)).isEqualTo(!generated)
  }

  @ParameterizedTest
  @ValueSource(strings = ["generated.txt", "libgenerated.a", "libgenerated.so"])
  fun `ignores outputs that are not C++ code`(path: String) {
    val file = prepare(ccLibrary("//lib:dependency") { srcs(path, generated = true) })

    assertThat(check(file)).isFalse()
  }

  @Test
  fun `ignores generated resources`() {
    val dependency = ccLibrary("//lib:dependency").copy(
      resources = OutputLocationCollectionBuilder.ofLocations(listOf(OutputLocation.Output(TEST_OUTPUT_ROOT, "Generated.h"))),
    )
    val file = prepare(dependency)

    assertThat(check(file)).isFalse()
  }

  @Test
  fun `requires an unresolved include`() {
    val file = prepare(generatedDependency(), unresolved = emptySet())

    assertThat(check(file)).isFalse()
  }

  @Test
  fun `does not suggest generation while the engine does not track the document`() {
    val file = prepare(generatedDependency(), unresolved = null)

    assertThat(check(file)).isFalse()
  }

  @Test
  fun `updates the result after sync`() {
    val file = prepare(generatedDependency())
    assertThat(check(file)).isTrue()

    fixture.testBazel.sync { targets(mainTarget, ccLibrary("//lib:dependency")) }

    assertThat(check(file)).isFalse()
  }

  @Test
  fun `does not suggest generation for an unmapped file`() {
    val file = prepare(generatedDependency())
    fixture.testBazel.sync { targets(generatedDependency()) }

    assertThat(check(file)).isFalse()
  }

  @Test
  fun `updates the result when the unresolved includes change`() {
    val file = prepare(generatedDependency(), unresolved = emptySet())
    assertThat(check(file)).isFalse()
    unresolvedIncludes.value = setOf("missing.h")
    assertThat(check(file)).isTrue()

    unresolvedIncludes.value = emptySet()

    assertThat(check(file)).isFalse()
  }

  private fun generatedDependency(label: String = "//lib:dependency"): BuildTarget =
    ccLibrary(label) { srcs("Generated.cpp", generated = true) }

  private fun ccLibrary(label: String, declare: CcTargetBuilder.() -> Unit = {}): BuildTarget =
    CcProjectBuilder().ccLibrary {
      label(label)
      declare()
    }

  private fun prepare(
    vararg dependencies: BuildTarget,
    unresolved: Set<String>? = setOf("missing.h"),
  ): PsiFile {
    val file = addMainFile()
    fixture.testBazel.sync { targets(mainTarget, *dependencies) }
    unresolvedIncludes.value = unresolved
    return file
  }

  private fun addMainFile(text: String = "#include \"missing.h\"\nint main() {}"): PsiFile {
    val file = fixture.addFileToProject("Main.cpp", text)
    val model = OCWorkspace.getInstance(fixture.project).getModifiableModel(CC_CLIENT_KEY, true)
    try {
      val id = CcResolveConfiguration.Identifier(0, WorkspaceConfigurationId.of("test")).encode()
      model.addConfiguration(id, "test").addSource(file.virtualFile, CLanguageKind.CPP)
      model.preCommit()
      timeoutRunBlocking { model.commitAndContribute() }
    } finally {
      Disposer.dispose(model)
    }
    return file
  }

  private fun writeOutput(path: String) {
    val output = fixture.project.targetStorage.resolveExecrootOutputLocation(OutputLocation.Output(TEST_OUTPUT_ROOT, path))!!
    fixture.testBazel.writeFile(output)
  }

  private fun check(file: PsiFile): Boolean =
    runReadActionBlocking { checker.hasUnresolvedImport(fixture.project, file) }
}
