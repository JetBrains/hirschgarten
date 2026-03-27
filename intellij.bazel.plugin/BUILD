load("//:versions.bzl", "INTELLIJ_BAZEL_VERSION", "PLATFORM_VERSION", "SINCE_VERSION", "UNTIL_VERSION")
load("//rules_intellij/build_defs:build_defs.bzl", "stamped_plugin_xml")
load("//rules_intellij/build_defs:build_defs.bzl", "jvm_library", "resourcegroup")

package(default_visibility = ["//visibility:public"])

stamped_plugin_xml(
    name = "plugin_xml_with_versions",
    plugin_xml = "resources/META-INF/plugin.xml",
    since_build_numbers = {PLATFORM_VERSION: SINCE_VERSION},
    stamp_since_build = True,
    stamp_until_build = True,
    until_build_numbers = {PLATFORM_VERSION: UNTIL_VERSION},
    version = INTELLIJ_BAZEL_VERSION,
)

resourcegroup(
  name = "plugin_resources",
  # Exclude needed because we add versions to plugin.xml, see plugin_xml_with_versions
  srcs = glob(["resources/**/*"], exclude = ["resources/META-INF/plugin.xml"]),
  strip_prefix = "resources"
)

jvm_library(
  name = "plugin",
  module_name = "bazel-plugin",
  visibility = ["//visibility:public"],
  srcs = glob([], allow_empty = True),
  resources = [":plugin_resources", ":plugin_xml_with_versions"],
  plugin_zip_layout = "bazel-plugin/lib",
)
