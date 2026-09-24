load("@rules_cc//cc/common:cc_common.bzl", "cc_common")
load("@rules_cc//cc/common:cc_info.bzl", "CcInfo")

def _impl(ctx):
    include_dir = ctx.actions.declare_directory(ctx.attr.name)

    args = ctx.actions.args()
    args.add(include_dir.path)
    args.add(ctx.attr.include_prefix)
    args.add_all([
        "%s=%s" % (name, value)
        for name, value in sorted(ctx.attr.headers.items())
    ])

    ctx.actions.run(
        executable = ctx.executable._generator,
        arguments = [args],
        outputs = [include_dir],
        mnemonic = "GenerateHeaders",
        progress_message = "Generating headers into %{output}",
    )

    compilation_context = cc_common.create_compilation_context(
        headers = depset([include_dir]),
        includes = depset([include_dir.path]),
    )
    return [
        DefaultInfo(files = depset([include_dir])),
        CcInfo(compilation_context = compilation_context),
    ]

generated_headers = rule(
    implementation = _impl,
    attrs = {
        "headers": attr.string_dict(
            mandatory = True,
        ),
        "include_prefix": attr.string(
            mandatory = True,
        ),
        "_generator": attr.label(
            default = "//codegen:header_gen",
            executable = True,
            cfg = "exec",
        ),
    },
    fragments = ["cpp"],
)
