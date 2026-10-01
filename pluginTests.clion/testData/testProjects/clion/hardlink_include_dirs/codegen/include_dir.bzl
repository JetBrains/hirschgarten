load("@rules_cc//cc/common:cc_common.bzl", "cc_common")
load("@rules_cc//cc/common:cc_info.bzl", "CcInfo")

def _include_dir_impl(ctx):
    include_dir = ctx.actions.declare_directory(ctx.attr.name)

    args = ctx.actions.args()
    args.add(include_dir.path)
    args.add(ctx.attr.include_prefix)
    args.add_all(ctx.files.hdrs)

    ctx.actions.run_shell(
        inputs = ctx.files.hdrs,
        outputs = [include_dir],
        arguments = [args],
        command = 'dir="$1/$2"; shift 2; mkdir -p "$dir"; cp "$@" "$dir"',
    )

    compilation_context = cc_common.create_compilation_context(
        headers = depset([include_dir]),
        includes = depset([include_dir.path]),
    )
    return [
        DefaultInfo(files = depset([include_dir])),
        CcInfo(compilation_context = compilation_context),
    ]

# an include directory that is a tree artifact, like the ones rules_foreign_cc produces
include_dir = rule(
    implementation = _include_dir_impl,
    attrs = {
        "hdrs": attr.label_list(allow_files = True),
        "include_prefix": attr.string(mandatory = True),
    },
)
