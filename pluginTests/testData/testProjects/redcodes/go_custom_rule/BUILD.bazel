load("@rules_go//go:def.bzl", "go_library")
load(":rules.bzl", "simple_gen")

simple_gen(
    name = "generated_lib",
    importpath = "github.com/example/generated",
)

go_library(
    name = "main",
    srcs = ["main.go"],
    importpath = "github.com/example/main",
    deps = [":generated_lib"],
)
