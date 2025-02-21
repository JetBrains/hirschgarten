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
load("//:versions.bzl", "INTELLIJ_BAZEL_VERSION", "PLATFORM_VERSION", "SINCE_VERSION")

optional_plugin_dep(
    name = "bsp_with_python",
    module = ["com.intellij.modules.python"],
    plugin_xml = "//plugin-bazel/src:main/xml/bsp-withPython.xml",
)

optional_plugin_dep(
    name = "bsp_with_android",
    module = ["org.jetbrains.android"],
    plugin_xml = "//plugin-bazel/src:main/xml/bsp-withAndroid.xml",
)

optional_plugin_dep(
    name = "bsp_with_go",
    module = ["org.jetbrains.plugins.go"],
    plugin_xml = "//plugin-bazel/src:main/xml/bsp-withGo.xml",
)

optional_plugin_dep(
    name = "bsp_with_cpp",
    module = ["com.intellij.clion"],
    plugin_xml = "//plugin-bazel/src:main/xml/bsp-withCpp.xml",
)

optional_plugin_dep(
    name = "bsp_ide_starter",
    module = ["com.jetbrains.performancePlugin"],
    plugin_xml = "//plugin-bazel/src:main/xml/bsp-withIdeStarter.xml",
)

optional_plugin_dep(
    name = "bsp_devkit",
    module = ["DevKit"],
    plugin_xml = "//plugin-bazel/src:main/xml/bsp-withDevKit.xml",
)

optional_plugin_dep(
    name = "bsp_code_with_me",
    module = ["com.jetbrains.codeWithMe"],
    plugin_xml = "//plugin-bazel/src:main/xml/bsp-withCodeWithMe.xml",
)

intellij_plugin_library(
    name = "plugin_library",
    optional_plugin_deps = [
        ":bsp_with_python",
        ":bsp_with_android",
        ":bsp_with_go",
        ":bsp_ide_starter",
        ":bsp_devkit",
        ":bsp_code_with_me",
    ],
    plugin_deps = [
        "com.intellij.modules.platform",
        "com.intellij.java",
        "org.jetbrains.kotlin",
    ],
    plugin_xmls = [
        "//plugin-bazel/src:main/xml/base.xml",
    ],
    visibility = ["//visibility:public"],
    deps = [
        "//plugin-bazel/jps-compilation/src:jps-compilation",
        "//plugin-bazel/src:intellij-bazel",
        "//plugin-bazel/src:intellij-bsp",
        "//plugin-bazel/workspacemodel/src:workspacemodel",
        "//protocol",
        "//server/server/src/main/kotlin/org/jetbrains/bsp/bazel:bsp_export",
    ],
)

stamped_plugin_xml(
    name = "stamped_plugin_xml",
    changelog_file = "CHANGELOG.md",
    description_file = "description.html",
    plugin_id = "org.jetbrains.bazel",
    plugin_name = "Bazel (EAP)",
    since_build_numbers = {PLATFORM_VERSION: SINCE_VERSION},
    stamp_since_build = True,
    version = INTELLIJ_BAZEL_VERSION,
)

java_binary(
    name = "intellij-bazel_exclude",
    main_class = "DummySourceForExclude",
    tags = ["no-ide"],
    runtime_deps = [
        "@maven//:io_opentelemetry_opentelemetry_api",
        "@maven//:io_opentelemetry_opentelemetry_exporter_otlp",
        "@maven//:io_opentelemetry_opentelemetry_extension_kotlin",
        "@maven//:io_opentelemetry_opentelemetry_sdk",
        "@maven//:io_opentelemetry_opentelemetry_sdk_trace",
        "@maven//:io_opentelemetry_semconv_opentelemetry_semconv",
        "@maven//:org_apache_velocity_velocity_engine_core",
        "@maven//:org_jetbrains_kotlinx_kotlinx_coroutines_core",
        "@maven//:org_jetbrains_kotlinx_kotlinx_coroutines_core_jvm",
    ],
)

intellij_plugin(
    name = "intellij-bazel",
    deploy_env = [
        "intellij-bazel_exclude",
    ],
    plugin_deps = [],
    plugin_xml = ":stamped_plugin_xml",
    visibility = ["//visibility:public"],
    deps = [
        ":plugin_library",
    ],
)

repackaged_files(
    name = "intellij-bazel_jar",
    srcs = [":intellij-bazel"],
    prefix = "intellij-bazel/lib",
)

plugin_deploy_zip(
    name = "intellij-bazel_zip",
    srcs = [
        ":intellij-bazel_jar",
    ],
    visibility = ["//visibility:public"],
    zip_filename = "intellij-bazel.zip",
)

intellij_plugin_debug_target(
    name = "plugin-bazel-debug",
    deps = [
        ":intellij-bazel_jar",
    ],
)
