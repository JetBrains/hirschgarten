load("@rules_shell//shell:sh_binary.bzl", "sh_binary")
load("//protobuf:check_in_protobuf.bzl", "check_in_protobuf")

sh_binary(
    name = "extract_bin",
    srcs = ["extract.sh"],
)

check_in_protobuf(
    name = "check_in_protobuf",
    jars = [
        {
            "label": "//server/server/src/main/kotlin/org/jetbrains/bazel/server/sync/proto:bsp_target_info_java_proto_private",
            "path": "server/server/src/main/kotlin/org/jetbrains/bazel/server/sync/proto/bsp_target_info_proto-speed-src.jar",
        },
        {
            "label": "@googleapis//google/devtools/build/v1:build_java_proto",
            "path": "../googleapis+/google/devtools/build/v1/build_proto-speed-src.jar",
        },
        {
            "label": "@googleapis//google/devtools/build/v1:build_java_grpc",
            "path": "../googleapis+/google/devtools/build/v1/libbuild_java_grpc-src.jar",
        },
        {
            "label": "//third_party/bazel/src/main/protobuf:build_java_proto",
            "path": "third_party/bazel/src/main/protobuf/build_proto-speed-src.jar",
        },
        {
            "label": "//third_party/bazel/src/main/protobuf:deps_java_proto",
            "path": "third_party/bazel/src/main/protobuf/deps_proto-speed-src.jar",
        },
        {
            "label": "//third_party/bazel/src/main/java/com/google/devtools/build/lib/buildeventstream/proto:build_event_stream_java_proto",
            "path": "third_party/bazel/src/main/java/com/google/devtools/build/lib/buildeventstream/proto/build_event_stream_proto-speed-src.jar",
        },
        {
            "label": "//third_party/bazel/src/main/java/com/google/devtools/build/lib/packages/metrics:package_load_metrics_java_proto",
            "path": "third_party/bazel/src/main/java/com/google/devtools/build/lib/packages/metrics/package_load_metrics_proto-speed-src.jar",
        },
        {
            "label": "//third_party/bazel/src/main/protobuf:action_cache_java_proto",
            "path": "third_party/bazel/src/main/protobuf/action_cache_proto-speed-src.jar",
        },
        {
            "label": "//third_party/bazel/src/main/protobuf:option_filters_java_proto",
            "path": "third_party/bazel/src/main/protobuf/option_filters_proto-speed-src.jar",
        },
        {
            "label": "//third_party/bazel/src/main/protobuf:command_line_java_proto",
            "path": "third_party/bazel/src/main/protobuf/command_line_proto-speed-src.jar",
        },
        {
            "label": "//third_party/bazel/src/main/protobuf:failure_details_java_proto",
            "path": "third_party/bazel/src/main/protobuf/failure_details_proto-speed-src.jar",
        },
        {
            "label": "//third_party/bazel/src/main/protobuf:strategy_policy_java_proto",
            "path": "third_party/bazel/src/main/protobuf/strategy_policy_proto-speed-src.jar",
        },
        {
            "label": "//third_party/bazel/src/main/protobuf:invocation_policy_java_proto",
            "path": "third_party/bazel/src/main/protobuf/invocation_policy_proto-speed-src.jar",
        },
        {
            "label": "//third_party/bazel/src/main/java/com/google/devtools/build/lib/starlarkdebug/proto:starlark_debugging_java_proto",
            "path": "third_party/bazel/src/main/java/com/google/devtools/build/lib/starlarkdebug/proto/starlark_debugging_proto-speed-src.jar",
        },
        {
            "label": "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/intellij:intellij_plugin_deploy_target_info",
            "path": "plugin-bazel/src/main/kotlin/org/jetbrains/bazel/intellij/intellij_plugin_deploy_target_info_lib-speed-src.jar",
        },
        {
            "label": "//third_party/bazel/src/main/protobuf:stardoc_output_java_proto",
            "path": "third_party/bazel/src/main/protobuf/stardoc_output_proto-speed-src.jar",
        },
    ],
    tool = ":extract_bin",
)
