//@file:Suppress("MaxLineLength", "LongMethod")
//
//package org.jetbrains.magicmetamodel.impl.workspacemodel.impl
//
//import ch.epfl.scala.bsp4j.BuildTarget
//import ch.epfl.scala.bsp4j.BuildTargetCapabilities
//import ch.epfl.scala.bsp4j.BuildTargetIdentifier
//import ch.epfl.scala.bsp4j.DependencySourcesItem
//import ch.epfl.scala.bsp4j.ResourcesItem
//import ch.epfl.scala.bsp4j.SourceItem
//import ch.epfl.scala.bsp4j.SourceItemKind
//import ch.epfl.scala.bsp4j.SourcesItem
//import com.intellij.openapi.command.WriteCommandAction
//import org.jetbrains.magicmetamodel.impl.workspacemodel.ModuleDetails
//import org.jetbrains.workspace.model.test.framework.WorkspaceModelBaseTest
//import org.junit.jupiter.api.DisplayName
//import org.junit.jupiter.api.Test
//
//// TODO load modules test
//@DisplayName("workspaceModelUpdaterImpl.loadModule(moduleDetails) tests")
//class WorkspaceModelUpdaterImplLoadModuleTest : WorkspaceModelBaseTest() {
//
//  @Test
//  fun `should add java module`() {
//    // given
//    val moduleTargetId = BuildTargetIdentifier("//test-module")
//
//    val moduleTargetLanguageIds = listOf("java")
//    val moduleTargetDependencies = listOf(
//      BuildTargetIdentifier("@maven//:com_google_guava_guava"),
//      BuildTargetIdentifier("//another-module"),
//    )
//    val moduleTargetCapabilities = BuildTargetCapabilities()
//    val moduleTarget = BuildTarget(
//      moduleTargetId,
//      emptyList(),
//      moduleTargetLanguageIds,
//      moduleTargetDependencies,
//      moduleTargetCapabilities
//    )
//
//    val moduleSourcesList = listOf(
//      SourceItem(
//        "file:///Users/marcin.abramowicz/Projects/bsp-sample/app/src/java/bsp/app/App.java",
//        SourceItemKind.FILE,
//        false
//      )
//    )
//    val moduleSourceRoots = listOf("file:///Users/marcin.abramowicz/Projects/bsp-sample/app/src/java/")
//    val moduleSources = SourcesItem(moduleTargetId, moduleSourcesList)
//    moduleSources.roots = moduleSourceRoots
//
//    val moduleResources = ResourcesItem(
//      moduleTargetId,
//      listOf("file:///Users/marcin.abramowicz/Projects/bsp-sample/app/src/resources/randomResource.txt")
//    )
//
//    val moduleDependenciesSources = DependencySourcesItem(
//      moduleTargetId,
//      listOf(
//        "file:///private/var/tmp/_bazel_marcin.abramowicz/a2afba1d0e52eedc0574abc5f4ddea23/execroot/bsp_sample/external/maven/v1/https/repo.maven.apache.org/maven2/io/vavr/vavr-match/0.9.0/vavr-match-0.9.0-sources.jar",
//        "file:///private/var/tmp/_bazel_marcin.abramowicz/a2afba1d0e52eedc0574abc5f4ddea23/execroot/bsp_sample/external/maven/v1/https/repo.maven.apache.org/maven2/io/vavr/vavr/0.9.0/vavr-0.9.0-sources.jar",
//        "file:///private/var/tmp/_bazel_marcin.abramowicz/a2afba1d0e52eedc0574abc5f4ddea23/execroot/bsp_sample/external/maven/v1/https/repo.maven.apache.org/maven2/com/google/errorprone/error_prone_annotations/2.3.2/error_prone_annotations-2.3.2-sources.jar",
//        "file:///private/var/tmp/_bazel_marcin.abramowicz/a2afba1d0e52eedc0574abc5f4ddea23/execroot/bsp_sample/external/maven/v1/https/repo.maven.apache.org/maven2/org/codehaus/mojo/animal-sniffer-annotations/1.18/animal-sniffer-annotations-1.18-sources.jar",
//        "file:///private/var/tmp/_bazel_marcin.abramowicz/a2afba1d0e52eedc0574abc5f4ddea23/execroot/bsp_sample/external/maven/v1/https/repo.maven.apache.org/maven2/org/checkerframework/checker-qual/2.8.1/checker-qual-2.8.1-sources.jar",
//        "file:///private/var/tmp/_bazel_marcin.abramowicz/a2afba1d0e52eedc0574abc5f4ddea23/execroot/bsp_sample/external/maven/v1/https/repo.maven.apache.org/maven2/com/google/j2objc/j2objc-annotations/1.3/j2objc-annotations-1.3-sources.jar",
//        "file:///private/var/tmp/_bazel_marcin.abramowicz/a2afba1d0e52eedc0574abc5f4ddea23/execroot/bsp_sample/external/maven/v1/https/repo.maven.apache.org/maven2/com/google/guava/failureaccess/1.0.1/failureaccess-1.0.1-sources.jar",
//        "file:///private/var/tmp/_bazel_marcin.abramowicz/a2afba1d0e52eedc0574abc5f4ddea23/execroot/bsp_sample/external/maven/v1/https/repo.maven.apache.org/maven2/com/google/code/findbugs/jsr305/3.0.2/jsr305-3.0.2-sources.jar",
//        "file:///private/var/tmp/_bazel_marcin.abramowicz/a2afba1d0e52eedc0574abc5f4ddea23/execroot/bsp_sample/external/maven/v1/https/repo.maven.apache.org/maven2/com/google/guava/guava/28.1-jre/guava-28.1-jre-sources.jar"
//      )
//    )
//
//    val moduleDetails = ModuleDetails(
//      target = moduleTarget,
//      allTargetsIds = listOf(moduleTargetId),
//      sources = listOf(moduleSources),
//      resources = listOf(moduleResources),
//      dependenciesSources = listOf(moduleDependenciesSources),
////      javacOptions = listOf()
//    )
//
//    // when
//    val workspaceModelUpdater = WorkspaceModelUpdaterImpl(workspaceEntityStorageBuilder, virtualFileUrlManager)
//
//    WriteCommandAction.runWriteCommandAction(project) {
//      workspaceModelUpdater.loadModule(moduleDetails)
//    }
//
//    // then
//    // TODO
//    // val expectedDependency = ModuleDependencyItem.Exportable.ModuleDependency(
//    //  module = ModuleId("//another-module"),
//    //  exported = true,
//    //  scope = ModuleDependencyItem.DependencyScope.COMPILE,
//    //  productionOnTest = false
//    // )
//
//    // val expectedModuleEntity = ExpectedModuleEntity(
//    //  moduleEntity = ModuleEntity(
//    //    name = moduleTargetId.uri,
//    //    type = "JAVA_MODULE",
//    //    dependencies = listOf(expectedDependency),
//    //  )
//    // )
//
//    // TODO test libraries
//    // loadedEntries(ModuleEntity::class.java) shouldContainExactlyInAnyOrder listOf(expectedModuleEntity)
//  }
//}
