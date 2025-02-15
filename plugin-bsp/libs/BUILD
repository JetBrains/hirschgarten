load("@aspect_bazel_lib//lib:output_files.bzl", "output_files")
load("@aspect_bazel_lib//lib:write_source_files.bzl", "write_source_files")

output_files(
    name = "intellij_plugin_deploy_target_proto_jar",
    paths = ["plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/intellij/libintellij_plugin_deploy_target_info_lib-speed.jar"],
    target = "//plugin-bsp/src/main/kotlin/org/jetbrains/plugins/bsp/intellij:intellij_plugin_deploy_target_info",
)

write_source_files(
    name = "check_in_proto_jar",
    files = {
        "intellij_plugin_deploy_target_info.jar": ":intellij_plugin_deploy_target_proto_jar",
    },
)
