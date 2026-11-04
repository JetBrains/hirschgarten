load("@rules_cc//cc:defs.bzl", "cc_library")

cc_library(
    name = "lib",
    srcs = ["impl.cc"],
    hdrs = ["hdr.h"],
    include_prefix = "external_lib",
    visibility = ["//visibility:public"],
)
