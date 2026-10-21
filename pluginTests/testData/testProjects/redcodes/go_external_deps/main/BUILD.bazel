load("@rules_go//go:def.bzl", "go_library")

go_library(
    name = "main",
    srcs = ["main.go"],
    importpath = "example.com/extdeps/main",
    visibility = ["//visibility:public"],
    deps = ["@com_github_google_uuid//:uuid"],
)
