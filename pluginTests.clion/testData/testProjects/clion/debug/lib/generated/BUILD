load("@rules_cc//cc:defs.bzl", "cc_library")

genrule(
    name = "impl",
    outs = ["impl.cc"],
    cmd = r"""echo '#include "lib/generated/hdr.h"' > $@ && echo 'int generated_lib_function(void) { const int val = GENERATED_LIB_VALUE; return val; }' >> $@""",
)

cc_library(
    name = "lib",
    srcs = ["impl.cc"],
    hdrs = ["hdr.h"],
    visibility = ["//main:__pkg__"],
)
