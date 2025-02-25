package org.jetbrains.bsp.bazel

import org.jetbrains.bsp.bazel.android.BazelBspAndroidProjectTestBase
import org.jetbrains.bsp.protocol.AndroidBuildTarget
import org.jetbrains.bsp.protocol.AndroidTargetType
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetCapabilities
import org.jetbrains.bsp.protocol.BuildTargetIdentifier
import org.jetbrains.bsp.protocol.JvmBuildTarget
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsResult

object BazelBspAndroidProjectTest : BazelBspAndroidProjectTestBase() {
  @JvmStatic
  fun main(args: Array<String>) = executeScenario()

  override val enabledRules: List<String>
    get() = listOf("rules_android")

  override fun expectedWorkspaceBuildTargetsResult(): WorkspaceBuildTargetsResult {
    val javaHome =
      "file://\$BAZEL_OUTPUT_BASE_PATH/external/rules_java~~toolchains~remotejdk17_$javaHomeArchitecture/"
    val jvmBuildTargetData =
      JvmBuildTarget(
        javaHome = javaHome,
        javaVersion = "17",
      )

    val androidJar = "file://\$BAZEL_OUTPUT_BASE_PATH/external/androidsdk/platforms/android-34/android.jar"

    val appAndroidBuildTargetData =
      AndroidBuildTarget(
        androidJar = androidJar,
        androidTargetType = AndroidTargetType.APP,
        manifest = "file://\$WORKSPACE/src/main/AndroidManifest.xml",
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
        manifest = "file://\$WORKSPACE/src/main/java/com/example/myapplication/AndroidManifest.xml",
        manifestOverrides = emptyMap(),
        resourceDirectories = listOf("file://\$WORKSPACE/src/main/java/com/example/myapplication/res/"),
        resourceJavaPackage = null,
        assetsDirectories = emptyList(),
        jvmBuildTarget = jvmBuildTargetData,
      )

    val libTestAndroidBuildTargetData =
      AndroidBuildTarget(
        androidJar = androidJar,
        androidTargetType = AndroidTargetType.TEST,
        manifest = "file://\$WORKSPACE/src/test/java/com/example/myapplication/AndroidManifest.xml",
        manifestOverrides = emptyMap(),
        resourceDirectories = emptyList(),
        resourceJavaPackage = null,
        assetsDirectories = emptyList(),
        jvmBuildTarget = jvmBuildTargetData,
      )

    val appBuildTarget =
      BuildTarget(
        BuildTargetIdentifier("@@//src/main:app"),
        listOf("application"),
        listOf("android", "java"),
        listOf(BuildTargetIdentifier("@@//src/main/java/com/example/myapplication:lib")),
        BuildTargetCapabilities(
          canCompile = true,
          canTest = true,
          canRun = true,
          canDebug = false,
        ),
        displayName = "@@//src/main:app",
        baseDirectory = "file://\$WORKSPACE/src/main/",
        data = appAndroidBuildTargetData,
      )

    val libBuildTarget =
      BuildTarget(
        BuildTargetIdentifier("@@//src/main/java/com/example/myapplication:lib"),
        listOf("library"),
        listOf("android", "java"),
        listOf(BuildTargetIdentifier("@@rules_jvm_external~~maven~maven//:androidx_appcompat_appcompat")),
        BuildTargetCapabilities(
          canCompile = true,
          canTest = false,
          canRun = false,
          canDebug = false,
        ),
        displayName = "@@//src/main/java/com/example/myapplication:lib",
        baseDirectory = "file://\$WORKSPACE/src/main/java/com/example/myapplication/",
        data = libAndroidBuildTargetData,
      )

    val libTestBuildTarget =
      BuildTarget(
        BuildTargetIdentifier("@@//src/test/java/com/example/myapplication:lib_test"),
        listOf("test"),
        listOf("android", "java"),
        listOf(
          BuildTargetIdentifier("@@//src/main/java/com/example/myapplication:lib"),
          BuildTargetIdentifier("@@rules_jvm_external~~maven~maven//:junit_junit"),
          BuildTargetIdentifier("@@rules_jvm_external~~maven~maven//:org_robolectric_robolectric"),
          BuildTargetIdentifier("@@rules_jvm_external~~maven~maven//:org_robolectric_shadows_framework"),
          BuildTargetIdentifier("@@rules_robolectric~//bazel:android-all"),
        ),
        BuildTargetCapabilities(
          canCompile = true,
          canRun = false,
          canTest = true,
        ),
        displayName = "@@//src/test/java/com/example/myapplication:lib_test",
        baseDirectory = "file://\$WORKSPACE/src/test/java/com/example/myapplication/",
        data = libTestAndroidBuildTargetData,
      )

    return WorkspaceBuildTargetsResult(listOf(appBuildTarget, libBuildTarget, libTestBuildTarget))
  }
}
