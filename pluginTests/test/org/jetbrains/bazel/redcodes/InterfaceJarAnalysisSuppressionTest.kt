package org.jetbrains.bazel.redcodes

import com.intellij.codeInspection.dataFlow.ConstantValueInspection
import com.intellij.codeInspection.dataFlow.DataFlowInspection
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelSyncCodeInsightFixture
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

@BazelTestApplication
class InterfaceJarAnalysisSuppressionTest {

  private val projectFixture = projectFixture(openAfterCreation = true)
  private val tempDir = tempPathFixture()
  private val fixture by bazelSyncCodeInsightFixture(projectFixture, tempDir)

  @Test
  fun testHighlighting(): Unit = timeoutRunBlocking(timeout = 5.minutes) {
    fixture.copyBazelTestProject("redcodes/interface_jar_analysis")
    fixture.setProjectView(projectview = ".bazelproject")
    fixture.performBazelSync(buildProject = true)
    withContext(Dispatchers.EDT) {
      fixture.enableInspections(ConstantValueInspection(), DataFlowInspection())
      val psiFile = fixture.configureByFile("app/App.java")
      readAction {
        psiFile.libraryMethodOf("InterfaceJarLib.next()").containingFile.virtualFile.path shouldContain "-hjar.jar!/"
        psiFile.libraryMethodOf("KotlinHalf.INSTANCE.next()").containingFile.virtualFile.path shouldContain ".abi.jar!/"
        psiFile.libraryMethodOf("JavaHalf.next()").containingFile.virtualFile.path shouldContain ".abi.jar!/"
      }
      fixture.checkHighlighting()
    }
  }
}

private fun PsiFile.libraryMethodOf(call: String): PsiMethod {
  val offset = text.indexOf(call)
  offset shouldBeGreaterThanOrEqual 0
  val nameOffset = offset + call.lastIndexOf('.') + 1
  val resolved = findReferenceAt(nameOffset)?.resolve() ?: error("unresolved reference at '$call'")
  return resolved as? PsiMethod ?: error("'$call' resolved to ${resolved.javaClass.name}, not a method")
}
