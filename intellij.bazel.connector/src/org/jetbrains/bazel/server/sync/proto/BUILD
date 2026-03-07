load("@protobuf//bazel:java_proto_library.bzl", "java_proto_library")
load("@rules_java//java:defs.bzl", "java_library")
load("@rules_proto//proto:defs.bzl", "proto_library")

java_library(
    name = "bsp_target_info_java_proto",
    visibility = ["//visibility:public"],
    exports = [
        "bsp_target_info_java_proto_private",
        "//rules_intellij/intellij_platform_sdk:plugin_api",
    ],
)

java_proto_library(
    name = "bsp_target_info_java_proto_private",
    visibility = ["//protobuf:__subpackages__"],
    deps = [":bsp_target_info_proto"],
)

proto_library(
    name = "bsp_target_info_proto",
    srcs = ["bsp_target_info.proto"],
    visibility = ["//visibility:private"],
)
