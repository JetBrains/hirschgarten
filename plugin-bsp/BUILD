load(
    "@rules_intellij//build_defs:build_defs.bzl",
    "intellij_plugin",
    "intellij_plugin_library",
    "stamped_plugin_xml",
    "repackaged_files",
    "plugin_deploy_zip",
    "optional_plugin_dep",
)
load(
    "@rules_intellij//intellij_platform_sdk:build_defs.bzl",
    "select_for_ide",
    "select_from_plugin_api_directory",
)
load(
    "@rules_intellij//build_defs:intellij_plugin_debug_target.bzl",
    "intellij_plugin_debug_target",
)
load("@rules_kotlin//kotlin:core.bzl", "define_kt_toolchain")

load("//:versions.bzl", "INTELLIJ_BSP_VERSION")

# Changelog file
filegroup(
    name = "changelog",
    srcs = ["CHANGELOG.md"]
)

optional_plugin_dep(
    name = "bsp_with_python",
    plugin_xml = "//plugin-bsp/src:main/xml/bsp-withPython.xml",
    module = ["com.intellij.modules.python"],
)

optional_plugin_dep(
    name = "bsp_with_android",
    plugin_xml = "//plugin-bsp/src:main/xml/bsp-withAndroid.xml",
    module = ["org.jetbrains.android"],
)

optional_plugin_dep(
    name = "bsp_performance_testing",
    plugin_xml = "//plugin-bsp/src:main/xml/bsp-performanceTesting.xml",
    module = ["com.jetbrains.performancePlugin"],
)

intellij_plugin_library(
    name = "plugin_library",
    plugin_deps = [
        "com.intellij.modules.platform",
        "com.intellij.java",
        "org.jetbrains.kotlin",
    ],
    optional_plugin_deps = [
        ":bsp_with_python",
        ":bsp_with_android",
        ":bsp_performance_testing",
    ],
    plugin_xmls = ["//plugin-bsp/src:main/xml/base.xml"],
    visibility = ["//visibility:public"],
    deps = [
        "//plugin-bsp/workspacemodel/src:workspacemodel",
        "//plugin-bsp/protocol/src:protocol",
        "//plugin-bsp/src:intellij-bsp",
        "//plugin-bsp/jps-compilation/src:jps-compilation",
    ],
)

stamped_plugin_xml(
    name = "stamped_plugin_xml",
    changelog_file = "CHANGELOG.md",
    description_file = "description.html",
    plugin_id = "org.jetbrains.bsp",
    plugin_name = "Build Server Protocol (BSP)",
    stamp_since_build = True,
    stamp_until_build = True,
    version = INTELLIJ_BSP_VERSION,
)

intellij_plugin(
    name = "intellij-bsp",
    plugin_xml = ":stamped_plugin_xml",
    tags = [],
    deps = [
        ":plugin_library",
    ],
    visibility = ["//visibility:public"],
)

intellij_plugin_debug_target(
    name = "ijwb_bazel_dev",
    deps = [
        ":intellij-bsp_jar",
    ],
    visibility = ["//visibility:public"],
)

repackaged_files(
    name = "intellij-bsp_jar",
    srcs = [":intellij-bsp"],
    prefix = "intellij-bsp/lib",
    visibility = ["//visibility:public"],
)

plugin_deploy_zip(
    name = "intellij-bsp_zip",
    srcs = [
        ":intellij-bsp_jar",
    ],
    zip_filename = "intellij-bsp.zip",
    visibility = ["//visibility:public"],
)