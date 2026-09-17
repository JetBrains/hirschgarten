load("@protobuf//bazel:java_proto_library.bzl", "java_proto_library")
load("@rules_proto//proto:defs.bzl", "proto_library")

package(default_visibility = ["//visibility:public"])

proto_library(
    name = "requests_proto",
    srcs = ["requests.proto"],
    deps = [
        "//common:common_proto",
        "@protobuf//:timestamp_proto",
    ],
)

java_proto_library(
    name = "requests_java_proto",
    deps = [":requests_proto"],
)
