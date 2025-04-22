load("@rules_java//java:java_binary.bzl", "java_binary")
load("//:versions.bzl", "INTELLIJ_BAZEL_VERSION", "PLATFORM_VERSION", "SINCE_VERSION")
load(
    "//rules_intellij/build_defs:build_defs.bzl",
    "intellij_plugin",
    "intellij_plugin_library",
    "plugin_deploy_zip",
    "repackaged_files",
    "stamped_plugin_xml",
)
load(
    "//rules_intellij/build_defs:intellij_plugin_debug_target.bzl",
    "intellij_plugin_debug_target",
)

intellij_plugin_library(
    name = "plugin_library",
    plugin_xmls = [
        "//plugin-bazel/src:main/jps-resources/META-INF/plugin.xml",
    ],
    visibility = ["//visibility:public"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bsp/protocol",
        "//plugin-bazel/src:plugin-bazel-lib",
        "//server/server/src/main/kotlin/org/jetbrains/bazel:bsp_export",
    ],
)

stamped_plugin_xml(
    name = "stamped_plugin_xml",
    changelog_file = "CHANGELOG.md",
    since_build_numbers = {PLATFORM_VERSION: SINCE_VERSION},
    stamp_since_build = True,
    version = INTELLIJ_BAZEL_VERSION,
)

java_binary(
    name = "plugin-bazel_exclude",
    main_class = "DummySourceForExclude",
    tags = ["no-ide"],
    runtime_deps = [
        "@maven//:org_apache_velocity_velocity_engine_core",
        "@maven//:org_jetbrains_kotlinx_kotlinx_coroutines_core",
        "@maven//:org_jetbrains_kotlinx_kotlinx_coroutines_core_jvm",
    ],
)

intellij_plugin(
    name = "plugin-bazel",
    deploy_env = [
        ":plugin-bazel_exclude",
    ],
    plugin_deps = [],
    plugin_xml = ":stamped_plugin_xml",
    visibility = ["//visibility:public"],
    deps = [
        ":plugin_library",
    ],
)

repackaged_files(
    name = "plugin-bazel_jar",
    srcs = [
        ":plugin-bazel",
    ],
    prefix = "plugin-bazel/lib",
)

plugin_deploy_zip(
    name = "plugin-bazel_zip",
    srcs = [
        ":plugin-bazel_jar",
    ],
    visibility = ["//visibility:public"],
    zip_filename = "plugin-bazel.zip",
)

intellij_plugin_debug_target(
    name = "plugin-bazel-debug",
    deps = [
        ":plugin-bazel_jar",
    ],
)
