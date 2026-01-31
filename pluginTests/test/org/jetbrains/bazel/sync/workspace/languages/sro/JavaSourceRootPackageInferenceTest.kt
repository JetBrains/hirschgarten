package org.jetbrains.bazel.sync.workspace.languages.sro

import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.sync.workspace.languages.JvmPackageResolver
import org.jetbrains.bazel.sync.workspace.languages.java.sourceRoot.JavaSourceRootPackageInference
import org.jetbrains.bsp.protocol.SourceItem
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

    JavaSourceRootPackageInference(packageResolver = resolver)
      .inferPackages(sources)

    sources[0].jvmPackagePrefix shouldBe "my.package"
    sources[1].jvmPackagePrefix shouldBe "my.package"
    sources[2].jvmPackagePrefix shouldBe "my.package"
    sources[3].jvmPackagePrefix shouldBe "my.package.subpackage"
    sources[4].jvmPackagePrefix shouldBe "my.package.subpackage"
    sources[5].jvmPackagePrefix shouldBe "my.package.subpackage"
    sources[6].jvmPackagePrefix shouldBe "my.package.subpackage.subpkg"
  }

  @Test
  fun `test not matching package should be overridden`() {
    val (sources, resolver) = setupSources(
      "src/com/pkg/RootClass.java" to "com.pkg",
      "src/com/pkg/sub/Sub1.java" to "com.pkg.sub",
      "src/com/pkg/sub/Sub2.java" to "com.other_pkg",
    )

    JavaSourceRootPackageInference(packageResolver = resolver)
      .inferPackages(sources)

    sources[0].jvmPackagePrefix shouldBe "com.pkg"
    sources[1].jvmPackagePrefix shouldBe "com.pkg.sub"
    sources[2].jvmPackagePrefix shouldBe "com.pkg.sub"
  }

  @Test
  fun `test deeply nested package structures`() {
    val (sources, resolver) = setupSources(
      "src/main/java/com/company/project/module/feature/impl/RootClass.java" to "com.company.project.module.feature.impl",
      "src/main/java/com/company/project/module/feature/impl/sub/SubClass.java" to "com.company.project.module.feature.impl.sub",
      "src/main/java/com/company/project/module/feature/impl/sub/deep/DeepClass.java" to "com.company.project.module.feature.impl.sub.deep",
    )

    JavaSourceRootPackageInference(packageResolver = resolver)
      .inferPackages(sources)

    sources[0].jvmPackagePrefix shouldBe "com.company.project.module.feature.impl"
    sources[1].jvmPackagePrefix shouldBe "com.company.project.module.feature.impl.sub"
    sources[2].jvmPackagePrefix shouldBe "com.company.project.module.feature.impl.sub.deep"
  }

  @Test
  fun `test files at root level with no package`() {
    val (sources, resolver) = setupSources(
      "RootClass.java" to "",
      "AnotherRoot.java" to "",
    )

    JavaSourceRootPackageInference(packageResolver = resolver)
      .inferPackages(sources)

    sources[0].jvmPackagePrefix shouldBe ""
    sources[1].jvmPackagePrefix shouldBe ""
  }

  @Test
  fun `test mixed file extensions`() {
    val (sources, resolver) = setupSources(
      "src/main/java/com/pkg/JavaClass.java" to "com.pkg",
      "src/main/kotlin/com/pkg/KotlinClass.kt" to "com.pkg",
      "src/main/scala/com/pkg/ScalaClass.scala" to "com.pkg",
    )

    JavaSourceRootPackageInference(packageResolver = resolver)
      .inferPackages(sources)

    sources[0].jvmPackagePrefix shouldBe "com.pkg"
    sources[1].jvmPackagePrefix shouldBe "com.pkg"
    sources[2].jvmPackagePrefix shouldBe "com.pkg"
  }

  @Test
  fun `test package resolver returns null`() {
    val sources = listOf(
      SourceItem(path = Path.of("src/main/java/Class.java"), generated = false),
    )
    val resolver = object : JvmPackageResolver {
      override fun calculateJvmPackagePrefix(source: Path, multipleLines: Boolean): String? = null
    }

    JavaSourceRootPackageInference(packageResolver = resolver)
      .inferPackages(sources)

    sources[0].jvmPackagePrefix shouldBe null
  }

  @Test
  fun `test single directory with multiple conflicting packages`() {
    val (sources, resolver) = setupSources(
      "src/com/pkg/Class1.java" to "com.pkg",
      "src/com/pkg/Class2.java" to "com.other",
      "src/com/pkg/Class3.java" to "com.different",
    )

    JavaSourceRootPackageInference(packageResolver = resolver)
      .inferPackages(sources)

    sources[0].jvmPackagePrefix shouldBe "com.pkg"
    sources[1].jvmPackagePrefix shouldBe "com.pkg"
    sources[2].jvmPackagePrefix shouldBe "com.pkg"
  }

  @Test
  fun `test parallel directory structures with different packages`() {
    val (sources, resolver) = setupSources(
      "src/main/java/com/pkg1/Class1.java" to "com.pkg1",
      "src/main/java/com/pkg1/sub/SubClass1.java" to "com.pkg1.sub",
      "src/main/java/com/pkg2/Class2.java" to "com.pkg2",
      "src/main/java/com/pkg2/sub/SubClass2.java" to "com.pkg2.sub",
    )

    JavaSourceRootPackageInference(packageResolver = resolver)
      .inferPackages(sources)

    sources[0].jvmPackagePrefix shouldBe "com.pkg1"
    sources[1].jvmPackagePrefix shouldBe "com.pkg1.sub"
    sources[2].jvmPackagePrefix shouldBe "com.pkg2"
    sources[3].jvmPackagePrefix shouldBe "com.pkg2.sub"
  }

  @Test
  fun `test sources with empty string packages`() {
    val (sources, resolver) = setupSources(
      "src/Class1.java" to "",
      "src/subdir/Class2.java" to "subdir",
    )

    JavaSourceRootPackageInference(packageResolver = resolver)
      .inferPackages(sources)

    sources[0].jvmPackagePrefix shouldBe ""
    sources[1].jvmPackagePrefix shouldBe "subdir"
  }

  @Test
  fun `test files in same parent with different subdirectories`() {
    val (sources, resolver) = setupSources(
      "src/main/java/com/example/feature1/Class1.java" to "com.example.feature1",
      "src/main/java/com/example/feature1/impl/ImplClass1.java" to "com.example.feature1.impl",
      "src/main/java/com/example/feature2/Class2.java" to "com.example.feature2",
      "src/main/java/com/example/feature2/impl/ImplClass2.java" to "com.example.feature2.impl",
    )

    JavaSourceRootPackageInference(packageResolver = resolver)
      .inferPackages(sources)

    sources[0].jvmPackagePrefix shouldBe "com.example.feature1"
    sources[1].jvmPackagePrefix shouldBe "com.example.feature1.impl"
    sources[2].jvmPackagePrefix shouldBe "com.example.feature2"
    sources[3].jvmPackagePrefix shouldBe "com.example.feature2.impl"
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
  ): Pair<List<SourceItem>, JvmPackageResolver> {
    val sources = paths.map { (path, _) -> SourceItem(path = Path.of(path), generated = false) }
    val pkgs = paths.associate { (path, pkg) -> Path.of(path) to pkg }
    return sources to resolver(pkgs)
  }

  open class MockJvmPackageResolver(
    private val pkgs: Map<Path, String>,
  ) : JvmPackageResolver {
    override fun calculateJvmPackagePrefix(source: Path, multipleLines: Boolean): String? = pkgs[source]
  }
}
