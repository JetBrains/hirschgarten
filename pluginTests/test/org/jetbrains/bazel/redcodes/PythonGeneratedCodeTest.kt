package org.jetbrains.bazel.redcodes

import com.intellij.openapi.application.EDT
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.junit5.RegistryKey
import com.jetbrains.python.inspections.unresolvedReference.PyUnresolvedReferencesInspection
import com.jetbrains.python.psi.PyTargetExpression
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.sync.environment.projectCtx
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelSyncCodeInsightFixture
import org.jetbrains.bazel.test.framework.checkHighlighting
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.time.Duration.Companion.minutes

@BazelTestApplication
class PythonGeneratedCodeTest {

  private val fixture by bazelSyncCodeInsightFixture(
    "redcodes/python_generated_code",
    buildProject = true,
    configure = {
      Path(it.tempDirPath).resolve("genpy/consumer.py").writeText("import data\nfrom data import D\n\nC = D + data.D\n")
    },
  )

  @Test
  @Timeout(300)
  @RegistryKey(key = "bazel.hard.link.output.files", value = "true")
  fun testGeneratedPythonSourceResolves(): Unit = timeoutRunBlocking(timeout = 5.minutes) {
    checkGeneratedReferences("D = P + 1")
    fixture.performBazelSync()
    checkGeneratedReferences("D = P + 1")

    val buildFile = Path(fixture.tempDirPath).resolve("genpy/BUILD")
    buildFile.writeText(buildFile.readText().replace("D = P + 1", "D = P + 2"))
    fixture.performBazelSync(buildProject = true)
    checkGeneratedReferences("D = P + 2")
  }

  private suspend fun checkGeneratedReferences(expectedDefinition: String) {
    fixture.project.projectCtx.bazelBinPath!!.resolve("genpy/data.py").exists().shouldBeTrue()
    withContext(Dispatchers.EDT) {
      fixture.enableInspections(PyUnresolvedReferencesInspection())
      fixture.checkHighlighting("genpy/consumer.py")
      val file = fixture.file
      for (offset in listOf(file.text.indexOf("D +"), file.text.lastIndexOf("D"))) {
        val reference = file.findReferenceAt(offset) as PsiPolyVariantReference
        val definitions = reference.multiResolve(false).mapNotNull { it.element }.filterIsInstance<PyTargetExpression>()
        definitions shouldHaveSize 1
        val definitionFile = checkNotNull(definitions.single().containingFile)
        definitionFile.virtualFile.path shouldContain "/intellij-hardlinks/"
        definitionFile.text shouldContain expectedDefinition
      }
    }
  }
}
