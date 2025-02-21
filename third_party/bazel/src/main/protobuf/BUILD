load("@protobuf//bazel:cc_proto_library.bzl", "cc_proto_library")
load("@protobuf//bazel:java_proto_library.bzl", "java_proto_library")
load("@protobuf//bazel:proto_library.bzl", "proto_library")
load("@protobuf//bazel:py_proto_library.bzl", "py_proto_library")

package(default_visibility = ["//visibility:public"])

FILES = [
    "action_cache",
    "android_deploy_info",
    "bazel_flags",
    "builtin",
    "deps",
    "java_compilation",
    "strategy_policy",
    "test_status",
    "execution_graph",
]

[proto_library(
    name = s + "_proto",
    srcs = [s + ".proto"],
) for s in FILES]

[java_proto_library(
    name = s + "_java_proto",
    deps = [":" + s + "_proto"],
) for s in FILES]

proto_library(
    name = "build_proto",
    srcs = ["build.proto"],
    deps = [":stardoc_output_proto"],
)

java_proto_library(
    name = "build_java_proto",
    deps = [":build_proto"],
)

proto_library(
    name = "analysis_v2_proto",
    srcs = ["analysis_v2.proto"],
    deps = [":build_proto"],
)

py_proto_library(
    name = "analysis_v2_py_proto",
    deps = [":analysis_v2_proto"],
)

java_proto_library(
    name = "analysis_v2_java_proto",
    deps = [":analysis_v2_proto"],
)

java_proto_library(
    name = "any_java_proto",
    deps = ["@protobuf//:any_proto"],
)

java_proto_library(
    name = "wrappers_java_proto",
    deps = ["@protobuf//:wrappers_proto"],
)

proto_library(
    name = "command_server_proto",
    srcs = ["command_server.proto"],
    deps = [
        ":failure_details_proto",
        "@protobuf//:any_proto",
    ],
)

java_proto_library(
    name = "command_server_java_proto",
    deps = [":command_server_proto"],
)

proto_library(
    name = "failure_details_proto",
    srcs = ["failure_details.proto"],
    deps = ["@protobuf//:descriptor_proto"],
)

java_proto_library(
    name = "failure_details_java_proto",
    deps = [":failure_details_proto"],
)

proto_library(
    name = "invocation_policy_proto",
    srcs = ["invocation_policy.proto"],
    deps = [":strategy_policy_proto"],
)

java_proto_library(
    name = "invocation_policy_java_proto",
    deps = [":invocation_policy_proto"],
)

proto_library(
    name = "option_filters_proto",
    srcs = ["option_filters.proto"],
)

java_proto_library(
    name = "option_filters_java_proto",
    deps = [":option_filters_proto"],
)

proto_library(
    name = "command_line_proto",
    srcs = ["command_line.proto"],
    deps = [":option_filters_proto"],
)

java_proto_library(
    name = "command_line_java_proto",
    deps = [":command_line_proto"],
)

proto_library(
    name = "spawn_proto",
    srcs = ["spawn.proto"],
    deps = [
        "@protobuf//:duration_proto",
        "@protobuf//:timestamp_proto",
    ],
)

java_proto_library(
    name = "spawn_java_proto",
    deps = [":spawn_proto"],
)

proto_library(
    name = "stardoc_output_proto",
    srcs = ["stardoc_output.proto"],
)

java_proto_library(
    name = "stardoc_output_java_proto",
    deps = [":stardoc_output_proto"],
)

filegroup(
    name = "srcs",
    srcs = glob(["**"]),
)
