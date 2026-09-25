load("@rules_cc//cc:defs.bzl", "cc_library", "cc_shared_library")

genrule(
    name = "generated_impl",
    srcs = [],
    outs = ["impl.cc"],
    cmd = r"""echo '#include "lib/hdr.h"' > $@ && echo 'int lib_function() { return LIB_VALUE; }' >> $@""",
)

cc_library(
    name = "lib",
    srcs = ["impl.cc"],
    hdrs = ["hdr.h"],
    visibility = ["//visibility:public"],
)

cc_shared_library(
    name = "shared",
    deps = [":lib"],
)

cc_library(
    name = "prebuild",
    srcs = [":shared"],
    hdrs = ["hdr.h"],
    visibility = ["//visibility:public"],
)
