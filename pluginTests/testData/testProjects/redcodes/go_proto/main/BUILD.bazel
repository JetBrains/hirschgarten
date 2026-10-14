load("@rules_go//go:def.bzl", "go_library")

go_library(
    name = "main",
    srcs = ["main.go"],
    importpath = "example.com/main",
    visibility = ["//visibility:public"],
    deps = ["//proto:greeting_go_proto"],
)
