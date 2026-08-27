package org.jetbrains.bsp.protocol

import com.google.devtools.intellij.aspect.Common
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.commons.BzlmodRepoMapping
import org.junit.jupiter.api.Test
import kotlin.io.path.Path

class OutputLocationParserTest {

  private fun proto(
    rootPath: String,
    relativePath: String,
    isSource: Boolean = true,
    isExternal: Boolean = false,
  ): Common.ArtifactLocation =
    Common.ArtifactLocation
      .newBuilder()
      .setRootPath(rootPath)
      .setRelativePath(relativePath)
      .setIsSource(isSource)
      .setIsExternal(isExternal)
      .build()

  private fun output(vararg rootSegments: String, relativePath: String): OutputLocation.Output =
    OutputLocation.Output(OutputRoot.of(rootSegments.toList()), relativePath)

  @Test
  fun `classifies an absolute unix path as host`() {
    OutputLocation.parseExecrootPath("/usr/bin/clang") shouldBe OutputLocation.Host("/usr/bin/clang")
  }

  @Test
  fun `classifies an absolute windows path as host`() {
    OutputLocation.parseExecrootPath("C:\\LLVM\\bin\\clang.exe") shouldBe OutputLocation.Host("C:\\LLVM\\bin\\clang.exe")
  }

  @Test
  fun `strips the proc self cwd marker`() {
    OutputLocation.parseExecrootPath("/proc/self/cwd/bazel-out/k8-opt/bin/foo/gen.h") shouldBe
      output("k8-opt", "bin", relativePath = "foo/gen.h")
  }

  @Test
  fun `does not strip a proc self cwd prefix without a segment boundary`() {
    OutputLocation.parseExecrootPath("/proc/self/cwdfoo") shouldBe OutputLocation.Host("/proc/self/cwdfoo")
  }

  @Test
  fun `maps the exact proc self cwd marker to the empty workspace location`() {
    OutputLocation.parseExecrootPath("/proc/self/cwd") shouldBe OutputLocation.Workspace("")
  }

  @Test
  fun `splits the output root into the mnemonic and the output directory`() {
    OutputLocation.parseExecrootPath("bazel-out/k8-opt/bin/pkg/f.h") shouldBe
      output("k8-opt", "bin", relativePath = "pkg/f.h")
  }

  @Test
  fun `classifies an external path`() {
    OutputLocation.parseExecrootPath("external/rules_cc+/include/x.h") shouldBe
      OutputLocation.External("rules_cc+", "include/x.h")
  }

  @Test
  fun `classifies a sibling layout path as external`() {
    OutputLocation.parseExecrootPath("../llvm+/include/x.h") shouldBe
      OutputLocation.External("llvm+", "include/x.h", siblingLayout = true)
  }

  @Test
  fun `maps a convenience symlink name to the workspace`() {
    OutputLocation.parseExecrootPath("bazel-bin/foo/gen.h") shouldBe OutputLocation.Workspace("bazel-bin/foo/gen.h")
  }

  @Test
  fun `maps a plain relative path to the workspace`() {
    OutputLocation.parseExecrootPath("foo/bar.h") shouldBe OutputLocation.Workspace("foo/bar.h")
  }

  @Test
  fun `maps the empty string to the empty workspace location`() {
    OutputLocation.parseExecrootPath("") shouldBe OutputLocation.Workspace("")
  }

  @Test
  fun `keeps a dot path`() {
    OutputLocation.parseExecrootPath(".") shouldBe OutputLocation.Workspace(".")
  }

  @Test
  fun `execroot paths tests e2e`() {
    val paths = listOf(
      "/usr/bin/clang",
      "external/rules_cc+/include/x.h",
      "external/rules_cc+",
      "../llvm+/include/x.h",
      "../repo+/bazel-out/k8-fastbuild/bin/pkg/gen.h",
      "bazel-out/k8-opt/bin/pkg/f.h",
      "bazel-out/k8-opt/testlogs/pkg/test.log",
      "foo/bar.h",
      "bazel-bin/foo/gen.h",
      ".",
      "",
    )
    for (path in paths) {
      OutputLocation.parseExecrootPath(path).toExecrootPath() shouldBe path
    }
  }

  @Test
  fun `parses a main workspace source`() {
    OutputLocation.parse(proto("", "c/d/E.java")) shouldBe OutputLocation.Workspace("c/d/E.java")
  }

  @Test
  fun `parses an external source with an external root`() {
    OutputLocation.parse(proto("external/foo+", "d/E.java", isExternal = true)) shouldBe
      OutputLocation.External("foo+", "d/E.java")
  }

  @Test
  fun `parses an external source with a sibling root`() {
    OutputLocation.parse(proto("../foo+", "d/E.java", isExternal = true)) shouldBe
      OutputLocation.External("foo+", "d/E.java", siblingLayout = true)
  }

  @Test
  fun `parses a generated file`() {
    OutputLocation.parse(proto("bazel-out/k8-fastbuild/bin", "pkg/gen.h", isSource = false)) shouldBe
      output("k8-fastbuild", "bin", relativePath = "pkg/gen.h")
  }

  @Test
  fun `parses a generated file of an external target as an output`() {
    OutputLocation.parse(proto("bazel-out/k8-fastbuild/bin", "external/bar+/lib.jar", isSource = false, isExternal = true)) shouldBe
      output("k8-fastbuild", "bin", relativePath = "external/bar+/lib.jar")
  }

