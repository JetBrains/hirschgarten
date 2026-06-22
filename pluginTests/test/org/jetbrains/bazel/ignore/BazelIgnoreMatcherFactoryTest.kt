package org.jetbrains.bazel.ignore

import com.intellij.testFramework.junit5.TestApplication
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.nio.file.Path

@TestApplication
class BazelIgnoreMatcherFactoryTest {
  @Test
  fun `empty content returns EMPTY matcher`() {
    BazelIgnoreMatcherFactory.fromBazelIgnoreFile("") shouldBe BazelIgnoreMatcher.EMPTY
  }

  @Test
  fun `blank-only content returns EMPTY matcher`() {
    BazelIgnoreMatcherFactory.fromBazelIgnoreFile("\n\n   \n\t\n") shouldBe BazelIgnoreMatcher.EMPTY
  }

  @Test
  fun `comment-only content returns EMPTY matcher`() {
    val content = """
      # one
      # two
      # three
    """.trimIndent()

    BazelIgnoreMatcherFactory.fromBazelIgnoreFile(content) shouldBe BazelIgnoreMatcher.EMPTY
  }

  @Test
  fun `mixed blanks comments and entries keep only real entries`() {
    val content = """
      # leading comment

      out
      # trailing comment

    """.trimIndent()
    val matcher = BazelIgnoreMatcherFactory.fromBazelIgnoreFile(content)

    matcher.match(Path.of("out")).shouldBeTrue()
    matcher.match(Path.of("out/build.log")).shouldBeTrue()
    matcher.match(Path.of("outside")).shouldBeFalse()
    matcher.match(Path.of("src/Main.kt")).shouldBeFalse()
  }

  @Test
  fun `single-segment entry matches itself`() {
    val matcher = BazelIgnoreMatcherFactory.fromBazelIgnoreFile(SAMPLE_BAZELIGNORE)

    matcher.match(Path.of(".git")).shouldBeTrue()
    matcher.match(Path.of(".claude")).shouldBeTrue()
    matcher.match(Path.of("out")).shouldBeTrue()
    matcher.match(Path.of("system")).shouldBeTrue()
  }

  @Test
  fun `single-segment entry matches descendants`() {
    val matcher = BazelIgnoreMatcherFactory.fromBazelIgnoreFile(SAMPLE_BAZELIGNORE)

    matcher.match(Path.of(".git/HEAD")).shouldBeTrue()
    matcher.match(Path.of(".git/refs/heads/main")).shouldBeTrue()
    matcher.match(Path.of("out/some/nested/file.txt")).shouldBeTrue()
  }

  @Test
  fun `multi-segment entry matches descendants`() {
    val matcher = BazelIgnoreMatcherFactory.fromBazelIgnoreFile(SAMPLE_BAZELIGNORE)

    matcher.match(Path.of("dotnet/Obj/something/x.txt")).shouldBeTrue()
    matcher.match(Path.of("dotnet/Bin.RiderBackend")).shouldBeTrue()
    matcher.match(Path.of("dotnet/Bin.RiderBackend/inner")).shouldBeTrue()
    matcher.match(Path.of("plugins/bazel/pluginTests/testData/testProjects/foo")).shouldBeTrue()
    matcher.match(Path.of("community/build/jvm-rules/sub/BUILD.bazel")).shouldBeTrue()
  }

  @Test
  fun `unrelated paths do not match`() {
    val matcher = BazelIgnoreMatcherFactory.fromBazelIgnoreFile(SAMPLE_BAZELIGNORE)

    matcher.match(Path.of("src/main/Foo.kt")).shouldBeFalse()
    matcher.match(Path.of("plugins/bazel/intellij.bazel.core/src/Foo.kt")).shouldBeFalse()
    matcher.match(Path.of("dotnet/Source")).shouldBeFalse()
  }

  @Test
  fun `partial directory-name prefix does not match`() {
    val matcher = BazelIgnoreMatcherFactory.fromBazelIgnoreFile(SAMPLE_BAZELIGNORE)

    matcher.match(Path.of("communityX")).shouldBeFalse()
    matcher.match(Path.of("dotnet/Bin")).shouldBeFalse()
    matcher.match(Path.of("outside")).shouldBeFalse()
  }

  @Test
  fun `comment lines from sample are not treated as entries`() {
    val matcher = BazelIgnoreMatcherFactory.fromBazelIgnoreFile(SAMPLE_BAZELIGNORE)

    matcher.match(Path.of("Keep")).shouldBeFalse()
    matcher.match(Path.of("# Keep")).shouldBeFalse()
    matcher.match(Path.of("specifically")).shouldBeFalse()
  }

  @Test
  fun `empty REPO bazel content returns EMPTY matcher`() {
    BazelIgnoreMatcherFactory.fromRepoBazelFile("") shouldBe BazelIgnoreMatcher.EMPTY
  }

  @Test
  fun `repo bazel without ignore_directories call returns EMPTY matcher`() {
    val content = """
      # just a comment, no ignore_directories call here
    """.trimIndent()

    BazelIgnoreMatcherFactory.fromRepoBazelFile(content) shouldBe BazelIgnoreMatcher.EMPTY
  }

  @Test
  fun `repo bazel with empty ignore_directories list returns EMPTY matcher`() {
    BazelIgnoreMatcherFactory.fromRepoBazelFile("ignore_directories([])") shouldBe BazelIgnoreMatcher.EMPTY
  }

