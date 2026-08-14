load("@my_protobuf//bazel:proto_library.bzl", "proto_library")

proto_library(
    name = "lib_a",
    srcs = ["src/package/lib_a.proto"],
    strip_import_prefix = "src/package",
    visibility = ["//visibility:public"],
)
