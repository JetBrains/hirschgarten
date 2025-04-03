package org.jetbrains.bazel

import org.jetbrains.bazel.android.BazelBspAndroidProjectTestBase
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.AndroidBuildTarget
import org.jetbrains.bsp.protocol.AndroidTargetType
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetCapabilities
import org.jetbrains.bsp.protocol.JvmBuildTarget
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsResult
import kotlin.io.path.Path

object BazelBspAndroidProjectTest : BazelBspAndroidProjectTestBase() {
  @JvmStatic
  fun main(args: Array<String>) = executeScenario()

  override val enabledRules: List<String>
    get() = listOf("rules_android")

  override fun expectedWorkspaceBuildTargetsResult(): WorkspaceBuildTargetsResult {
    val javaHome =
      Path("\$BAZEL_OUTPUT_BASE_PATH/external/rules_java~~toolchains~remotejdk17_$javaHomeArchitecture/")
    val jvmBuildTargetData =
      JvmBuildTarget(
        javaHome = javaHome,
        javaVersion = "17",
      )

    val androidJar = Path("\$BAZEL_OUTPUT_BASE_PATH/external/androidsdk/platforms/android-34/android.jar")

    val appAndroidBuildTargetData =
      AndroidBuildTarget(
        androidJar = androidJar,
        androidTargetType = AndroidTargetType.APP,
        manifest = Path("\$WORKSPACE/src/main/AndroidManifest.xml"),
        manifestOverrides = emptyMap(),
        resourceDirectories = emptyList(),
        resourceJavaPackage = null,
        assetsDirectories = emptyList(),
        jvmBuildTarget = jvmBuildTargetData,
      )

    val libAndroidBuildTargetData =
      AndroidBuildTarget(
        androidJar = androidJar,
        androidTargetType = AndroidTargetType.LIBRARY,
        manifest = Path("\$WORKSPACE/src/main/java/com/example/myapplication/AndroidManifest.xml"),
        manifestOverrides = emptyMap(),
        resourceDirectories = listOf(Path("\$WORKSPACE/src/main/java/com/example/myapplication/res/")),
        resourceJavaPackage = null,
        assetsDirectories = emptyList(),
        jvmBuildTarget = jvmBuildTargetData,
      )

    val libTestAndroidBuildTargetData =
      AndroidBuildTarget(
        androidJar = androidJar,
        androidTargetType = AndroidTargetType.TEST,
        manifest = Path("\$WORKSPACE/src/test/java/com/example/myapplication/AndroidManifest.xml"),
        manifestOverrides = emptyMap(),
        resourceDirectories = emptyList(),
        resourceJavaPackage = null,
        assetsDirectories = emptyList(),
        jvmBuildTarget = jvmBuildTargetData,
      )

    val appBuildTarget =
      BuildTarget(
        Label.parse("@@//src/main:app"),
        listOf("application"),
        listOf("android", "java"),
        listOf(Label.parse("@@//src/main/java/com/example/myapplication:lib")),
        BuildTargetCapabilities(
          canCompile = true,
          canTest = true,
          canRun = true,
        ),
        baseDirectory = Path("\$WORKSPACE/src/main/"),
        data = appAndroidBuildTargetData,
        sources = emptyList(),
        resources = listOf(Path("\$WORKSPACE/src/main/AndroidManifest.xml")),
      )

    val libBuildTarget =
      BuildTarget(
        Label.parse("@@//src/main/java/com/example/myapplication:lib"),
        listOf("library"),
        listOf("android", "java"),
        listOf(Label.parse("@@rules_jvm_external~~maven~maven//:androidx_appcompat_appcompat")),
        BuildTargetCapabilities(
          canCompile = true,
          canTest = false,
          canRun = false,
        ),
        baseDirectory = Path("\$WORKSPACE/src/main/java/com/example/myapplication/"),
        data = libAndroidBuildTargetData,
        sources = emptyList(),
        resources = listOf(Path("\$WORKSPACE/src/main/java/com/example/myapplication/AndroidManifest.xml")),
      )

    val libTestBuildTarget =
      BuildTarget(
        Label.parse("@@//src/test/java/com/example/myapplication:lib_test"),
        listOf("test"),
        listOf("android", "java"),
        listOf(
          Label.parse("@@//src/main/java/com/example/myapplication:lib"),
          Label.parse("@@rules_jvm_external~~maven~maven//:junit_junit"),
          Label.parse("@@rules_jvm_external~~maven~maven//:org_robolectric_robolectric"),
          Label.parse("@@rules_jvm_external~~maven~maven//:org_robolectric_shadows_framework"),
          Label.parse("@@rules_robolectric~//bazel:android-all"),
        ),
        BuildTargetCapabilities(
          canCompile = true,
          canRun = false,
          canTest = true,
        ),
        baseDirectory = Path("\$WORKSPACE/src/test/java/com/example/myapplication/"),
        data = libTestAndroidBuildTargetData,
        sources = emptyList(),
        resources = listOf(Path("\$WORKSPACE/src/test/java/com/example/myapplication/AndroidManifest.xml")),
      )

    return WorkspaceBuildTargetsResult(listOf(appBuildTarget, libBuildTarget, libTestBuildTarget))
  }
}
