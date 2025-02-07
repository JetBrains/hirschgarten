load("@rules_proto//proto:defs.bzl", "proto_library")

proto_library(
    name = "proto_lib",
    srcs = glob(["**/*.proto"]),
)

java_proto_library(
    name = "proto",
    visibility = ["//plugin-bazel:__subpackages__"],
    deps = [":proto_lib"],
)
