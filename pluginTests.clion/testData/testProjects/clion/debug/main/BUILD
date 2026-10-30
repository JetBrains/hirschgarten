load("@rules_cc//cc:defs.bzl", "cc_binary", "cc_test")

cc_binary(
    name = "main",
    srcs = ["main.cc"],
    deps = [
        "//lib/default:lib",
        "//lib/generated:lib",
        "//lib/hdronly:lib",
        "@external_lib//:lib",
    ],
)

cc_test(
    name = "test",
    srcs = ["test.cc"],
    deps = ["@catch2//:catch2_main"],
)
