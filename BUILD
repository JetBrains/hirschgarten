load("@rules_kotlin//kotlin:core.bzl", "define_kt_toolchain", "kt_kotlinc_options")
load("@rules_java//java:java_binary.bzl", "java_binary")
load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")
load("//:versions.bzl", "INTELLIJ_BAZEL_VERSION", "PLATFORM_VERSION", "SINCE_VERSION", "UNTIL_VERSION")
load(
    "//rules_intellij/build_defs:build_defs.bzl",
    "intellij_plugin",
    "intellij_plugin_library",
    "plugin_deploy_zip",
    "repackaged_files",
    "stamped_plugin_xml",
)
load("//rules_intellij/intellij_platform_sdk:build_defs.bzl", "select_for_plugin_api")
load(
    "//rules_intellij/build_defs:intellij_plugin_debug_target.bzl",
    "intellij_plugin_debug_target",
)

package(default_visibility = ["//visibility:public"])

kt_kotlinc_options(
    name = "kotlinc_options",
    include_stdlibs = "none",
    jvm_target = select_for_plugin_api({
        "intellij-2026.1": "21",
    }),
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

kt_jvm_library(
  name = "bazel-plugin_resources",
  resources = glob(["plugin-bazel/src/main/resources/**/*"]),
  resource_strip_prefix = "plugin-bazel/src/main/resources"
)

kt_jvm_library(
  name = "bazel-plugin_resources_1",
  # Exclude needed because we add versions to plugin.xml, see plugin_xml_with_versions
  resources = glob(["plugin-bazel/src/main/jps-resources/**/*"], exclude = ["plugin-bazel/src/main/jps-resources/META-INF/plugin.xml"]),
  resource_strip_prefix = "plugin-bazel/src/main/jps-resources"
)

kt_jvm_library(
    name = "bazel-plugin_resources_2",
    resources = glob(["server/resources/**/*"]),
    resource_strip_prefix = "server/resources",
)

kt_jvm_library(
  name = "bazel-plugin",
  module_name = "intellij.bazel.plugin",
  # plugin_neverlink should be used instead
  visibility = ["//visibility:private"],
  srcs = glob(["plugin-bazel/src/main/kotlin/**/*.kt", "plugin-bazel/src/main/kotlin/**/*.java", "plugin-bazel/src/main/kotlin/**/*.form", "plugin-bazel/src/main/gen/**/*.kt", "plugin-bazel/src/main/gen/**/*.java", "server/server/src/main/kotlin/**/*.kt", "server/server/src/main/kotlin/**/*.java", "server/server/src/main/kotlin/**/*.form", "server/logger/src/main/kotlin/**/*.kt", "server/logger/src/main/kotlin/**/*.java", "server/logger/src/main/kotlin/**/*.form", "server/install/src/main/kotlin/**/*.kt", "server/install/src/main/kotlin/**/*.java", "server/install/src/main/kotlin/**/*.form", "server/commons/src/main/kotlin/**/*.kt", "server/commons/src/main/kotlin/**/*.java", "server/commons/src/main/kotlin/**/*.form", "server/bazelrunner/src/main/kotlin/**/*.kt", "server/bazelrunner/src/main/kotlin/**/*.java", "server/bazelrunner/src/main/kotlin/**/*.form"], allow_empty = True),
  deps = [
    ":bazel-plugin_resources",
    ":bazel-plugin_resources_1",
    ":bazel-plugin_resources_2",
    "//commons",
    "//protobuf",
    "//rules_intellij/third_party/code_with_me",
    "//rules_intellij/third_party/devkit",
    "//rules_intellij/third_party/go",
    "//rules_intellij/third_party/performance",
    "//rules_intellij/third_party/protobuf:protoedit",
    "//rules_intellij/third_party/python",
    "//rules_intellij/third_party/terminal",
    "//rules_intellij/intellij_platform_sdk:java",
    "//rules_intellij/intellij_platform_sdk:kotlin",
    "//rules_intellij/intellij_platform_sdk:bytecode_viewer",
    "//rules_intellij/intellij_platform_sdk:junit",
    "//rules_intellij/intellij_platform_sdk:plugin_api",
    "@maven//:com_fasterxml_jackson_dataformat_jackson_dataformat_xml",
  ],
)

kt_jvm_library(
  name = "bazel-plugin_neverlink",
  visibility = ["//visibility:public"],
  neverlink = True,
  exports = [":bazel-plugin"],
)

intellij_plugin_library(
  name = "bazel-plugin_library",
  plugin_xmls = [],
  visibility = ["//visibility:public"],
  deps = [":bazel-plugin"],
)

intellij_plugin(
  name = "plugin-bazel",
  plugin_deps = [],
  plugin_xml = ":plugin_xml_with_versions",
  visibility = ["//visibility:public"],
  deps = [":bazel-plugin_library"],
)

repackaged_files(
  name = "plugin-bazel_jar",
  srcs = [
    ":plugin-bazel",
  ],
  prefix = "plugin-bazel/lib",
)

repackaged_files(
  name = "k2_jar",
  srcs = [
    "//k2:intellij.bazel.kotlin.k2",
  ],
  prefix = "plugin-bazel/lib/modules",
)

plugin_deploy_zip(
  name = "plugin-bazel_zip",
  srcs = [
    ":plugin-bazel_jar",
    ":k2_jar",
  ],
  visibility = ["//visibility:public"],
  zip_filename = "plugin-bazel.zip",
)

intellij_plugin_debug_target(
  name = "plugin-bazel-debug",
  deps = [
    ":plugin-bazel_jar",
    ":k2_jar",
  ],
)

stamped_plugin_xml(
  name = "plugin_xml_with_versions",
  plugin_xml = "plugin-bazel/src/main/jps-resources/META-INF/plugin.xml",
  changelog_file = "plugin-bazel/CHANGELOG.md",
  since_build_numbers = {PLATFORM_VERSION: SINCE_VERSION},
  stamp_since_build = True,
  stamp_until_build = True,
  until_build_numbers = {PLATFORM_VERSION: UNTIL_VERSION},
  version = INTELLIJ_BAZEL_VERSION,
)
