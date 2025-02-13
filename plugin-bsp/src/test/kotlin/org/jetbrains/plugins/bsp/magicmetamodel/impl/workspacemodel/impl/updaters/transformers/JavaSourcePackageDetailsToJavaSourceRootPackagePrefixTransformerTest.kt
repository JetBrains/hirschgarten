package org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.net.URI
import kotlin.io.path.toPath

@DisplayName("JavaSourcePackageDetailsToJavaSourceRootPackagePrefixTransformer.transform(javaSourcePackageDetails) tests")
class JavaSourcePackageDetailsToJavaSourceRootPackagePrefixTransformerTest {
  @Test
  fun `should return no package for equal source dir and single root`() {
    // given
    val sourceDir = URI.create("file:///example/package/root/").toPath()
    val roots = setOf(URI.create("file:///example/package/root/").toPath())
    val javaSourcePackageDetails = JavaSourcePackageDetails(sourceDir, roots)

    // when
    val packagePrefix =
      JavaSourcePackageDetailsToJavaSourceRootPackagePrefixTransformer.transform(javaSourcePackageDetails)

    // then
    packagePrefix shouldBe JavaSourceRootPackagePrefix("")
  }

  @Test
  fun `should return no package for equal source dir and one root from all roots`() {
    // given
    val sourceDir = URI.create("file:///example/package/root/").toPath()
    val roots =
      setOf(
        URI.create("file:///another/example/package/root/").toPath(),
        URI.create("file:///example/package/root/").toPath(),
        URI.create("file:///another/another/example/package/root/").toPath(),
      )
    val javaSourcePackageDetails = JavaSourcePackageDetails(sourceDir, roots)

    // when
    val packagePrefix =
      JavaSourcePackageDetailsToJavaSourceRootPackagePrefixTransformer.transform(javaSourcePackageDetails)

    // then
    packagePrefix shouldBe JavaSourceRootPackagePrefix("")
  }

  @Test
  fun `should return package for source dir and single root`() {
    // given
    val sourceDir = URI.create("file:///root/dir/example/package/").toPath()
    val roots = setOf(URI.create("file:///root/dir/").toPath())
    val javaSourcePackageDetails = JavaSourcePackageDetails(sourceDir, roots)

    // when
    val packagePrefix =
      JavaSourcePackageDetailsToJavaSourceRootPackagePrefixTransformer.transform(javaSourcePackageDetails)

    // then
    packagePrefix shouldBe JavaSourceRootPackagePrefix("example.package")
  }

  @Test
  fun `should return package for source dir and multiple roots`() {
    // given
    val sourceDir = URI.create("file:///root/dir/example/package/").toPath()
    val roots =
      setOf(
        URI.create("file:///another/example/package/root/").toPath(),
        URI.create("file:///root/dir/").toPath(),
        URI.create("file:///another/another/example/package/root/").toPath(),
      )
    val javaSourcePackageDetails = JavaSourcePackageDetails(sourceDir, roots)

    // when
    val packagePrefix =
      JavaSourcePackageDetailsToJavaSourceRootPackagePrefixTransformer.transform(javaSourcePackageDetails)

    // then
    packagePrefix shouldBe JavaSourceRootPackagePrefix("example.package")
  }

  @Test
  fun `should return no package for source dir being parent for roots`() {
    // given
    val sourceDir = URI.create("file:///example/package/").toPath()
    val roots =
      setOf(
        URI.create("file:///another/example/package/root/").toPath(),
        URI.create("file:///example/package/root/").toPath(),
        URI.create("file:///another/another/example/package/root/").toPath(),
      )
    val javaSourcePackageDetails = JavaSourcePackageDetails(sourceDir, roots)

    // when
    val packagePrefix =
      JavaSourcePackageDetailsToJavaSourceRootPackagePrefixTransformer.transform(javaSourcePackageDetails)

    // then
    packagePrefix shouldBe JavaSourceRootPackagePrefix("")
  }

  @Test
  fun `should return no package for no matching roots`() {
    // given
    val sourceDir = URI.create("file:///root/dir/example/package/").toPath()
    val roots =
      setOf(
        URI.create("file:///another/root/dir/example/package/").toPath(),
        URI.create("file:///not/matching/root/dir/").toPath(),
        URI.create("file:///another/another/root/dir/example/package/").toPath(),
      )
    val javaSourcePackageDetails = JavaSourcePackageDetails(sourceDir, roots)

    // when
    val packagePrefix =
      JavaSourcePackageDetailsToJavaSourceRootPackagePrefixTransformer.transform(javaSourcePackageDetails)

    // then
    packagePrefix shouldBe JavaSourceRootPackagePrefix("")
  }
}
