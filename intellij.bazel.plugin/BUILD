load("//:versions.bzl", "INTELLIJ_BAZEL_VERSION", "PLATFORM_VERSION", "SINCE_VERSION", "UNTIL_VERSION")
load("//:jvm.bzl", "stamped_plugin_xml")
load("//:jvm.bzl", "jvm_library")

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

jvm_library(
    name = "plugin",
    srcs = glob(
        [],
        allow_empty = True,
    ),
    module_name = "bazel-plugin",
    resources = glob(
        ["resources/**/*"],
        # Exclude needed because we add versions to plugin.xml, see plugin_xml_with_versions
        exclude = ["resources/META-INF/plugin.xml"]
    ),
    visibility = ["//visibility:public"],
    runtime_deps = ["//plugins/bazel/protobuf"],
    plugin_zip_layout = "bazel-plugin/lib",
    deps_to_bundle = [":plugin_xml_with_versions"]
)
