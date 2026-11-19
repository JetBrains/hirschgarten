package org.jetbrains.bazel.clion

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.runReadActionBlocking
import com.intellij.openapi.editor.ex.RangeHighlighterEx
import com.intellij.openapi.editor.impl.DocumentMarkupModel
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiFile
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.junit5.SystemPropertyClassLevel
import com.jetbrains.cidr.lang.CLanguageKind
import com.jetbrains.cidr.lang.workspace.OCWorkspace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@CcTestApplication
@SystemPropertyClassLevel("patch.engine.backend.freeze.timeout", "-1")
class CcUnresolvedImportCheckerTest {
  private val fixture by bazelCodeInsightFixture()
  private val checker = CcUnresolvedImportChecker()
  private val mainTarget = ccLibrary("//app:main") {
    srcs("Main.cpp")
    deps(ccLibrary("//lib:dependency"))
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

  @ParameterizedTest
  @ValueSource(booleans = [false, true])
  fun `ignores transitive dependencies`(header: Boolean) {
    val generated = ccLibrary("//lib:generated") {
      if (header) hdrs("Generated.h", generated = true) else srcs("Generated.cpp", generated = true)
    }
    val file = prepare(ccLibrary("//lib:dependency") { deps(generated) }, generated)

    assertThat(check(file)).isFalse()
  }

  @ParameterizedTest
  @ValueSource(strings = ["headers", "textualHeaders", "compilationHeaders"])
  fun `suggests generation for an unresolved include in an owned header`(field: String) {
    val file = addMainFile(path = "api.h")
    val dependency = ccLibrary("//lib:dependency") { hdrs("Generated.h", generated = true) }
    val main = ccLibrary("//app:main") {
      srcs("Main.cpp")
      when (field) {
        "headers" -> hdrs("api.h")
        "textualHeaders" -> textualHdrs("api.h")
        "compilationHeaders" -> {
          privateHdrs("api.h")
          noRuleContext()
        }
      }
      deps(dependency)
    }
    fixture.testBazel.sync { targets(main, dependency) }
    highlight(file, "missing.h")

    assertThat(check(file)).isTrue()
    writeOutput("Generated.h")
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
    highlight(file, "missing.h")
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
    val file = prepare(generatedDependency(), unresolved = false)

    assertThat(check(file)).isFalse()
  }

  @Test
  fun `ignores other compiler errors on includes`() {
    val file = prepare(generatedDependency(), unresolved = false)
    highlight(file, "missing.h", HighlightInfoType.ERROR)

    assertThat(check(file)).isFalse()
  }

  @ParameterizedTest
  @ValueSource(strings = [
    "Missing value;", "// #include \"missing.h\"", "/* #include \"missing.h\" */", "const char* text = \"missing.h\";",
    "const char* text = R\"(\n#include \"missing.h\"\n)\";", "#define HEADER \"missing.h\"", "#error missing.h",
    "#include \"present.h\"\nMissing value;",
  ])
  fun `ignores unresolved references outside include directives`(text: String) {
    val file = prepare(generatedDependency(), text = text, unresolved = false)
    highlight(file, if ("Missing" in text) "Missing" else "missing.h")

    assertThat(check(file)).isFalse()
  }

  @ParameterizedTest
  @ValueSource(strings = [
    "#include \"missing.h\"", "#include <missing.h>", "# include \"missing.h\"", "#include MISSING_HEADER",
    "#/**/include \"missing.h\"", "#\\\ninclude \"missing.h\"", "#include_next \"missing.h\"", "#import \"missing.h\"",
    "#include \\\n\"missing.h\"",
  ])
  fun `supports literal and macro includes`(text: String) {
    val file = prepare(generatedDependency(), text = text, unresolved = false)
    highlight(file, if (text.endsWith("MISSING_HEADER")) "MISSING_HEADER" else "missing.h")

    assertThat(check(file)).isTrue()
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
  fun `updates the result when include highlighting changes`() {
    val file = prepare(generatedDependency(), unresolved = false)
    assertThat(check(file)).isFalse()
    val highlighter = highlight(file, "missing.h")
    assertThat(check(file)).isTrue()

    timeoutRunBlocking { withContext(Dispatchers.EDT) { highlighter.dispose() } }

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
    text: String = "#include \"missing.h\"\nint main() {}",
    unresolved: Boolean = true,
  ): PsiFile {
    val file = addMainFile(text)
    fixture.testBazel.sync { targets(mainTarget, *dependencies) }
    if (unresolved) highlight(file, "missing.h")
    return file
  }

  private fun highlight(file: PsiFile, text: String, type: HighlightInfoType = HighlightInfoType.WRONG_REF): RangeHighlighterEx =
    timeoutRunBlocking {
      withContext(Dispatchers.EDT) {
        val document = file.viewProvider.document
        val start = document.text.indexOf(text)
        check(start >= 0)
        val markup = DocumentMarkupModel.forDocument(document, fixture.project, true)
        val highlighter = markup.addRangeHighlighter(
          type.attributesKey, start, start + text.length, HighlighterLayer.ERROR, HighlighterTargetArea.EXACT_RANGE,
        ) as RangeHighlighterEx
        val info = HighlightInfo.newHighlightInfo(type).range(start, start + text.length).createUnconditionally()
        info.setHighlighter(highlighter)
        highlighter.errorStripeTooltip = info
        highlighter
      }
    }

  private fun addMainFile(text: String = "#include \"missing.h\"\nint main() {}", path: String = "Main.cpp"): PsiFile {
    val file = fixture.addFileToProject(path, text)
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
