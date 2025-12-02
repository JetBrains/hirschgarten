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
load(
    "//rules_intellij/build_defs:intellij_plugin_debug_target.bzl",
    "intellij_plugin_debug_target",
)

kt_jvm_library(
  name = "plugin_resources",
  resources = glob(["src/main/resources/**/*"]),
  resource_strip_prefix = "plugin-bazel/src/main/resources"
)

kt_jvm_library(
  name = "plugin_resources_1",
  # Exclude needed because we add versions to plugin.xml, see plugin_xml_with_versions
  resources = glob(["src/main/jps-resources/**"], exclude = ["src/main/jps-resources/META-INF/plugin.xml"]),
  resource_strip_prefix = "plugin-bazel/src/main/jps-resources"
)

kt_jvm_library(
  name = "plugin",
  module_name = "intellij.bazel.plugin",
  # plugin_neverlink should be used instead
  visibility = ["//visibility:private"],
  srcs = glob(["src/main/kotlin/**/*.kt", "src/main/kotlin/**/*.java", "src/main/kotlin/**/*.form", "src/main/gen/**/*.kt", "src/main/gen/**/*.java"], allow_empty = True),
  deps = [
    ":plugin_resources",
    ":plugin_resources_1",
    "//commons",
    "//protobuf",
    "//server",
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
  ],
)

kt_jvm_library(
  name = "plugin_neverlink",
  visibility = ["//visibility:public"],
  neverlink = True,
  exports = [":plugin"],
)

intellij_plugin_library(
  name = "plugin_library",
  plugin_xmls = [],
  visibility = ["//visibility:public"],
  deps = [":plugin"],
)

intellij_plugin(
  name = "plugin-bazel",
  plugin_deps = [],
  plugin_xml = ":plugin_xml_with_versions",
  visibility = ["//visibility:public"],
  deps = [":plugin_library"],
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
  plugin_xml = "src/main/jps-resources/META-INF/plugin.xml",
  changelog_file = "CHANGELOG.md",
  since_build_numbers = {PLATFORM_VERSION: SINCE_VERSION},
  stamp_since_build = True,
  stamp_until_build = True,
  until_build_numbers = {PLATFORM_VERSION: UNTIL_VERSION},
  version = INTELLIJ_BAZEL_VERSION,
)
