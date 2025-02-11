load(
    "@rules_intellij//build_defs:build_defs.bzl",
    "intellij_plugin",
    "intellij_plugin_library",
    "optional_plugin_dep",
    "plugin_deploy_zip",
    "repackaged_files",
    "stamped_plugin_xml",
)
load(
    "@rules_intellij//build_defs:intellij_plugin_debug_target.bzl",
    "intellij_plugin_debug_target",
)
load("//:versions.bzl", "INTELLIJ_BSP_VERSION", "PLATFORM_VERSION", "SINCE_VERSION")

# Changelog file
filegroup(
    name = "changelog",
    srcs = ["CHANGELOG.md"],
)

optional_plugin_dep(
    name = "bsp_with_python",
    module = ["com.intellij.modules.python"],
    plugin_xml = "//plugin-bsp/src:main/xml/bsp-withPython.xml",
)

optional_plugin_dep(
    name = "bsp_with_android",
    module = ["org.jetbrains.android"],
    plugin_xml = "//plugin-bsp/src:main/xml/bsp-withAndroid.xml",
)

optional_plugin_dep(
    name = "bsp_with_go",
    module = ["org.jetbrains.plugins.go"],
    plugin_xml = "//plugin-bsp/src:main/xml/bsp-withGo.xml",
)

optional_plugin_dep(
    name = "bsp_with_cpp",
    module = ["com.intellij.clion"],
    plugin_xml = "//plugin-bsp/src:main/xml/bsp-withCpp.xml",
)

optional_plugin_dep(
    name = "bsp_performance_testing",
    module = ["com.jetbrains.performancePlugin"],
    plugin_xml = "//plugin-bsp/src:main/xml/bsp-performanceTesting.xml",
)

optional_plugin_dep(
    name = "bsp_devkit",
    module = ["DevKit"],
    plugin_xml = "//plugin-bsp/src:main/xml/bsp-withDevKit.xml",
)

optional_plugin_dep(
    name = "bsp_code_with_me",
    module = ["com.jetbrains.codeWithMe"],
    plugin_xml = "//plugin-bsp/src:main/xml/bsp-withCodeWithMe.xml",
)

intellij_plugin_library(
    name = "plugin_library",
    optional_plugin_deps = [
        ":bsp_with_python",
        ":bsp_with_android",
        ":bsp_with_go",
        ":bsp_performance_testing",
        ":bsp_devkit",
        ":bsp_code_with_me",
    ],
    plugin_deps = [
        "com.intellij.modules.platform",
        "com.intellij.java",
        "org.jetbrains.kotlin",
    ],
    plugin_xmls = ["//plugin-bsp/src:main/xml/base.xml"],
    visibility = ["//visibility:public"],
    deps = [
        "//plugin-bsp/jps-compilation/src:jps-compilation",
        "//plugin-bsp/src:intellij-bsp",
        "//plugin-bsp/workspacemodel/src:workspacemodel",
        "//protocol",
    ],
)

stamped_plugin_xml(
    name = "stamped_plugin_xml",
    changelog_file = "CHANGELOG.md",
    description_file = "description.html",
    plugin_id = "org.jetbrains.bsp",
    plugin_name = "Build Server Protocol (BSP)",
    since_build_numbers = {PLATFORM_VERSION: SINCE_VERSION},
    stamp_since_build = True,
    version = INTELLIJ_BSP_VERSION,
)

intellij_plugin(
    name = "intellij-bsp",
    plugin_xml = ":stamped_plugin_xml",
    tags = [],
    visibility = ["//visibility:public"],
    deps = [
        ":plugin_library",
    ],
)

intellij_plugin_debug_target(
    name = "plugin-bsp-debug",
    visibility = ["//visibility:public"],
    deps = [
        ":intellij-bsp",
        ":intellij-bsp_external_jars",
    ],
)

repackaged_files(
    name = "intellij-bsp_jar",
    srcs = [
        ":intellij-bsp",
        ":intellij-bsp_external_jars",
    ],
    prefix = "intellij-bsp/lib",
    visibility = ["//visibility:public"],
)

plugin_deploy_zip(
    name = "intellij-bsp_zip",
    srcs = [
        ":intellij-bsp_jar",
    ],
    visibility = ["//visibility:public"],
    zip_filename = "intellij-bsp.zip",
)
