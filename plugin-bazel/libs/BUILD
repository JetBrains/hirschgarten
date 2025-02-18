load("@aspect_bazel_lib//lib:output_files.bzl", "output_files")
load("@aspect_bazel_lib//lib:write_source_files.bzl", "write_source_files")

output_files(
    name = "starlark_debugging_proto_jar",
    paths = ["third_party/bazel/src/main/java/com/google/devtools/build/lib/starlarkdebug/proto/libstarlark_debugging_proto-speed.jar"],
    target = "//third_party/bazel/src/main/java/com/google/devtools/build/lib/starlarkdebug/proto:starlark_debugging_java_proto",
)

output_files(
    name = "intellij_plugin_deploy_target_proto_jar",
    paths = ["plugin-bazel/src/main/kotlin/org/jetbrains/bazel/intellij/libintellij_plugin_deploy_target_info_lib-speed.jar"],
    target = "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/intellij:intellij_plugin_deploy_target_info",
)

write_source_files(
    name = "check_in_proto_jar",
    files = {
        "starlark_debugging_proto.jar": ":starlark_debugging_proto_jar",
        "intellij_plugin_deploy_target_info.jar": ":intellij_plugin_deploy_target_proto_jar",
    },
)
