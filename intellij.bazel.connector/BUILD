### auto-generated section `iml intellij.bazel.connector` start
exports_files([
  "intellij.bazel.connector.iml",
], visibility = ["//visibility:public"])
### auto-generated section `iml intellij.bazel.connector` end

### auto-generated section `build intellij.bazel.connector` start
load("//rules_intellij/build_defs:build_defs.bzl", "jvm_library", "resourcegroup")
load("@rules_java//java:java_import.bzl", "java_import")
load("//rules_intellij/intellij_platform_sdk:build_defs.bzl", "select_for_plugin_api")

resourcegroup(
  name = "connector_resources",
  srcs = glob(["resources/**/*"]),
  strip_prefix = "resources"
)

jvm_library(
  name = "connector",
  module_name = "intellij.bazel.connector",
  visibility = ["//visibility:public"],
  srcs = glob(["src/**/*.kt", "src/**/*.java", "src/**/*.form"], allow_empty = True),
  resources = [":connector_resources"],
  deps = [
    "//intellij.bazel.commons:commons",
    "//protobuf",
  ],
  deps_to_bundle = [
    ":jackson_dataformat_without_deps",
  ],
)

# Using @maven//:com_fasterxml_jackson_dataformat_jackson_dataformat_xml directly means dependant libraries are added to the transitive closure.
# But all other Jackson libraries are already packed inside IJ platform, so import just one .jar file to avoid java.lang.LinkageError in runtime.
# See: https://youtrack.jetbrains.com/issue/BAZEL-3028/hirschgarten-253-and-261-branch-is-broken#focus=Comments-27-13760427.0-0
java_import(
    name = "jackson_dataformat_without_deps",
    jars = select_for_plugin_api({
        # The version must match IDEA's Jackson version exactly to the prevent exceptions in runtime
        "intellij-2026.1": ["@maven//:com_fasterxml_jackson_dataformat_jackson_dataformat_xml_2_19_0_extension"],
    }),
)

### auto-generated section `build intellij.bazel.connector` end