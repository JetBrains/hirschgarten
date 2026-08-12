load("@my_protobuf//bazel:proto_library.bzl", "proto_library")

java_proto_library(
    name = "lib_b_java",
    visibility = ["//visibility:public"],
    deps = [":lib_b"],
)

proto_library(
    name = "lib_b",
    srcs = ["lib_b.proto"],
    import_prefix = "my_prefix",
    visibility = ["//visibility:public"],
    deps = ["//prefix/libA:lib_a"],
)
