load(
    "@rules_intellij//build_defs:build_defs.bzl",
    "intellij_plugin",
    "intellij_plugin_library",
    "plugin_deploy_zip",
    "repackaged_files",
    "stamped_plugin_xml",
)
load(
    "@rules_intellij//build_defs:intellij_plugin_debug_target.bzl",
    "intellij_plugin_debug_target",
)
load("@rules_java//java:java_single_jar.bzl", "java_single_jar")
load("@rules_kotlin//kotlin:core.bzl", "define_kt_toolchain")
load("//:versions.bzl", "INTELLIJ_BAZEL_VERSION", "PLATFORM_VERSION", "SINCE_VERSION")

define_kt_toolchain(
    name = "kotlin_toolchain",
    api_version = "1.9",  # "1.1", "1.2", "1.3", "1.4", "1.5" "1.6", "1.7", "1.8", or "1.9"
    jvm_target = "17",  # "1.6", "1.8", "9", "10", "11", "12", "13", "15", "16", "17", "18", "19", "20" or "21"
    language_version = "1.9",  # "1.1", "1.2", "1.3", "1.4", "1.5" "1.6", "1.7", "1.8", or "1.9"
)

intellij_plugin_library(
    name = "plugin_library",
    optional_plugin_deps = [],
    plugin_deps = ["com.intellij.modules.platform"],
    plugin_xmls = [
        "//plugin-bazel/src:main/resources/META-INF/base.xml",
        # bazel plugin depends on bsp plugin, which already adds performance plugin as optional plugin dep
        "//plugin-bazel/src:test/resources/META-INF/bazel-ide-starter-test.xml",
    ],
    visibility = ["//visibility:public"],
    deps = [
        "//plugin-bazel/src:intellij-bazel",
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
        "//protocol",
        "@maven//:ch_epfl_scala_bsp4j",
        "@maven//:org_jetbrains_kotlinx_kotlinx_coroutines_core",
        "@maven//:org_jetbrains_kotlinx_kotlinx_coroutines_core_jvm",
    ],
)

intellij_plugin(
    name = "intellij-bazel",
    deploy_env = [
        "intellij-bazel_exclude",
    ],
    plugin_deps = ["org.jetbrains.bsp"],
    plugin_xml = ":stamped_plugin_xml",
    visibility = ["//visibility:public"],
    deps = [
        ":plugin_library",
    ],
)

java_binary(
    name = "intellij-bazel_exclude_debug",
    main_class = "DummySourceForExclude",
    tags = ["no-ide"],
    runtime_deps = [
        "@maven//:io_opentelemetry_opentelemetry_api",
        "@maven//:io_opentelemetry_opentelemetry_exporter_otlp",
        "@maven//:io_opentelemetry_opentelemetry_extension_kotlin",
        "@maven//:io_opentelemetry_opentelemetry_sdk",
        "@maven//:io_opentelemetry_opentelemetry_sdk_trace",
        "@maven//:io_opentelemetry_semconv_opentelemetry_semconv",
        "@maven//:org_jetbrains_kotlinx_kotlinx_coroutines_core",
        "@maven//:org_jetbrains_kotlinx_kotlinx_coroutines_core_jvm",
    ],
)

repackaged_files(
    name = "intellij-bazel_jar",
    srcs = [
        "intellij-bazel_external_jars",
        ":intellij-bazel",
    ],
    prefix = "intellij-bazel/lib",
)

java_single_jar(
    name = "intellij-bazel_single_jar_for_debug",
    deps = [
        "intellij-bazel_external_jars",
        ":intellij-bazel",
    ],
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
        ":intellij-bazel_single_jar_for_debug",
        "//plugin-bsp:intellij-bsp_single_jar_for_debug",
    ],
)

intellij_plugin_debug_target(
    name = "plugin-bazel-with-server-debug",
    deps = [
        ":intellij-bazel-with-inmemory-server_single_jar_for_debug",
        "//plugin-bsp:intellij-bsp_single_jar_for_debug",
    ],
)

java_binary(
    name = "intellij-bazel-debug_exclude",
    main_class = "DummySourceForExclude",
    tags = ["no-ide"],
    runtime_deps = [
        "//protocol",
        "@maven//:ch_epfl_scala_bsp4j",
        "@maven//:org_apache_velocity_velocity_engine_core",
        "@maven//:org_jetbrains_kotlinx_kotlinx_coroutines_core",
        "@maven//:org_jetbrains_kotlinx_kotlinx_coroutines_core_jvm",
    ],
)

intellij_plugin(
    name = "intellij-bazel-with-inmemory-server",
    deploy_env = [
        "intellij-bazel-debug_exclude",
    ],
    plugin_deps = ["org.jetbrains.bsp"],
    plugin_xml = ":stamped_plugin_xml",
    visibility = ["//visibility:public"],
    deps = [
        ":plugin_library",
        "//plugin-bazel/plugin-inmem-server:plugin_library",
    ],
)

java_single_jar(
    name = "intellij-bazel-with-inmemory-server_single_jar_for_debug",
    deps = [
        ":intellij-bazel-with-inmemory-server",
        ":intellij-bazel-with-inmemory-server_external_jars",
    ],
)

repackaged_files(
    name = "intellij-bazel-with-inmemory-server_jar",
    srcs = [
        ":intellij-bazel-with-inmemory-server",
        ":intellij-bazel-with-inmemory-server_external_jars",
    ],
    prefix = "intellij-bazel-with-inmemory-server/lib",
)
