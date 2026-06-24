package org.jetbrains.bazel.jvm.ui.gutters

import com.intellij.icons.AllIcons
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiUtilCore
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.java.ui.gutters.BazelJavaRunLineMarkerContributor
import org.jetbrains.bazel.kotlin.ui.gutters.BazelKotlinRunLineMarkerHider
import org.jetbrains.bazel.kotlin.ui.gutters.BazelKotlinRunLineMarkerContributor
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.project.BazelProjectFixtures.initializeBazelProject
import org.jetbrains.bazel.run.test.forceDisableJetBrainsTestRunner
import org.jetbrains.bazel.test.framework.target.TestBuildTargetFactory
import org.jetbrains.bsp.protocol.ExecutableTarget
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BazelRunLineMarkerContributorTest : BasePlatformTestCase() {
  @Before
  fun beforeEach() {
    myFixture.setBuildTool()
  }

  private fun CodeInsightTestFixture.setBuildTool() {
    initializeBazelProject(project, myFixture.tempDirPath)
    forceDisableJetBrainsTestRunner = true
  }

  private fun CodeInsightTestFixture.getKotlinTestFile(): PsiFile =
    configureByText(
      "BspJVMRunLineMarkerContributorTestData.kt",
      """
      package org.jetbrains.bazel.ui.gutters

      import io.kotest.matchers.shouldBe
      
      class BspJVMRunLineMarkerContributorTestData() {
        fun `should add 1 plus 1`() {
          // given
          val one = 1
          val expectedResult = 2
          // when
          val result = one + one
          // then
          result shouldBe expectedResult
        }
      }
      """.trimMargin(),
    )

  @Test
  fun `should return single test filter for Kotlin test function`() {
    // given
    val testFile = myFixture.getKotlinTestFile()
    val psiElement =
      testFile
        .getChildOfType<KtClass>()
        ?.getChildOfType<KtClassBody>()
        ?.getChildOfType<KtNamedFunction>()
        ?.getChildOfType<LeafPsiElement>()
        ?.nextSibling
        ?.nextSibling
    psiElement.shouldNotBeNull()
    val runLineMarkerContributor = BazelKotlinRunLineMarkerContributor()

    // when
    val result = runLineMarkerContributor.getSingleTestFilter(psiElement)

    // then
    val expectedSingleTestFilter = "BspJVMRunLineMarkerContributorTestData.should add 1 plus 1$"
    result shouldBe expectedSingleTestFilter
  }

  @Test
  fun `should return single test filter for Kotlin test class`() {
    // given
    val testFile = myFixture.getKotlinTestFile()
    val psiElement =
      testFile
        .getChildOfType<KtClass>()
        ?.getChildOfType<LeafPsiElement>()
        ?.nextSibling
        ?.nextSibling
    psiElement.shouldNotBeNull()
    val runLineMarkerContributor = BazelKotlinRunLineMarkerContributor()

    // when
    val result = runLineMarkerContributor.getSingleTestFilter(psiElement)

    // then
    val expectedSingleTestFilter = "BspJVMRunLineMarkerContributorTestData"
    result shouldBe expectedSingleTestFilter
  }

  @Test
  fun `should add synthetic run actions for Kotlin library top-level main`() {
    // given
    Registry.get("bazel.run.synthetic.enable").setValue(true, testRootDisposable)
    val target = TestBuildTargetFactory.createSimpleKotlinLibraryTarget(id = Label.parse("//kotlin_target:my_kt_lib"))
    val sourceFile = myFixture.configureByText(
      "main.kt",
      """
      package com.jetbrains

      fun main() {
        println("Hello from main1")
      }
      """.trimIndent(),
    )
    val elementAtCaret = PsiUtilCore.getElementAtOffset(sourceFile, sourceFile.text.indexOf("main"))!!
    val runLineMarkerContributor =
      object : BazelKotlinRunLineMarkerContributor() {
        override fun getTargets(element: PsiElement): List<ExecutableTarget> = listOf(target)
      }

    // when
    val result = runLineMarkerContributor.getInfo(elementAtCaret)

    // then
    result.shouldNotBeNull()
    result.icon shouldBe AllIcons.Actions.Execute
    result.actions.shouldHaveSize(2)
  }

  @Test
  fun `should hide platform Kotlin run marker when Bazel run marker is available`() {
    // given
    Registry.get("bazel.run.synthetic.enable").setValue(true, testRootDisposable)
    val target = TestBuildTargetFactory.createSimpleKotlinLibraryTarget(id = Label.parse("//kotlin_target:my_kt_lib"))
    val sourceFile = myFixture.configureByText(
      "main.kt",
      """
      package com.jetbrains

      fun main() {
        println("Hello from main1")
      }
      """.trimIndent(),
    )
    val elementAtCaret = PsiUtilCore.getElementAtOffset(sourceFile, sourceFile.text.indexOf("main"))!!
    val runLineMarkerContributor =
      object : BazelKotlinRunLineMarkerContributor() {
        override fun getTargets(element: PsiElement): List<ExecutableTarget> = listOf(target)
      }
    val hider = BazelKotlinRunLineMarkerHider(runLineMarkerContributor)

    // when
    val result = hider.shouldHideRunLineMarker(elementAtCaret)

    // then
    result shouldBe true
  }

  private fun CodeInsightTestFixture.getJavaTestFile(): PsiFile =
    configureByText(
      "BspJVMRunLineMarkerContributorTestData.java",
      """
      package org.jetbrains.bazel.ui.gutters;
      
      import org.junit.Test;
      
      public class BspJVMRunLineMarkerContributorTestData {
        @Test
        public void addOnePlusOne() {
          int one = 1;
          int result = one + one;
          assert(result == 2);
        }
      }
      """.trimMargin(),
    )

  @Test
  fun `should return single test filter for Java test function`() {
    // given
    val testFile = myFixture.getJavaTestFile()
    val psiElement =
      testFile
        .getChildOfType<PsiClass>()
        ?.getChildOfType<PsiMethod>()
        ?.getChildOfType<LeafPsiElement>()
        ?.nextSibling
        ?.nextSibling
        ?.nextSibling
    psiElement.shouldNotBeNull()
    val runLineMarkerContributor = BazelJavaRunLineMarkerContributor()

    // when
    val result = runLineMarkerContributor.getSingleTestFilter(psiElement)

    // then
    val expectedSingleTestFilter = "BspJVMRunLineMarkerContributorTestData.addOnePlusOne$"
    result shouldBe expectedSingleTestFilter
  }

  @Test
  fun `should return FQN test filter for Java test class`() {
    // given
    val testFile = myFixture.getJavaTestFile()
    val psiElement =
      testFile
        .getChildOfType<PsiClass>()
        ?.getChildOfType<LeafPsiElement>()
        ?.nextSibling
        ?.nextSibling
    psiElement.shouldNotBeNull()
    val runLineMarkerContributor = BazelJavaRunLineMarkerContributor()

    // when
    val result = runLineMarkerContributor.getSingleTestFilter(psiElement)

    // then
    val expectedSingleTestFilter = "org.jetbrains.bazel.ui.gutters.BspJVMRunLineMarkerContributorTestData"
    result shouldBe expectedSingleTestFilter
  }
}
