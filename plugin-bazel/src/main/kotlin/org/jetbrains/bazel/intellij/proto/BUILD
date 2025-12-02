load("@protobuf//bazel:java_proto_library.bzl", "java_proto_library")
load("@rules_proto//proto:defs.bzl", "proto_library")

proto_library(
    name = "intellij_plugin_deploy_target_info_lib",
    srcs = ["intellij_plugin_deploy_target_info.proto"],
)

java_proto_library(
    name = "intellij_plugin_deploy_target_info",
    visibility = ["//visibility:public"],
    deps = [":intellij_plugin_deploy_target_info_lib"],
)
