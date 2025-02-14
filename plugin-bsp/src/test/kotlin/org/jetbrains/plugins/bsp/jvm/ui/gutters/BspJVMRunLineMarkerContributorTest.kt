package org.jetbrains.plugins.bsp.jvm.ui.gutters

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.config.BazelPluginConstants.bazelBspBuildToolId
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.plugins.bsp.config.buildToolId
import org.jetbrains.plugins.bsp.config.isBspProject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BspJVMRunLineMarkerContributorTest : BasePlatformTestCase() {
  @Before
  fun beforeEach() {
    myFixture.setBuildTool()
  }

  private fun CodeInsightTestFixture.setBuildTool() {
    project.isBspProject = true
    project.buildToolId = bazelBspBuildToolId
  }

  private fun CodeInsightTestFixture.getKotlinTestFile(): PsiFile =
    configureByText(
      "BspJVMRunLineMarkerContributorTestData.kt",
      """
      package org.jetbrains.plugins.bsp.ui.gutters

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
    val runLineMarkerContributor = BspJVMRunLineMarkerContributor()

    // when
    val result = runLineMarkerContributor.getSingleTestFilter(psiElement)

    // then
    val expectedSingleTestFilter = "should add 1 plus 1"
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
    val runLineMarkerContributor = BspJVMRunLineMarkerContributor()

    // when
    val result = runLineMarkerContributor.getSingleTestFilter(psiElement)

    // then
    val expectedSingleTestFilter = "BspJVMRunLineMarkerContributorTestData"
    result shouldBe expectedSingleTestFilter
  }

  private fun CodeInsightTestFixture.getJavaTestFile(): PsiFile =
    configureByText(
      "BspJVMRunLineMarkerContributorTestData.java",
      """
      package org.jetbrains.plugins.bsp.ui.gutters;
      
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
    val runLineMarkerContributor = BspJVMRunLineMarkerContributor()

    // when
    val result = runLineMarkerContributor.getSingleTestFilter(psiElement)

    // then
    val expectedSingleTestFilter = "addOnePlusOne"
    result shouldBe expectedSingleTestFilter
  }

  @Test
  fun `should return single test filter for Java test class`() {
    // given
    val testFile = myFixture.getJavaTestFile()
    val psiElement =
      testFile
        .getChildOfType<PsiClass>()
        ?.getChildOfType<LeafPsiElement>()
        ?.nextSibling
        ?.nextSibling
    psiElement.shouldNotBeNull()
    val runLineMarkerContributor = BspJVMRunLineMarkerContributor()

    // when
    val result = runLineMarkerContributor.getSingleTestFilter(psiElement)

    // then
    val expectedSingleTestFilter = "BspJVMRunLineMarkerContributorTestData"
    result shouldBe expectedSingleTestFilter
  }
}
