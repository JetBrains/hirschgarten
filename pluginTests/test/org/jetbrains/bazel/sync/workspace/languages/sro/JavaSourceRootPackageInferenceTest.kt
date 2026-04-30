package org.jetbrains.bazel.sync.workspace.languages.sro

import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.sync.workspace.languages.JvmPackageResolver
import org.jetbrains.bazel.sync.workspace.languages.java.sourceRoot.JavaSourceRootPackageInference
import org.junit.jupiter.api.Test
import java.nio.file.Path

class JavaSourceRootPackageInferenceTest {

  @Test
  fun `test general maven package layout inference`() {
    val (sources, resolver) = setupSources(
      "src/main/java/my/package/RootClass1.java" to "my.package",
      "src/main/java/my/package/RootClass2.java" to "my.package",
      "src/main/java/my/package/RootClass3.java" to "my.package",
      "src/main/java/my/package/subpackage/SubpackageClass.java" to "my.package.subpackage",
      "src/main/java/my/package/subpackage/SubpackageClass2.java" to "my.package.subpackage",
      "src/main/java/my/package/subpackage/SubpackageClass3.java" to "my.package.subpackage",
      "src/main/java/my/package/subpackage/subpkg/SubpackageClass.java" to "my.package.subpackage.subpkg",
    )

    val inferred = JavaSourceRootPackageInference(packageResolver = resolver)
      .inferPackages(sources)

    inferred[sources[0]] shouldBe "my.package"
    inferred[sources[1]] shouldBe "my.package"
    inferred[sources[2]] shouldBe "my.package"
    inferred[sources[3]] shouldBe "my.package.subpackage"
    inferred[sources[4]] shouldBe "my.package.subpackage"
    inferred[sources[5]] shouldBe "my.package.subpackage"
    inferred[sources[6]] shouldBe "my.package.subpackage.subpkg"
  }

  @Test
  fun `test not matching package should be overridden`() {
    val (sources, resolver) = setupSources(
      "src/com/pkg/RootClass.java" to "com.pkg",
      "src/com/pkg/sub/Sub1.java" to "com.pkg.sub",
      "src/com/pkg/sub/Sub2.java" to "com.other_pkg",
    )

    val inferred = JavaSourceRootPackageInference(packageResolver = resolver)
      .inferPackages(sources)

    inferred[sources[0]] shouldBe "com.pkg"
    inferred[sources[1]] shouldBe "com.pkg.sub"
    inferred[sources[2]] shouldBe "com.pkg.sub"
  }

  @Test
  fun `test deeply nested package structures`() {
    val (sources, resolver) = setupSources(
      "src/main/java/com/company/project/module/feature/impl/RootClass.java" to "com.company.project.module.feature.impl",
      "src/main/java/com/company/project/module/feature/impl/sub/SubClass.java" to "com.company.project.module.feature.impl.sub",
      "src/main/java/com/company/project/module/feature/impl/sub/deep/DeepClass.java" to "com.company.project.module.feature.impl.sub.deep",
    )

    val inferred = JavaSourceRootPackageInference(packageResolver = resolver)
      .inferPackages(sources)

    inferred[sources[0]] shouldBe "com.company.project.module.feature.impl"
    inferred[sources[1]] shouldBe "com.company.project.module.feature.impl.sub"
    inferred[sources[2]] shouldBe "com.company.project.module.feature.impl.sub.deep"
  }

  @Test
  fun `test files at root level with no package`() {
    val (sources, resolver) = setupSources(
      "RootClass.java" to "",
      "AnotherRoot.java" to "",
    )

    val inferred = JavaSourceRootPackageInference(packageResolver = resolver)
      .inferPackages(sources)

    inferred[sources[0]] shouldBe ""
    inferred[sources[1]] shouldBe ""
  }

  @Test
  fun `test mixed file extensions`() {
    val (sources, resolver) = setupSources(
      "src/main/java/com/pkg/JavaClass.java" to "com.pkg",
      "src/main/kotlin/com/pkg/KotlinClass.kt" to "com.pkg",
      "src/main/scala/com/pkg/ScalaClass.scala" to "com.pkg",
    )

    val inferred = JavaSourceRootPackageInference(packageResolver = resolver)
      .inferPackages(sources)

    inferred[sources[0]] shouldBe "com.pkg"
    inferred[sources[1]] shouldBe "com.pkg"
    inferred[sources[2]] shouldBe "com.pkg"
  }

  @Test
  fun `test package resolver returns null`() {
    val sources = listOf(
      Path.of("src/main/java/Class.java"),
    )
    val resolver = object : JvmPackageResolver {
      override fun calculateJvmPackagePrefix(source: Path, multipleLines: Boolean): String? = null
    }

    val inferred = JavaSourceRootPackageInference(packageResolver = resolver)
      .inferPackages(sources)

    inferred.size shouldBe 0
  }

  @Test
  fun `test single directory with multiple conflicting packages`() {
    val (sources, resolver) = setupSources(
      "src/com/pkg/Class1.java" to "com.pkg",
      "src/com/pkg/Class2.java" to "com.other",
      "src/com/pkg/Class3.java" to "com.different",
    )

    val inferred = JavaSourceRootPackageInference(packageResolver = resolver)
      .inferPackages(sources)

    inferred[sources[0]] shouldBe "com.pkg"
    inferred[sources[1]] shouldBe "com.pkg"
    inferred[sources[2]] shouldBe "com.pkg"
  }

