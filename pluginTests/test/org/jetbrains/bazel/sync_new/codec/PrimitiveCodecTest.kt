package org.jetbrains.bazel.sync_new.codec

import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.label.AllPackagesBeneath
import org.jetbrains.bazel.label.AllRuleTargets
import org.jetbrains.bazel.label.AllRuleTargetsAndFiles
import org.jetbrains.bazel.label.AmbiguousEmptyTarget
import org.jetbrains.bazel.label.Apparent
import org.jetbrains.bazel.label.Canonical
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.Main
import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bazel.label.SingleTarget
import org.jetbrains.bazel.sync_new.codec.kryo.KryoCodecContext
import org.jetbrains.bazel.sync_new.storage.util.UnsafeByteBufferCodecBuffer
import org.jetbrains.bazel.label.Package
import org.jetbrains.bazel.label.RelativeLabel
import org.jetbrains.bazel.label.SyntheticLabel
import org.jetbrains.bazel.sync_new.graph.impl.BazelPath
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.FieldSource

internal class PrimitiveCodecTest {

  companion object {

    val labelTestCases = listOf(
      Label.parse("@//test"),
      Label.parse("@//package/path:target"),
      Label.parse("@@//package:target"),
      Label.parse("//:target"),
      Label.parse("//a/b/c/d:target"),
      Label.parse("//package/path"),

      Label.parse("@@external_repo//package:target"),
      Label.parse("@@my_repo//:target"),
      Label.parse("@@repo//a/b/c:my_target"),

      Label.parse("@external_repo//package:target"),
      Label.parse("@my_repo//:target"),
      Label.parse("@repo//a/b/c:my_target"),

      Label.parse("//package:*"),
      Label.parse("//package:all-targets"),
      Label.parse("@@repo//package:*"),

      Label.parse("//package:all"),
      Label.parse("@repo//package:all"),

      Label.parse("//..."),
      Label.parse("//package/path/..."),
      Label.parse("@@repo//path/..."),
      Label.parse("@repo//some/path/..."),
      Label.parse("//...:all"),
      Label.parse("//...:*"),

      Label.parse("package:target"),
      Label.parse("a/b/c:target"),
      Label.parse("package/path"),
      Label.parse("package:all"),
      Label.parse("package:*"),
      Label.parse("package/..."),
      Label.parse(":target"),

      Label.synthetic("synthetic_target"),
      Label.parse("my_target[synthetic]"),
      Label.synthetic("complex/target/name"),

      Label.parse("//package:file.java"),
      Label.parse("//package:my_target_name"),
      Label.parse("//package:my-target-name"),
      Label.parse("//package:target123"),
      Label.parse("//my-package/sub_package:target"),

      ResolvedLabel(Main, Package(listOf("pkg")), SingleTarget("target")),
      ResolvedLabel(Canonical.createCanonicalOrMain("repo") as Canonical, Package(listOf("pkg")), SingleTarget("target")),
      ResolvedLabel(Apparent("repo"), Package(listOf("pkg")), SingleTarget("target")),
      RelativeLabel(Package(listOf("a", "b")), SingleTarget("target")),
      SyntheticLabel(SingleTarget("target")),
      ResolvedLabel(Main, AllPackagesBeneath(listOf("a", "b")), AllRuleTargets),
      ResolvedLabel(Main, Package(listOf("package")), AmbiguousEmptyTarget),
      ResolvedLabel(Main, Package(listOf("package")), AllRuleTargetsAndFiles),
      ResolvedLabel(Main, Package(emptyList()), SingleTarget("target")),
      ResolvedLabel(Main, AllPackagesBeneath(emptyList()), AllRuleTargets),
    )

    @JvmStatic
    val bazelPathTestCases = listOf(
      BazelPath.Absolute("/absolute/path/to/file.txt"),
      BazelPath.Absolute("/"),
      BazelPath.Absolute("/single"),
      BazelPath.Absolute("/path/with/many/segments/file.java"),
      BazelPath.Absolute("relative/looking/but/absolute"),

      BazelPath.MainWorkspace("src/main/java/File.java"),
      BazelPath.MainWorkspace(""),
      BazelPath.MainWorkspace("single"),
      BazelPath.MainWorkspace("deeply/nested/path/to/resource.txt"),
      BazelPath.MainWorkspace("path-with-dashes/and_underscores/file.kt"),

      BazelPath.ExternalWorkspace("external/repo_name", "src/File.java", isSource = true),
      BazelPath.ExternalWorkspace("external/repo", "", isSource = true),
      BazelPath.ExternalWorkspace("external/my_repo", "deeply/nested/path.txt", isSource = true),

      BazelPath.ExternalWorkspace("bazel-out/k8-fastbuild/bin/external/repo", "generated/File.java", isSource = false),
      BazelPath.ExternalWorkspace("external/repo", "path/to/file", isSource = false),
      BazelPath.ExternalWorkspace("", "relative/path", isSource = false),

      BazelPath.Absolute("/path/with spaces/file.txt"),
      BazelPath.MainWorkspace("path/with.dots/and-dashes/file_name.ext"),
      BazelPath.ExternalWorkspace("external/repo~version", "src/main.kt", isSource = true),
      BazelPath.ExternalWorkspace("external/repo@1.0.0", "lib/util.java", isSource = false),
    )
  }

  @ParameterizedTest(name = "{0}")
  @FieldSource("labelTestCases")
  fun `test label codec`(label: Label) {
    testCodecNaive(label, LabelCodec)
  }

  @ParameterizedTest(name = "{0}")
  @FieldSource("bazelPathTestCases")
  fun `test bazel path codec`(path: BazelPath) {
    testCodecNaive(path, BazelPathCodec)
  }

  fun <T> testCodecNaive(template: T, codec: Codec<T>) {
    val buffer = UnsafeByteBufferCodecBuffer.allocateHeap()
    codec.encode(KryoCodecContext, buffer, template)
    buffer.flip()
    val result = codec.decode(KryoCodecContext, buffer)
    result.shouldBe(template)
  }

}
