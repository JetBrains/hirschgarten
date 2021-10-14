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

  // targets

  // //libB/src/java/bsp/libB:libB-2
  val libBB2Id = BuildTargetIdentifier("//libB/src/java/bsp/libB:libB-2")
  val libBB2DisplayName = "//libB/src/java/bsp/libB:libB-2"
  val libBB2BaseDirectory = "file:///Users/marcin.abramowicz/Projects/bsp-sample/libB/src/java/bsp/libB/"
  val libBB2Tags = listOf("library")
  val libBB2LanguageIds = listOf("java")
  val libBB2Dependencies = listOf(
    BuildTargetIdentifier("@maven//:com_google_guava_guava")
  )
  val libBB2Capabilities = BuildTargetCapabilities(true, false, false, false)
  val libBB2DataKind = "jvm"
  val libBB2Data = JvmBuildTarget(
    "file:///private/var/tmp/_bazel_marcin.abramowicz/a2afba1d0e52eedc0574abc5f4ddea23/external/local_jdk/",
    "8"
  )

  val libBB2Target = BuildTarget(libBB2Id, libBB2Tags, libBB2LanguageIds, libBB2Dependencies, libBB2Capabilities)

  init {
    libBB2Target.displayName = libBB2DisplayName
    libBB2Target.baseDirectory = libBB2BaseDirectory
    libBB2Target.dataKind = libBB2DataKind
    libBB2Target.data = libBB2Data
  }

  // //app/src/java/bsp/app:another-app
  val anotherAppId = BuildTargetIdentifier("//app/src/java/bsp/app:another-app")
  val anotherAppDisplayName = "//app/src/java/bsp/app:another-app"
  val anotherAppBaseDirectory = "file:///Users/marcin.abramowicz/Projects/bsp-sample/app/src/java/bsp/app/"
  val anotherAppTags = listOf("application")
  val anotherAppLanguageIds = listOf("java")
  val anotherAppDependencies = listOf(
    BuildTargetIdentifier("@maven//:com_google_guava_guava"),
    BuildTargetIdentifier("//libB/src/java/bsp/libB:libB-2"),
    BuildTargetIdentifier("//libA2/src/java/bsp/libA:libA"),
  )
  val anotherAppCapabilities = BuildTargetCapabilities(true, false, true, false)
  val anotherAppDataKind = "jvm"
  val anotherAppData = JvmBuildTarget(
    "file:///private/var/tmp/_bazel_marcin.abramowicz/a2afba1d0e52eedc0574abc5f4ddea23/external/local_jdk/",
    "8"
  )
  val anotherAppTarget =
    BuildTarget(anotherAppId, anotherAppTags, anotherAppLanguageIds, anotherAppDependencies, anotherAppCapabilities)

  init {
    anotherAppTarget.displayName = anotherAppDisplayName
    anotherAppTarget.baseDirectory = anotherAppBaseDirectory
    anotherAppTarget.dataKind = anotherAppDataKind
    anotherAppTarget.data = anotherAppData
  }

  // //app/src/java/bsp/app:app
  val appId = BuildTargetIdentifier("//app/src/java/bsp/app:app")
  val appDisplayName = "//app/src/java/bsp/app:app"
  val appBaseDirectory = "file:///Users/marcin.abramowicz/Projects/bsp-sample/app/src/java/bsp/app/"
  val appTags = listOf("application")
  val appLanguageIds = listOf("java")
  val appDependencies = listOf(
    BuildTargetIdentifier("@maven//:com_google_guava_guava"),
    BuildTargetIdentifier("//libA1/src/java/bsp/libA:libA"),
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

  // //libB/src/java/bsp/libB:libB
  val libBBId = BuildTargetIdentifier("//libB/src/java/bsp/libB:libB")
  val libBBDisplayName = "//libB/src/java/bsp/libB:libB"
  val libBBBaseDirectory = "file:///Users/marcin.abramowicz/Projects/bsp-sample/libB/src/java/bsp/libB/"
  val libBBTags = listOf("library")
  val libBBLanguageIds = listOf("java")
  val libBBDependencies = listOf(
    BuildTargetIdentifier("@maven//:com_google_guava_guava")
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

  // //libB:libB
  val libBId = BuildTargetIdentifier("//libB:libB")
  val libBDisplayName = "//libB:libB"
  val libBBaseDirectory = "file:///Users/marcin.abramowicz/Projects/bsp-sample/libB/"
  val libBTags = listOf<String>()
  val libBLanguageIds = listOf<String>()
  val libBDependencies = listOf(
    BuildTargetIdentifier("//libB/src/java/bsp/libB:libB")
  )
  val libBCapabilities = BuildTargetCapabilities(true, false, false, false)
  val libBDataKind = null
  val libBData = null

  val libBTarget = BuildTarget(libBId, libBTags, libBLanguageIds, libBDependencies, libBCapabilities)

  init {
    libBTarget.displayName = libBDisplayName
    libBTarget.baseDirectory = libBBaseDirectory
    libBTarget.dataKind = libBDataKind
    libBTarget.data = libBData
  }

  // //libA1/src/java/bsp/libA:libA
  val libA1Id = BuildTargetIdentifier("//libA1/src/java/bsp/libA:libA")
  val libA1DisplayName = "//libA1/src/java/bsp/libA:libA"
  val libA1BaseDirectory = "file:///Users/marcin.abramowicz/Projects/bsp-sample/libA1/src/java/bsp/libA/"
  val libA1Tags = listOf("library")
  val libA1LanguageIds = listOf("java")
  val libA1Dependencies = emptyList<BuildTargetIdentifier>()
  val libA1Capabilities = BuildTargetCapabilities(true, false, false, false)
  val libA1DataKind = "jvm"
  val libA1Data = JvmBuildTarget(
    "file:///private/var/tmp/_bazel_marcin.abramowicz/a2afba1d0e52eedc0574abc5f4ddea23/external/local_jdk/",
    "8"
  )

  val libA1Target = BuildTarget(libA1Id, libA1Tags, libA1LanguageIds, libA1Dependencies, libA1Capabilities)

  init {
    libA1Target.displayName = libA1DisplayName
    libA1Target.baseDirectory = libA1BaseDirectory
    libA1Target.dataKind = libA1DataKind
    libA1Target.data = libA1Data
  }

  // //libA2/src/java/bsp/libA:libA
  val libA2Id = BuildTargetIdentifier("//libA2/src/java/bsp/libA:libA")
  val libA2DisplayName = "//libA2/src/java/bsp/libA:libA"
  val libA2BaseDirectory = "file:///Users/marcin.abramowicz/Projects/bsp-sample/libA2/src/java/bsp/libA/"
  val libA2Tags = listOf("library")
  val libA2LanguageIds = listOf("java")
  val libA2Dependencies = listOf(
    BuildTargetIdentifier("@maven//:com_google_guava_guava"),
    BuildTargetIdentifier("@maven//:io_vavr_vavr"),
    BuildTargetIdentifier("//libB:libB"),
  )
  val libA2Capabilities = BuildTargetCapabilities(true, false, false, false)
  val libA2DataKind = "jvm"
  val libA2Data = JvmBuildTarget(
    "file:///private/var/tmp/_bazel_marcin.abramowicz/a2afba1d0e52eedc0574abc5f4ddea23/external/local_jdk/",
    "8"
  )

  val libA2Target = BuildTarget(libA2Id, libA2Tags, libA2LanguageIds, libA2Dependencies, libA2Capabilities)

  init {
    libA2Target.displayName = libA2DisplayName
    libA2Target.baseDirectory = libA2BaseDirectory
    libA2Target.dataKind = libA2DataKind
    libA2Target.data = libA2Data
  }

  // sources

  // //libB/src/java/bsp/libB:libB-2
  val libBB2SourcesList = listOf(
    SourceItem(
      "file:///Users/marcin.abramowicz/Projects/bsp-sample/libB/src/java/bsp/libB/LibB2.java",
      SourceItemKind.FILE, false
    )
  )
  val libBB2SourcesRoots = listOf("file:///Users/marcin.abramowicz/Projects/bsp-sample/libB/src/java/")

  val libBB2Sources = SourcesItem(libBB2Id, libBB2SourcesList)

  init {
    libBB2Sources.roots = libBB2SourcesRoots
  }

  // //app/src/java/bsp/app:another-app
  val anotherAppSourcesList = listOf(
    SourceItem(
      "file:///Users/marcin.abramowicz/Projects/bsp-sample/app/src/java/bsp/app/AnotherApp.java",
      SourceItemKind.FILE, false
    ),
    SourceItem(
      "file:///Users/marcin.abramowicz/Projects/bsp-sample/app/src/java/bsp/app/App.java",
      SourceItemKind.FILE, false
    )
  )
  val anotherAppSourcesRoots = listOf("file:///Users/marcin.abramowicz/Projects/bsp-sample/app/src/java/")

  val anotherAppSources = SourcesItem(anotherAppId, anotherAppSourcesList)

  init {
    anotherAppSources.roots = anotherAppSourcesRoots
  }

  // //app/src/java/bsp/app:app
  val appSourcesList = listOf(
    SourceItem(
      "file:///Users/marcin.abramowicz/Projects/bsp-sample/app/src/java/bsp/app/App.java",
      SourceItemKind.FILE, false
    )
  )
  val appSourcesRoots = listOf("file:///Users/marcin.abramowicz/Projects/bsp-sample/app/src/java/")

  val appSources = SourcesItem(appId, appSourcesList)

  init {
    appSources.roots = appSourcesRoots
  }

  // //libB/src/java/bsp/libB:libB
  val libBBSourcesList = listOf(
    SourceItem(
      "file:///Users/marcin.abramowicz/Projects/bsp-sample/libB/src/java/bsp/libB/LibB1.java",
      SourceItemKind.FILE, false
    ),
    SourceItem(
      "file:///Users/marcin.abramowicz/Projects/bsp-sample/libB/src/java/bsp/libB/LibB2.java",
      SourceItemKind.FILE, false
    ),
    SourceItem(
      "file:///Users/marcin.abramowicz/Projects/bsp-sample/libB/src/java/bsp/libB/LibB3.java",
      SourceItemKind.FILE, false
    )
  )
  val libBBSourcesRoots = listOf("file:///Users/marcin.abramowicz/Projects/bsp-sample/libB/src/java/")

  val libBBSources = SourcesItem(libBBId, libBBSourcesList)

  init {
    libBBSources.roots = libBBSourcesRoots
  }

  // //libA1/src/java/bsp/libA:libA
  val libA1SourcesList = listOf(
    SourceItem(
      "file:///Users/marcin.abramowicz/Projects/bsp-sample/libA1/src/java/bsp/libA/LibA.java",
      SourceItemKind.FILE, false
    )
  )
  val libA1SourcesRoots = listOf("file:///Users/marcin.abramowicz/Projects/bsp-sample/libA1/src/java/")

  val libA1Sources = SourcesItem(libA1Id, libA1SourcesList)

  init {
    libA1Sources.roots = libA1SourcesRoots
  }

  // //libA2/src/java/bsp/libA:libA
  val libA2SourcesList = listOf(
    SourceItem(
      "file:///Users/marcin.abramowicz/Projects/bsp-sample/libA2/src/java/bsp/libA/LibA.java",
      SourceItemKind.FILE, false
    )
  )
  val libA2SourcesRoots = listOf("file:///Users/marcin.abramowicz/Projects/bsp-sample/libA2/src/java/")

  val libA2Sources = SourcesItem(libA2Id, libA2SourcesList)

  init {
    libA2Sources.roots = libA2SourcesRoots
  }

  // //libB
  val libBSourcesList = emptyList<SourceItem>()
  val libBSourcesRoots = listOf("file:///Users/marcin.abramowicz/Projects/bsp-sample/")

  val libBSources = SourcesItem(libBId, libBSourcesList)

  init {
    libBSources.roots = libBSourcesRoots
  }

  // resources

  // //libB/src/java/bsp/libB:libB
  val libBBResources = ResourcesItem(
    libBBId,
    listOf("file:///Users/marcin.abramowicz/Projects/bsp-sample/libB/src/resources/randomResource.txt")
  )

  // //app/src/java/bsp/app:App
  val appResources = ResourcesItem(
    appId,
    listOf("file:///Users/marcin.abramowicz/Projects/bsp-sample/app/src/resources/randomResource.txt")
  )

  val anotherAppResources = ResourcesItem(
    anotherAppId,
    listOf("file:///Users/marcin.abramowicz/Projects/bsp-sample/app/src/resources/randomResource.txt")
  )

  // dependency sources

  // //app/src/java/bsp/app:app
  val appDependenciesSources = DependencySourcesItem(
    appId,
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

  // //libA/src/java/bsp/libA:libA
  val libA2DependenciesSources = DependencySourcesItem(
    libA2Id,
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

  // //libB/src/java/bsp/libB:libB
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

  // //libB:libB
  val libBDependenciesSources = DependencySourcesItem(libBId, emptyList())

  // libA1
  val libA1DependenciesSources = DependencySourcesItem(libA1Id, emptyList())

  // //app/src/java/bsp/app:another-app
  val anotherAppDependenciesSources = DependencySourcesItem(
    anotherAppId,
    listOf(
      "file:///private/var/tmp/_bazel_marcin.abramowicz/a2afba1d0e52eedc0574abc5f4ddea23/execroot/bsp_sample/external/maven/v1/https/repo.maven.apache.org/maven2/org/codehaus/mojo/animal-sniffer-annotations/1.18/animal-sniffer-annotations-1.18-sources.jar",
      "file:///private/var/tmp/_bazel_marcin.abramowicz/a2afba1d0e52eedc0574abc5f4ddea23/execroot/bsp_sample/external/maven/v1/https/repo.maven.apache.org/maven2/com/google/code/findbugs/jsr305/3.0.2/jsr305-3.0.2-sources.jar",
      "file:///private/var/tmp/_bazel_marcin.abramowicz/a2afba1d0e52eedc0574abc5f4ddea23/execroot/bsp_sample/external/maven/v1/https/repo.maven.apache.org/maven2/org/checkerframework/checker-qual/2.8.1/checker-qual-2.8.1-sources.jar",
      "file:///private/var/tmp/_bazel_marcin.abramowicz/a2afba1d0e52eedc0574abc5f4ddea23/execroot/bsp_sample/external/maven/v1/https/repo.maven.apache.org/maven2/io/vavr/vavr-match/0.9.0/vavr-match-0.9.0-sources.jar",
      "file:///private/var/tmp/_bazel_marcin.abramowicz/a2afba1d0e52eedc0574abc5f4ddea23/execroot/bsp_sample/external/maven/v1/https/repo.maven.apache.org/maven2/com/google/errorprone/error_prone_annotations/2.3.2/error_prone_annotations-2.3.2-sources.jar",
      "file:///private/var/tmp/_bazel_marcin.abramowicz/a2afba1d0e52eedc0574abc5f4ddea23/execroot/bsp_sample/external/maven/v1/https/repo.maven.apache.org/maven2/com/google/j2objc/j2objc-annotations/1.3/j2objc-annotations-1.3-sources.jar",
      "file:///private/var/tmp/_bazel_marcin.abramowicz/a2afba1d0e52eedc0574abc5f4ddea23/execroot/bsp_sample/external/maven/v1/https/repo.maven.apache.org/maven2/com/google/guava/failureaccess/1.0.1/failureaccess-1.0.1-sources.jar",
      "file:///private/var/tmp/_bazel_marcin.abramowicz/a2afba1d0e52eedc0574abc5f4ddea23/execroot/bsp_sample/external/maven/v1/https/repo.maven.apache.org/maven2/io/vavr/vavr/0.9.0/vavr-0.9.0-sources.jar",
      "file:///private/var/tmp/_bazel_marcin.abramowicz/a2afba1d0e52eedc0574abc5f4ddea23/execroot/bsp_sample/external/maven/v1/https/repo.maven.apache.org/maven2/com/google/guava/guava/28.1-jre/guava-28.1-jre-sources.jar"
    )
  )

  val libBB2DependenciesSources = DependencySourcesItem(
    libBB2Id,
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

  val allTargetsIds = listOf(appId, libBId, libA2Id, libBB2Id, libBBId, libA1Id, anotherAppId)
}