  @Test
  fun `test parallel directory structures with different packages`() {
    val (sources, resolver) = setupSources(
      "src/main/java/com/pkg1/Class1.java" to "com.pkg1",
      "src/main/java/com/pkg1/sub/SubClass1.java" to "com.pkg1.sub",
      "src/main/java/com/pkg2/Class2.java" to "com.pkg2",
      "src/main/java/com/pkg2/sub/SubClass2.java" to "com.pkg2.sub",
    )

    val inferred = JavaSourceRootPackageInference(packageResolver = resolver)
      .inferPackages(sources)

    inferred[sources[0]] shouldBe "com.pkg1"
    inferred[sources[1]] shouldBe "com.pkg1.sub"
    inferred[sources[2]] shouldBe "com.pkg2"
    inferred[sources[3]] shouldBe "com.pkg2.sub"
  }

  @Test
  fun `test sources with empty string packages`() {
    val (sources, resolver) = setupSources(
      "src/Class1.java" to "",
      "src/subdir/Class2.java" to "subdir",
    )

    val inferred = JavaSourceRootPackageInference(packageResolver = resolver)
      .inferPackages(sources)

    inferred[sources[0]] shouldBe ""
    inferred[sources[1]] shouldBe "subdir"
  }

  @Test
  fun `test files in same parent with different subdirectories`() {
    val (sources, resolver) = setupSources(
      "src/main/java/com/example/feature1/Class1.java" to "com.example.feature1",
      "src/main/java/com/example/feature1/impl/ImplClass1.java" to "com.example.feature1.impl",
      "src/main/java/com/example/feature2/Class2.java" to "com.example.feature2",
      "src/main/java/com/example/feature2/impl/ImplClass2.java" to "com.example.feature2.impl",
    )

    val inferred = JavaSourceRootPackageInference(packageResolver = resolver)
      .inferPackages(sources)

    inferred[sources[0]] shouldBe "com.example.feature1"
    inferred[sources[1]] shouldBe "com.example.feature1.impl"
    inferred[sources[2]] shouldBe "com.example.feature2"
    inferred[sources[3]] shouldBe "com.example.feature2.impl"
  }

  @Test
  fun `test file read decreased`() {
    var callCount: Int = 0
    val (sources, resolver) = setupSources(
      paths = arrayOf(
        "src/main/java/com/example/level1/Class1.java" to "com.example.level1",
        "src/main/java/com/example/level1/Class2.java" to "com.example.level1",
        "src/main/java/com/example/level1/Class3.java" to "com.example.level1",
        "src/main/java/com/example/level1/Class4.java" to "com.example.level1",
        "src/main/java/com/example/level1/level2/Class5.java" to "com.example.level1.level2",
        "src/main/java/com/example/level1/level2/Class6.java" to "com.example.level1.level2",
        "src/main/java/com/example/level1/level2/Class7.java" to "com.example.level1.level2",
        "src/main/java/com/example/level1/level2/level3/Class8.java" to "com.example.level1.level2.level3",
        "src/main/java/com/example/level1/level2/level3/Class9.java" to "com.example.level1.level2.level3",
        "src/main/java/com/example/level1/level2/level3/Class10.java" to "com.example.level1.level2.level3",
        "src/main/java/com/example/level1/level2/level3/Class11.java" to "com.example.level1.level2.level3",
        "src/main/java/com/example/level1/level2/level3/level4/Class12.java" to "com.example.level1.level2.level3.level4",
        "src/main/java/com/example/level1/level2/level3/level4/Class13.java" to "com.example.level1.level2.level3.level4",
        "src/main/java/com/example/level1/level2/level3/level4/Class14.java" to "com.example.level1.level2.level3.level4",
        "src/main/java/com/example/level1/level2/level3/level4/level5/Class15.java" to "com.example.level1.level2.level3.level4.level5",
        "src/main/java/com/example/level1/level2/level3/level4/level5/Class16.java" to "com.example.level1.level2.level3.level4.level5",
        "src/main/java/com/example/level1/level2/level3/level4/level5/Class17.java" to "com.example.level1.level2.level3.level4.level5",
        "src/main/java/com/example/level1/level2/level3/level4/level5/Class18.java" to "com.example.level1.level2.level3.level4.level5",
        "src/main/java/com/example/level1/level2/level3/level4/level5/Class19.java" to "com.example.level1.level2.level3.level4.level5",
        "src/main/java/com/example/level1/level2/level3/level4/level5/Class20.java" to "com.example.level1.level2.level3.level4.level5",
      ),
      resolver = { pkgs ->
        object : MockJvmPackageResolver(pkgs) {
          override fun calculateJvmPackagePrefix(source: Path, multipleLines: Boolean): String? {
            callCount++
            return super.calculateJvmPackagePrefix(source, multipleLines)
          }
        }
      },
    )

    JavaSourceRootPackageInference(packageResolver = resolver)
      .inferPackages(sources)

    callCount.shouldBeLessThan(sources.size)
  }


  private fun setupSources(
    vararg paths: Pair<String, String>,
    resolver: (pkgs: Map<Path, String>) -> JvmPackageResolver = { MockJvmPackageResolver(it) },
  ): Pair<List<Path>, JvmPackageResolver> {
    val sources = paths.map { (path, _) -> Path.of(path) }
    val pkgs = paths.associate { (path, pkg) -> Path.of(path) to pkg }
    return sources to resolver(pkgs)
  }

  open class MockJvmPackageResolver(
    val pkgs: Map<Path, String>,
  ) : JvmPackageResolver {
    override fun calculateJvmPackagePrefix(source: Path, multipleLines: Boolean): String? = pkgs[source]
  }
}
