package org.jetbrains.bazel.jvm.ui.gutters

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.java.ui.gutters.BazelJavaRunLineMarkerContributor
import org.jetbrains.bazel.kotlin.ui.gutters.BazelKotlinRunLineMarkerContributor
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
    project.isBazelProject = true
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
