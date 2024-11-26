package org.jetbrains.bsp.bazel.server.sync.firstStep.mappings

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.SourceItemKind
import ch.epfl.scala.bsp4j.SourcesItem
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.jetbrains.bsp.protocol.EnhancedSourceItem
import org.jetbrains.bsp.protocol.EnhancedSourceItemData
import org.junit.jupiter.api.Test
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.createParentDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText

class TargetToBspSourcesItemMapperTest {
  @Test
  fun `should map java target to sources item`() {
    // given
    val workspaceRoot = createTempDirectory("workspaceRoot").also { it.toFile().deleteOnExit() }
    val root12 = workspaceRoot.resolve("target/name").createDirectories()
    val source1 = workspaceRoot.resolve("target/name/src1.java").createFile()
    source1.writeText(
      """"
        | package com.example;
        |
        | class A { }
      """.trimMargin(),
    )

    val source2 = workspaceRoot.resolve("target/name/src2.java").createFile()
    source2.writeText(
      """"
        | package com.example;
        |
        | class B { }
      """.trimMargin(),
    )

    val root3 = root12.resolve("package").createDirectories()
    val source3 = workspaceRoot.resolve("target/name/package/src3.java").createParentDirectories().createFile()
    source3.writeText(
      """"
        | package com.example.package;
        |
        | class C { }
      """.trimMargin(),
    )

    val target =
      createMockTarget(
        name = "//target/name",
        kind = "java_library",
        srcs = listOf("//target/name:src1.java", "//target/name:src2.java", "//target/name:package/src3.java"),
      )

    // when
    val result = target.toBspSourcesItem(workspaceRoot)

    // then
    val expected =
      SourcesItem(
        BuildTargetIdentifier("//target/name"),
        listOf(
          EnhancedSourceItem(source1.toUri().toString(), SourceItemKind.FILE, false),
          EnhancedSourceItem(source2.toUri().toString(), SourceItemKind.FILE, false),
          EnhancedSourceItem(source3.toUri().toString(), SourceItemKind.FILE, false),
        ),
      ).apply {
        roots = listOf(root12.toUri().toString(), root3.toUri().toString())
      }

    result shouldBe expected
    result.sources.map { it as EnhancedSourceItem }.map { it.data } shouldContainExactly
      listOf(
        EnhancedSourceItemData(jvmPackagePrefix = "com.example"),
        EnhancedSourceItemData(jvmPackagePrefix = "com.example"),
        EnhancedSourceItemData(jvmPackagePrefix = "com.example.package"),
      )
  }

  @Test
  fun `should map kotlin target to sources item`() {
    // given
    val workspaceRoot = createTempDirectory("workspaceRoot").also { it.toFile().deleteOnExit() }
    val root = workspaceRoot.resolve("target/name").createDirectories()
    val source1 = workspaceRoot.resolve("target/name/src1.kt").createFile()
    source1.writeText(
      """"
        | package com.example;
        |
        | class A { }
      """.trimMargin(),
    )

    val source2 = workspaceRoot.resolve("target/name/src2.kt").createFile()
    source2.writeText(
      """"
        | package com.example;
        |
        | class B { }
      """.trimMargin(),
    )

    val target =
      createMockTarget(
        name = "//target/name",
        kind = "kt_jvm_library",
        srcs = listOf("//target/name:src1.kt", "//target/name:src2.kt"),
      )

    // when
    val result = target.toBspSourcesItem(workspaceRoot)

    // then
    val expected =
      SourcesItem(
        BuildTargetIdentifier("//target/name"),
        listOf(
          EnhancedSourceItem(
            source1.toUri().toString(),
            SourceItemKind.FILE,
            false,
            data = EnhancedSourceItemData(jvmPackagePrefix = "com.example"),
          ),
          EnhancedSourceItem(
            source2.toUri().toString(),
            SourceItemKind.FILE,
            false,
            data = EnhancedSourceItemData(jvmPackagePrefix = "com.example"),
          ),
        ),
      ).apply {
        roots = listOf(root.toUri().toString())
      }

    result shouldBe expected
    result.sources.map { it as EnhancedSourceItem }.map { it.data } shouldContainExactly
      listOf(
        EnhancedSourceItemData(jvmPackagePrefix = "com.example"),
        EnhancedSourceItemData(jvmPackagePrefix = "com.example"),
      )
  }
}
