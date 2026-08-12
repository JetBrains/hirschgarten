load("@my_protobuf//bazel:proto_library.bzl", "proto_library")

java_proto_library(
    name = "lib_b_java",
    visibility = ["//visibility:public"],
    deps = [":lib_b"],
)

proto_library(
    name = "lib_b",
    srcs = ["lib_b.proto"],
    visibility = ["//visibility:public"],
    deps = ["//stripped1/libA:lib_a"],
)
