def _fakelib_impl(ctx):
    ctx.file("include/fakelib/fakelib.h", "")

    ctx.file("BUILD.bazel", """
load("@rules_cc//cc:defs.bzl", "cc_library")
load("@@//codegen:include_dir.bzl", "include_dir")

package(default_visibility = ["//visibility:public"])

# a source include directory
cc_library(
    name = "fakelib",
    hdrs = ["include/fakelib/fakelib.h"],
    includes = ["include"],
)

genrule(
    name = "generated_files",
    outs = ["generated.h"],
    cmd = "touch $@",
)

# a generated include directory, that exists only under bazel-out
include_dir(
    name = "fakelib_generated",
    hdrs = [":generated_files"],
    include_prefix = "fakelib",
)
""")

fakelib = repository_rule(
    implementation = _fakelib_impl,
)
