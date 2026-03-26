load("@rules_kotlin//kotlin:core.bzl", "define_kt_toolchain", "kt_kotlinc_options")
load("//rules_intellij/intellij_platform_sdk:build_defs.bzl", "select_for_plugin_api")
load(
    "//rules_intellij/build_defs:intellij_plugin_debug_target.bzl",
    "intellij_plugin_zip_and_debug_target",
)

kt_kotlinc_options(
    name = "kotlinc_options",
    include_stdlibs = "none",
    jvm_target = select_for_plugin_api({
        "intellij-2026.1": "21",
    }),
    x_optin = ["org.jetbrains.kotlin.analysis.api.KaPlatformInterface"],
)

define_kt_toolchain(
    name = "kotlin_toolchain",
    api_version = select_for_plugin_api({
        "intellij-2026.1": "2.3",
    }),
    experimental_multiplex_workers = True,
    jvm_target = select_for_plugin_api({
        "intellij-2026.1": "21",
    }),
    kotlinc_options = ":kotlinc_options",
    language_version = select_for_plugin_api({
        "intellij-2026.1": "2.3",
    }),
)

intellij_plugin_zip_and_debug_target(
    name = "plugin-bazel",
    srcs = [
        "//intellij.bazel.bazelisk",
        #"//buildscripts:build",  # Internal JPS target, we don't need to include it
        "//misc/intellij.bazel.bytecodeViewer",
        "//intellij.bazel.commons",
        "//intellij.bazel.connector",
        "//intellij.bazel.core",
        "//intellij.bazel.core.performancePlugin",
        "//misc/intellij.bazel.coverage",
        "//misc/intellij.bazel.coverage.performancePlugin",
        "//misc/intellij.bazel.devkit",
        "//golang/intellij.bazel.golang.common",
        "//intellij.bazel.importer",
        "//java/intellij.bazel.java.common",
        "//java/intellij.bazel.java.common.performancePlugin",
        "//java/intellij.bazel.java.coverage",
        "//java/intellij.bazel.java.junit",
        "//java/intellij.bazel.java.sync",
        "//kotlin/intellij.bazel.kotlin.common",
        "//kotlin/intellij.bazel.kotlin.common.performancePlugin",
        "//kotlin/intellij.bazel.kotlin.coverage",
        "//kotlin/intellij.bazel.kotlin.junit",
        "//kotlin/intellij.bazel.kotlin.k2",
        "//kotlin/intellij.bazel.kotlin.sync",
        "//kotlin/intellij.bazel.kotlin.projectWizard",
        "//intellij.bazel.plugin:bazel-plugin",
        "//intellij.bazel.projectview",
        "//protobuf:intellij.bazel.protobuf",
        "//misc/intellij.bazel.protoedit",
        "//python/intellij.bazel.python.common",
        "//python/intellij.bazel.python.common.performancePlugin",
        "//misc/intellij.bazel.remoteDevelopment",
        "//misc/intellij.bazel.terminal",
        "//intellij.bazel.resource",  # Derived from plugin-content.yaml
        "//misc/intellij.bazel.devkit.monorepo",  # Derived from plugin-content.yaml
    ],
    visibility = ["//visibility:public"],
    zip_filename = "plugin-bazel.zip",
)
