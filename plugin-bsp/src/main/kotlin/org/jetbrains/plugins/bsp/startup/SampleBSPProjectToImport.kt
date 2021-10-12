@file:Suppress("MaxLineLength", "MayBeConst")
package org.jetbrains.plugins.bsp.startup

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetCapabilities
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.DependencySourcesItem
import ch.epfl.scala.bsp4j.JvmBuildTarget
import ch.epfl.scala.bsp4j.ResourcesItem
import ch.epfl.scala.bsp4j.SourceItem
import ch.epfl.scala.bsp4j.SourceItemKind
import ch.epfl.scala.bsp4j.SourcesItem

object SampleBSPProjectToImport {

  val appId = BuildTargetIdentifier("//app/src/java/bsp/app:App")
  val appDisplayName = "//app/src/java/bsp/app:App"
  val appBaseDirectory = "file:///Users/marcin.abramowicz/Projects/bsp-sample/app/src/java/bsp/app/"
  val appTags = listOf("application")
  val appLanguageIds = listOf("java")
  val appDependencies = listOf(
    BuildTargetIdentifier("@maven//:com_google_guava_guava"),
    BuildTargetIdentifier("//libA/src/java/bsp/libA:libA"),
    BuildTargetIdentifier("//libB:libB"),
  )
  val appCapabilities = BuildTargetCapabilities(true, false, true, false)
  val appDataKind = "jvm"
  val appData = JvmBuildTarget(
    "file:///private/var/tmp/_bazel_marcin.abramowicz/a2afba1d0e52eedc0574abc5f4ddea23/external/local_jdk/",
    "8"
  )

  val appTarget = BuildTarget(appId, appTags, appLanguageIds, appDependencies, appCapabilities)

  init {
    appTarget.displayName = appDisplayName
    appTarget.baseDirectory = appBaseDirectory
    appTarget.dataKind = appDataKind
    appTarget.data = appData
  }

  val libBId = BuildTargetIdentifier("//libB:libB")
  val libBDisplayName = "//libB:libB"
  val libBBaseDirectory = "file:///Users/marcin.abramowicz/Projects/bsp-sample/libB/"
  val libBTags = listOf<String>()
  val libBLanguageIds = listOf<String>()
  val libBDependencies = listOf(
    BuildTargetIdentifier("//libB/src/java/bsp/libB:libB")
  )
  val libBCapabilities = BuildTargetCapabilities(true, false, false, false)

  val libBTarget = BuildTarget(libBId, libBTags, libBLanguageIds, libBDependencies, libBCapabilities)

  init {
    libBTarget.displayName = libBDisplayName
    libBTarget.baseDirectory = libBBaseDirectory
  }

  val libAid = BuildTargetIdentifier("//libA/src/java/bsp/libA:libA")
  val libADisplayName = "//libA/src/java/bsp/libA:libA"
  val libABaseDirectory = "file:///Users/marcin.abramowicz/Projects/bsp-sample/libA/src/java/bsp/libA/"
  val libATags = listOf("library")
  val libALanguageIds = listOf("java")
  val libADependencies = listOf(
    BuildTargetIdentifier("@maven//:com_google_guava_guava"),
    BuildTargetIdentifier("@maven//:io_vavr_vavr"),
    BuildTargetIdentifier("//libB:libB"),
  )
  val libACapabilities = BuildTargetCapabilities(true, false, false, false)
  val libADataKind = "jvm"
  val libAData = JvmBuildTarget(
    "file:///private/var/tmp/_bazel_marcin.abramowicz/a2afba1d0e52eedc0574abc5f4ddea23/external/local_jdk/",
    "8"
  )

  val libATarget = BuildTarget(libAid, libATags, libALanguageIds, libADependencies, libACapabilities)

  init {
    libATarget.displayName = libADisplayName
    libATarget.baseDirectory = libABaseDirectory
    libATarget.dataKind = libADataKind
    libATarget.data = libAData
  }