  @Test
  fun `repo bazel with non-list argument to ignore_directories returns EMPTY matcher`() {
    BazelIgnoreMatcherFactory.fromRepoBazelFile("""ignore_directories("node_modules")""") shouldBe BazelIgnoreMatcher.EMPTY
  }

  @Test
  fun `repo bazel with multiple arguments to ignore_directories returns EMPTY matcher`() {
    BazelIgnoreMatcherFactory.fromRepoBazelFile("""ignore_directories(["a"], ["b"])""") shouldBe BazelIgnoreMatcher.EMPTY
  }

  @Test
  fun `formatjs REPO bazel matches top-level ignored directories`() {
    val matcher = BazelIgnoreMatcherFactory.fromRepoBazelFile(SAMPLE_REPO_BAZEL)

    matcher.match(Path.of("node_modules")).shouldBeTrue()
    matcher.match(Path.of("dist")).shouldBeTrue()
    matcher.match(Path.of("bazel-out")).shouldBeTrue()
    matcher.match(Path.of("target")).shouldBeTrue()
    matcher.match(Path.of("examples")).shouldBeTrue()
  }

  @Test
  fun `formatjs REPO bazel matches descendants of top-level ignored directories`() {
    val matcher = BazelIgnoreMatcherFactory.fromRepoBazelFile(SAMPLE_REPO_BAZEL)

    matcher.match(Path.of("node_modules/react")).shouldBeTrue()
    matcher.match(Path.of("node_modules/@scope/pkg/index.js")).shouldBeTrue()
    matcher.match(Path.of("dist/main.js")).shouldBeTrue()
    matcher.match(Path.of("bazel-out/k8-fastbuild/bin/foo")).shouldBeTrue()
    matcher.match(Path.of("examples/intl/index.tsx")).shouldBeTrue()
  }

  @Test
  fun `formatjs REPO bazel matches nested node_modules via glob pattern`() {
    val matcher = BazelIgnoreMatcherFactory.fromRepoBazelFile(SAMPLE_REPO_BAZEL)

    matcher.match(Path.of("packages/intl-messageformat/node_modules")).shouldBeTrue()
    matcher.match(Path.of("packages/intl-messageformat/node_modules/react")).shouldBeTrue()
    matcher.match(Path.of("a/b/c/d/node_modules/x/y/z.js")).shouldBeTrue()
  }

  @Test
  fun `formatjs REPO bazel does not match unrelated paths`() {
    val matcher = BazelIgnoreMatcherFactory.fromRepoBazelFile(SAMPLE_REPO_BAZEL)

    matcher.match(Path.of("src/index.ts")).shouldBeFalse()
    matcher.match(Path.of("packages/intl-messageformat/src/index.ts")).shouldBeFalse()
    matcher.match(Path.of("node_modules2")).shouldBeFalse()
    matcher.match(Path.of("not_node_modules")).shouldBeFalse()
    matcher.match(Path.of("dist_old")).shouldBeFalse()
  }

  @Test
  fun `repo bazel with glob wildcards works like glob`() {
    val content = """
      ignore_directories([
          "build-*",
          "**/.cache",
      ])
    """.trimIndent()
    val matcher = BazelIgnoreMatcherFactory.fromRepoBazelFile(content)

    matcher.match(Path.of("build-foo")).shouldBeTrue()
    matcher.match(Path.of("build-bar/inner")).shouldBeTrue()
    matcher.match(Path.of("build")).shouldBeFalse()

    matcher.match(Path.of(".cache")).shouldBeTrue()
    matcher.match(Path.of("packages/foo/.cache/x")).shouldBeTrue()
    matcher.match(Path.of("cache")).shouldBeFalse()
  }
}

private val SAMPLE_BAZELIGNORE = """
  # specifically ignore .ultimate.root.marker file,
  # so execroot won't be detected as an ultimate root
  .ultimate.root.marker
  .git
  .claude

  dotnet/Bin.RiderBackend
  dotnet/Obj

  plugins/bazel/pluginTests/testData/testProjects

  out
  system
  CIDR/clion/main/testData
  CIDR/regression-testData
  dotnet/Psi.Features/Cpp/test/data
  # Keep repo-backing dirs (lib, community, community/lib, community/build/jvm-rules) past
  # position 8: macOS watchfs excludes only the first 8 .bazelignore prefixes from FSEvents
  # (MacOSXFsEventsDiffAwareness), so otherwise edits to their BUILD.bazel are missed
  # ("Unexpected short read"). Workaround until https://github.com/bazelbuild/bazel/pull/26921
  lib
  community
  community/lib
  community/build/jvm-rules

  test-config
  test-system
  testData

  community/build/dependencies/build/android-sdk/prebuilts/studio/sdk
  plugins/jupyter/frontend/node_modules
  plugins/bazel/pluginTests/testData
  contrib/Angular/angular-backend/src-js
  fleet/plugins/bazel/integrationTest/testData
""".trimIndent()

private val SAMPLE_REPO_BAZEL = """
  # Repository-level ignore patterns
  # This file replaces .bazelignore (deprecated in Bazel 8.0+)
  #
  # Use ignore_directories() to exclude directories from Bazel's visibility.
  # These directories will not be scanned for BUILD files.
  #
  # Note: ignore_directories() must be called at most once per REPO.bazel file,
  # and if called, it must be the first statement in the file.
  # Supports the same wildcards as glob().

  ignore_directories([
      # Common build output and dependency directories
      "node_modules",
      "dist",
      "bazel-out",
      "target",
      "examples",
      # All node_modules directories recursively using glob pattern
      "**/node_modules",
  ])
""".trimIndent()
