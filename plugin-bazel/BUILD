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
load("@rules_kotlin//kotlin:core.bzl", "define_kt_toolchain")

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
    plugin_name = "Bazel by JetBrains (experimental)",
)

java_binary(
    name = "intellij-bazel_exclude",
    srcs = ["//plugin-bazel:DummySourceForExclude.java"],
    main_class = "DummySourceForExclude",
    deps = [
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

intellij_plugin(
    name = "intellij-bazel_merged",
    plugin_xml = ":stamped_plugin_xml",
    deps = [
        ":plugin_library",
        "//plugin-bsp:plugin_library",
    ],
)

intellij_plugin_debug_target(
    name = "ijwb_bazel_dev",
    deps = [
        ":intellij-bazel_merged",
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
