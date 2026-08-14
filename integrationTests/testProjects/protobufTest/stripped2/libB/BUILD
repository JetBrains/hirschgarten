load("@my_protobuf//bazel:java_proto_library.bzl", "java_proto_library")
load("@my_protobuf//bazel:proto_library.bzl", "proto_library")

java_proto_library(
    name = "lib_b_java",
    visibility = ["//visibility:public"],
    deps = [":lib_b"],
)

proto_library(
    name = "lib_b",
    srcs = ["lib_b.proto"],
    import_prefix = "my_prefix_1337",
    visibility = ["//visibility:public"],
    deps = ["//stripped2/libA:lib_a"],
)
