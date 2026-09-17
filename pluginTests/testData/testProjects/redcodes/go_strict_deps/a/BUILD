load("@rules_go//go:def.bzl", "go_library")

go_library(
    name = "a",
    srcs = ["a.go"],
    importpath = "github.com/example/a",
    visibility = ["//visibility:public"],
    deps = [
        "//b",
    ],
)
