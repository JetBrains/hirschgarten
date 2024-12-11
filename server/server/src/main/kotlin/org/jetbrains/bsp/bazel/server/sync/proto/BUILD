load("@protobuf//bazel:java_proto_library.bzl", "java_proto_library")
load("@rules_proto//proto:defs.bzl", "proto_library")

java_proto_library(
    name = "bsp_target_info_java_proto",
    visibility = ["//visibility:public"],
    deps = [":bsp_target_info_proto"],
)

proto_library(
    name = "bsp_target_info_proto",
    srcs = ["bsp_target_info.proto"],
    visibility = ["//visibility:private"],
)

java_proto_library(
    name = "build_java_proto",
    visibility = ["//visibility:public"],
    deps = [":build_proto"],
)

proto_library(
    name = "build_proto",
    srcs = ["build.proto"],
    visibility = ["//visibility:private"],
    deps = [":stardoc_output_proto"],
)

proto_library(
    name = "stardoc_output_proto",
    srcs = ["stardoc_output.proto"],
    visibility = ["//visibility:private"],
)