  @Test
  fun `parses a sibling layout generated external file`() {
    OutputLocation.parse(
      proto("../repo+", "bazel-out/k8-fastbuild/bin/pkg/gen.h", isSource = false, isExternal = true),
    ) shouldBe OutputLocation.External("repo+", "bazel-out/k8-fastbuild/bin/pkg/gen.h", siblingLayout = true)
  }

  @Test
  fun `parses an absolute from_execpath location as host`() {
    // local_jdk java_home: from_execpath sets is_source = false and an empty root
    OutputLocation.parse(proto("", "/Library/Java/home", isSource = false)) shouldBe
      OutputLocation.Host("/Library/Java/home")
  }

  @Test
  fun `parses an external from_execpath location`() {
    // remotejdk java_home: from_execpath sets is_external = true for an external root
    OutputLocation.parse(proto("external/remotejdk17+", "some/dir", isSource = false, isExternal = true)) shouldBe
      OutputLocation.External("remotejdk17+", "some/dir")
  }

  @Test
  fun `classifies identically from a string and from a proto`() {
    val fromString = OutputLocation.parseExecrootPath("bazel-out/k8-opt/bin/pkg/f.h")
    val fromProto = OutputLocation.parse(proto("bazel-out/k8-opt/bin", "pkg/f.h", isSource = false))
    fromString shouldBe fromProto
  }

  @Test
  fun `isExternal is structural`() {
    OutputLocation.parse(proto("external/bar+", "a/b/c/d/E.java", isExternal = true)).isExternal.shouldBeTrue()
    OutputLocation.parse(proto("../bar+", "a/b/c/d/E.java", isExternal = true)).isExternal.shouldBeTrue()
    OutputLocation.parse(proto("", "c/d/E.java")).isExternal.shouldBeFalse()
    output("k8-fastbuild", "bin", relativePath = "external/bar+/lib.jar").isExternal.shouldBeFalse()
    OutputLocation.Host("/usr/bin/clang").isExternal.shouldBeFalse()
  }

  @Test
  fun `isSource excludes a sibling layout generated external file`() {
    val generated = OutputLocation.parse(
      proto("../repo+", "bazel-out/k8-fastbuild/bin/pkg/gen.h", isSource = false, isExternal = true),
    )
    generated.isGenerated.shouldBeTrue()
    generated.isSource.shouldBeFalse()

    OutputLocation.External("repo+", "include/x.h", siblingLayout = true).isSource.shouldBeTrue()
    OutputLocation.External("rules_cc+", "include/x.h").isSource.shouldBeTrue()
    output("k8-fastbuild", "bin", relativePath = "pkg/gen.h").isSource.shouldBeFalse()
    OutputLocation.Workspace("c/d/E.java").isSource.shouldBeTrue()
    OutputLocation.Host("/usr/bin/clang").isSource.shouldBeTrue()
  }

  @Test
  fun `isUserCode respects local overrides`() {
    val repoMapping = BzlmodRepoMapping(
      canonicalRepoNameToLocalPath = mapOf("foo+" to Path("bar/baz")),
      apparentRepoNameToCanonicalName = mapOf(),
      canonicalRepoNameToPath = mapOf(),
      nonLocalCanonicalRepoNames = setOf(),
    )

    OutputLocation.Workspace("c/d/E.java").isUserCode(repoMapping).shouldBeTrue()
    OutputLocation.External("foo+", "d/E.java").isUserCode(repoMapping).shouldBeTrue()
    OutputLocation.External("bar+", "d/E.java").isUserCode(repoMapping).shouldBeFalse()
    output("k8-fastbuild", "bin", relativePath = "some/internal/lib.jar").isUserCode(repoMapping).shouldBeTrue()
    output("k8-fastbuild", "bin", relativePath = "external/foo+/lib.jar").isUserCode(repoMapping).shouldBeTrue()
    output("k8-fastbuild", "bin", relativePath = "external/bar+/lib.jar").isUserCode(repoMapping).shouldBeFalse()
    OutputLocation.Host("/usr/bin/clang").isUserCode(repoMapping).shouldBeFalse()
  }

  @Test
  fun `spells a sibling layout output with a parent prefix`() {
    OutputLocation.External("repo+", "bazel-out/k8-fastbuild/bin/pkg/gen.h", siblingLayout = true).toExecrootPath() shouldBe
      "../repo+/bazel-out/k8-fastbuild/bin/pkg/gen.h"
  }

  @Test
  fun `spells an empty relative path without a trailing slash`() {
    OutputLocation.External("rules_cc+", "").toExecrootPath() shouldBe "external/rules_cc+"
    OutputLocation.External("repo+", "", siblingLayout = true).toExecrootPath() shouldBe "../repo+"
    output("k8-opt", "bin", relativePath = "").toExecrootPath() shouldBe "bazel-out/k8-opt/bin"
  }

  @Test
  fun `keeps the sibling form for an external source`() {
    val location = OutputLocation.parseExecrootPath("../llvm+/include/x.h")
    location shouldBe OutputLocation.External("llvm+", "include/x.h", siblingLayout = true)
    location.toExecrootPath() shouldBe "../llvm+/include/x.h"
  }
}
