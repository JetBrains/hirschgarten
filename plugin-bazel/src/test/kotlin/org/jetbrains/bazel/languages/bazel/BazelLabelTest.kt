package org.jetbrains.bazel.languages.bazel

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class BazelLabelTest {
  @Test
  fun `canonical qualified label`() {
    // given
    val canonicalQualifiedLabel = "@@myrepo//my/app/main:app_binary"

    // when
    val label = BazelLabel.ofString(canonicalQualifiedLabel)

    // then
    label.repoName shouldBe "myrepo"
    label.packageName shouldBe "my/app/main"
    label.targetName shouldBe "app_binary"
    label.qualifiedTargetName shouldBe "myrepo//my/app/main:app_binary"
  }

  @Test
  fun `same repo label`() {
    // given
    val sameRepoLabel = "//my/app/main:app_binary"

    // when
    val label = BazelLabel.ofString(sameRepoLabel)

    // then
    label.repoName shouldBe ""
    label.packageName shouldBe "my/app/main"
    label.targetName shouldBe "app_binary"
    label.qualifiedTargetName shouldBe sameRepoLabel
  }

  @Test
  fun `same package file label`() {
    // given
    val samePackageFileLabel = "app_binary"

    // when
    val label = BazelLabel.ofString(samePackageFileLabel)

    // then
    label.repoName shouldBe ""
    label.packageName shouldBe ""
    label.targetName shouldBe samePackageFileLabel
  }

  @Test
  fun `same package rule label`() {
    // given
    val samePackageRuleLabel = ":app_binary"

    // when
    val label = BazelLabel.ofString(samePackageRuleLabel)

    // then
    label.repoName shouldBe ""
    label.packageName shouldBe ""
    label.targetName shouldBe "app_binary"
  }

  @Test
  fun `target name matches last component`() {
    // given
    val targetNameMatchesLastComponentLabel = "//my/app/lib"

    // when
    val label = BazelLabel.ofString(targetNameMatchesLastComponentLabel)

    // then
    label.repoName shouldBe ""
    label.packageName shouldBe "my/app/lib"
    label.targetName shouldBe "lib"
  }
}
