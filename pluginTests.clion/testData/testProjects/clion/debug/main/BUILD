load("@rules_cc//cc:defs.bzl", "cc_binary")

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
