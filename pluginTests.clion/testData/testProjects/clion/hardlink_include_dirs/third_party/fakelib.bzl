def _fakelib_impl(ctx):
    ctx.file("include/fakelib/fakelib.h", """
#pragma once

namespace fakelib {
int add(int a, int b);
}
""")

    ctx.file("src/fakelib.cc", """
#include "fakelib/fakelib.h"

namespace fakelib {
int add(int a, int b) { return a + b; }
}
    """)

    ctx.file("generated_include_dir.bzl", '''
"""A rule whose CcInfo include directory is a build-time tree artifact (like foreign_cc)."""

load("@rules_cc//cc/common:cc_common.bzl", "cc_common")
load("@rules_cc//cc/common:cc_info.bzl", "CcInfo")

def _impl(ctx):
    out = ctx.actions.declare_directory("gen/include")
    ctx.actions.run_shell(
        outputs = [out],
        command = """
mkdir -p "$1/fakelib"
cat > "$1/fakelib/generated.h" <<'H'
#pragma once
#define FAKELIB_GENERATED_ANSWER 42
H
""",
        arguments = [out.path],
    )
    compilation_context = cc_common.create_compilation_context(
        headers = depset([out]),
        includes = depset([out.path]),
    )
    return [
        DefaultInfo(files = depset([out])),
        CcInfo(compilation_context = compilation_context),
    ]

generated_include_dir = rule(
    implementation = _impl,
    fragments = ["cpp"],
)
''')
    ctx.file("BUILD.bazel", """
load("@rules_cc//cc:defs.bzl", "cc_library")
load(":generated_include_dir.bzl", "generated_include_dir")

package(default_visibility = ["//visibility:public"])

# Variant A: a SOURCE include directory. CcInfo.compilation_context.includes holds
#   external/<repo>/include             (source tree)
#   bazel-out/<cfg>/bin/external/<repo>/include   (its bin twin)
cc_library(
    name = "fakelib",
    srcs = ["src/fakelib.cc"],
    hdrs = ["include/fakelib/fakelib.h"],
    includes = ["include"],
)

# Variant B: a GENERATED include directory, a tree artifact that exists only under
# bazel-out/ (what rules_foreign_cc's cmake/configure_make targets produce). Its CcInfo holds
#   bazel-out/<cfg>/bin/external/<repo>/gen/include
generated_include_dir(
    name = "fakelib_generated",
)
""")

fakelib = repository_rule(
    implementation = _fakelib_impl,
    attrs = {},
)