  val libBBId = BuildTargetIdentifier("//libB/src/java/bsp/libB:libB")
  val libBBDisplayName = "//libB/src/java/bsp/libB:libB"
  val libBBBaseDirectory = "file:///Users/marcin.abramowicz/Projects/bsp-sample/libB/src/java/bsp/libB/"
  val libBBTags = listOf("library")
  val libBBLanguageIds = listOf("java")
  val libBBDependencies = listOf(
    BuildTargetIdentifier("@maven//:com_google_guava_guava"),
  )
  val libBBCapabilities = BuildTargetCapabilities(true, false, false, false)
  val libBBDataKind = "jvm"
  val libBBData = JvmBuildTarget(
    "file:///private/var/tmp/_bazel_marcin.abramowicz/a2afba1d0e52eedc0574abc5f4ddea23/external/local_jdk/",
    "8"
  )

  val libBBTarget = BuildTarget(libBBId, libBBTags, libBBLanguageIds, libBBDependencies, libBBCapabilities)

  init {
    libBBTarget.displayName = libBBDisplayName
    libBBTarget.baseDirectory = libBBBaseDirectory
    libBBTarget.dataKind = libBBDataKind
    libBBTarget.data = libBBData
  }

  val appSourcesList = listOf(
    SourceItem(
      "file:///Users/marcin.abramowicz/Projects/bsp-sample/app/src/java/bsp/app/App.java",
      SourceItemKind.FILE,
      false
    )
  )
  val appRoots = listOf("file:///Users/marcin.abramowicz/Projects/bsp-sample/app/src/java/")

  val appSources = SourcesItem(appId, appSourcesList)

  init {
    appSources.roots = appRoots
  }

  val libBSourcesList = listOf<SourceItem>()
  val libBRoots = listOf("file:///Users/marcin.abramowicz/Projects/bsp-sample/")

  val libBSources = SourcesItem(libBId, libBSourcesList)

  init {
    libBSources.roots = libBRoots
  }

  val libASourcesList = listOf(
    SourceItem(
      "file:///Users/marcin.abramowicz/Projects/bsp-sample/libA/src/java/bsp/libA/LibA1.java",
      SourceItemKind.FILE,
      false
    )
  )
  val libARoots = listOf("file:///Users/marcin.abramowicz/Projects/bsp-sample/libA/src/java/")

  val libASources = SourcesItem(libAid, libASourcesList)

  init {
    libASources.roots = libARoots
  }

  val libBBSourcesList = listOf(
    SourceItem(
      "file:///Users/marcin.abramowicz/Projects/bsp-sample/libB/src/java/bsp/libB/LibB1.java",
      SourceItemKind.FILE,
      false
    ),
    SourceItem(
      "file:///Users/marcin.abramowicz/Projects/bsp-sample/libB/src/java/bsp/libB/LibB2.java",
      SourceItemKind.FILE, false
    )
  )
  val libBBRoots = listOf("file:///Users/marcin.abramowicz/Projects/bsp-sample/libB/src/java/")

  val libBBSources = SourcesItem(libBBId, libBBSourcesList)

  init {
    libBBSources.roots = libBBRoots
  }

  val libBBResources = ResourcesItem(
    libBBId,
    listOf("file:///Users/marcin.abramowicz/Projects/bsp-sample/libB/src/resources/randomResource.txt")
  )

  val appResources = ResourcesItem(
    appId,
    listOf("file:///Users/marcin.abramowicz/Projects/bsp-sample/app/src/resources/randomResource.txt")
  )

