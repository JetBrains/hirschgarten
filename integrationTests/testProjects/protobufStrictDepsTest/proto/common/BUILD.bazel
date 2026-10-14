load("@protobuf//bazel:java_proto_library.bzl", "java_proto_library")
load("@rules_proto//proto:defs.bzl", "proto_library")

package(default_visibility = ["//visibility:public"])

proto_library(
    name = "common_proto",
    srcs = ["common.proto"],
)

java_proto_library(
    name = "common_java_proto",
    deps = [":common_proto"],
)
