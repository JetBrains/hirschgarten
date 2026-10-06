load("@rules_go//go:def.bzl", "go_binary", "go_library")

go_library(
    name = "foo_lib",
    srcs = ["main.go"],
    importpath = "github.com/example/foo",
    visibility = ["//visibility:private"],
    deps = [
        "//internal/lib",
    ],
)

go_binary(
    name = "foo",
    embed = [":foo_lib"],
    visibility = ["//visibility:public"],
)
