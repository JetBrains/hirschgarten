load("@rules_go//go:def.bzl", "go_library")

go_library(
    name = "b",
    srcs = ["b.go"],
    importpath = "github.com/example/b",
    visibility = ["//visibility:public"],
    deps = ["//c"],
)
