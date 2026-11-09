load("@rules_cc//cc:defs.bzl", "cc_binary", "cc_library")
load(":defs.bzl", my_macro = "macro")

package(default_visibility = ["//visibility:public"])

COPTS = ["-Wall"] + select({
    "//conditions:default": [],
    ":debug": ["-g"],
})

cc_library(
    name = "lib",
    srcs = glob(
        ["src/**/*.cc"],
        exclude = ["src/**/*_test.cc"],
    ),
    hdrs = glob(["include/**/*.h"]),
    copts = COPTS,
    deps = [
        ":base",
        "//third_party:absl",
    ],
)

cc_binary(
    name = "main",
    srcs = ["main.cc"],
    deps = [":lib"],
)

[my_macro(name = "gen_%s" % n) for n in ["a", "b"]]
