load("@io_bazel_rules_go//go:def.bzl", "go_binary", "go_library")

go_binary(
    name = "hello",
    embed = [":go_default_library"],
    visibility = ["//visibility:public"],
)

go_library(
    name = "go_default_library",
    srcs = ["hello.go"],
    importpath = "go-project/example",
    visibility = ["//visibility:private"],
    deps = [
        "//lib:go_default_library",
    ],
)