  val appDependenciesSources = DependencySourcesItem(
    appId,
    listOf(
      "file:///private/var/tmp/_bazel_marcin.abramowicz/a2afba1d0e52eedc0574abc5f4ddea23/execroot/bsp_sample/external/maven/v1/https/repo.maven.apache.org/maven2/io/vavr/vavr-match/0.9.0/vavr-match-0.9.0-sources.jar",
      "file:///private/var/tmp/_bazel_marcin.abramowicz/a2afba1d0e52eedc0574abc5f4ddea23/execroot/bsp_sample/external/maven/v1/https/repo.maven.apache.org/maven2/io/vavr/vavr/0.9.0/vavr-0.9.0-sources.jar",
      "file:///private/var/tmp/_bazel_marcin.abramowicz/a2afba1d0e52eedc0574abc5f4ddea23/execroot/bsp_sample/external/maven/v1/https/repo.maven.apache.org/maven2/com/google/errorprone/error_prone_annotations/2.3.2/error_prone_annotations-2.3.2-sources.jar",
      "file:///private/var/tmp/_bazel_marcin.abramowicz/a2afba1d0e52eedc0574abc5f4ddea23/execroot/bsp_sample/external/maven/v1/https/repo.maven.apache.org/maven2/org/codehaus/mojo/animal-sniffer-annotations/1.18/animal-sniffer-annotations-1.18-sources.jar",
      "file:///private/var/tmp/_bazel_marcin.abramowicz/a2afba1d0e52eedc0574abc5f4ddea23/execroot/bsp_sample/external/maven/v1/https/repo.maven.apache.org/maven2/org/checkerframework/checker-qual/2.8.1/checker-qual-2.8.1-sources.jar",
      "file:///private/var/tmp/_bazel_marcin.abramowicz/a2afba1d0e52eedc0574abc5f4ddea23/execroot/bsp_sample/external/maven/v1/https/repo.maven.apache.org/maven2/com/google/j2objc/j2objc-annotations/1.3/j2objc-annotations-1.3-sources.jar",
      "file:///private/var/tmp/_bazel_marcin.abramowicz/a2afba1d0e52eedc0574abc5f4ddea23/execroot/bsp_sample/external/maven/v1/https/repo.maven.apache.org/maven2/com/google/guava/failureaccess/1.0.1/failureaccess-1.0.1-sources.jar",
      "file:///private/var/tmp/_bazel_marcin.abramowicz/a2afba1d0e52eedc0574abc5f4ddea23/execroot/bsp_sample/external/maven/v1/https/repo.maven.apache.org/maven2/com/google/code/findbugs/jsr305/3.0.2/jsr305-3.0.2-sources.jar",
      "file:///private/var/tmp/_bazel_marcin.abramowicz/a2afba1d0e52eedc0574abc5f4ddea23/execroot/bsp_sample/external/maven/v1/https/repo.maven.apache.org/maven2/com/google/guava/guava/28.1-jre/guava-28.1-jre-sources.jar"
    )
  )

  val libADependenciesSources = DependencySourcesItem(
    libAid,
    listOf(
      "file:///private/var/tmp/_bazel_marcin.abramowicz/a2afba1d0e52eedc0574abc5f4ddea23/execroot/bsp_sample/external/maven/v1/https/repo.maven.apache.org/maven2/io/vavr/vavr-match/0.9.0/vavr-match-0.9.0-sources.jar",
      "file:///private/var/tmp/_bazel_marcin.abramowicz/a2afba1d0e52eedc0574abc5f4ddea23/execroot/bsp_sample/external/maven/v1/https/repo.maven.apache.org/maven2/io/vavr/vavr/0.9.0/vavr-0.9.0-sources.jar",
      "file:///private/var/tmp/_bazel_marcin.abramowicz/a2afba1d0e52eedc0574abc5f4ddea23/execroot/bsp_sample/external/maven/v1/https/repo.maven.apache.org/maven2/com/google/errorprone/error_prone_annotations/2.3.2/error_prone_annotations-2.3.2-sources.jar",
      "file:///private/var/tmp/_bazel_marcin.abramowicz/a2afba1d0e52eedc0574abc5f4ddea23/execroot/bsp_sample/external/maven/v1/https/repo.maven.apache.org/maven2/org/codehaus/mojo/animal-sniffer-annotations/1.18/animal-sniffer-annotations-1.18-sources.jar",
      "file:///private/var/tmp/_bazel_marcin.abramowicz/a2afba1d0e52eedc0574abc5f4ddea23/execroot/bsp_sample/external/maven/v1/https/repo.maven.apache.org/maven2/org/checkerframework/checker-qual/2.8.1/checker-qual-2.8.1-sources.jar",
      "file:///private/var/tmp/_bazel_marcin.abramowicz/a2afba1d0e52eedc0574abc5f4ddea23/execroot/bsp_sample/external/maven/v1/https/repo.maven.apache.org/maven2/com/google/j2objc/j2objc-annotations/1.3/j2objc-annotations-1.3-sources.jar",
      "file:///private/var/tmp/_bazel_marcin.abramowicz/a2afba1d0e52eedc0574abc5f4ddea23/execroot/bsp_sample/external/maven/v1/https/repo.maven.apache.org/maven2/com/google/guava/failureaccess/1.0.1/failureaccess-1.0.1-sources.jar",
      "file:///private/var/tmp/_bazel_marcin.abramowicz/a2afba1d0e52eedc0574abc5f4ddea23/execroot/bsp_sample/external/maven/v1/https/repo.maven.apache.org/maven2/com/google/code/findbugs/jsr305/3.0.2/jsr305-3.0.2-sources.jar",
      "file:///private/var/tmp/_bazel_marcin.abramowicz/a2afba1d0e52eedc0574abc5f4ddea23/execroot/bsp_sample/external/maven/v1/https/repo.maven.apache.org/maven2/com/google/guava/guava/28.1-jre/guava-28.1-jre-sources.jar"
    )
  )

