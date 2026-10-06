load("@protobuf//bazel:proto_library.bzl", "proto_library")
load("@rules_go//proto:def.bzl", "go_proto_library")

proto_library(
    name = "greeting_proto",
    srcs = ["greeting.proto"],
    strip_import_prefix = "/proto",
    visibility = ["//visibility:public"],
)

go_proto_library(
    name = "greeting_go_proto",
    importpath = "example.com/proto/greeting",
    proto = ":greeting_proto",
    visibility = ["//visibility:public"],
)