  val libBBDependenciesSources = DependencySourcesItem(
    libBBId,
    listOf(
      "file:///private/var/tmp/_bazel_marcin.abramowicz/a2afba1d0e52eedc0574abc5f4ddea23/execroot/bsp_sample/external/maven/v1/https/repo.maven.apache.org/maven2/com/google/errorprone/error_prone_annotations/2.3.2/error_prone_annotations-2.3.2-sources.jar",
      "file:///private/var/tmp/_bazel_marcin.abramowicz/a2afba1d0e52eedc0574abc5f4ddea23/execroot/bsp_sample/external/maven/v1/https/repo.maven.apache.org/maven2/org/codehaus/mojo/animal-sniffer-annotations/1.18/animal-sniffer-annotations-1.18-sources.jar",
      "file:///private/var/tmp/_bazel_marcin.abramowicz/a2afba1d0e52eedc0574abc5f4ddea23/execroot/bsp_sample/external/maven/v1/https/repo.maven.apache.org/maven2/org/checkerframework/checker-qual/2.8.1/checker-qual-2.8.1-sources.jar",
      "file:///private/var/tmp/_bazel_marcin.abramowicz/a2afba1d0e52eedc0574abc5f4ddea23/execroot/bsp_sample/external/maven/v1/https/repo.maven.apache.org/maven2/com/google/j2objc/j2objc-annotations/1.3/j2objc-annotations-1.3-sources.jar",
      "file:///private/var/tmp/_bazel_marcin.abramowicz/a2afba1d0e52eedc0574abc5f4ddea23/execroot/bsp_sample/external/maven/v1/https/repo.maven.apache.org/maven2/com/google/guava/failureaccess/1.0.1/failureaccess-1.0.1-sources.jar",
      "file:///private/var/tmp/_bazel_marcin.abramowicz/a2afba1d0e52eedc0574abc5f4ddea23/execroot/bsp_sample/external/maven/v1/https/repo.maven.apache.org/maven2/com/google/code/findbugs/jsr305/3.0.2/jsr305-3.0.2-sources.jar",
      "file:///private/var/tmp/_bazel_marcin.abramowicz/a2afba1d0e52eedc0574abc5f4ddea23/execroot/bsp_sample/external/maven/v1/https/repo.maven.apache.org/maven2/com/google/guava/guava/28.1-jre/guava-28.1-jre-sources.jar"
    )
  )

  val libBDependenciesSources = DependencySourcesItem(libBId, emptyList())

  val allTargetsIds = listOf(appId, libBId, libAid, libBBId)
}
