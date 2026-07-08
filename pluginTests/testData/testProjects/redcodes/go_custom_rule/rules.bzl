load("@rules_go//go:def.bzl", "GoInfo", "go_context", "new_go_info")
load(
    "@rules_go//go/private:common.bzl",
    "GO_TOOLCHAIN",
)
load(
    "@rules_go//go/private:context.bzl",
    "CGO_ATTRS",
    "CGO_FRAGMENTS",
    "CGO_TOOLCHAINS",
)

def _simple_gen_impl(ctx):
    go = go_context(ctx)
    out = go.declare_file(go, "{}.go".format(ctx.label.name))
    content = """package generated

func GeneratedFunction() {}
"""
    ctx.actions.write(
        output = out,
        content = content,
    )

    go_info = new_go_info(
        go = go,
        attr = ctx.attr,
        generated_srcs = [out],
        coverage_instrumented = ctx.coverage_instrumented(),
    )
    archive = go.archive(go, go_info)

    return [
        go_info,
        archive,
        DefaultInfo(
            files = depset([archive.data.file]),
            runfiles = archive.runfiles,
        ),
        OutputGroupInfo(
            go_generated_srcs = [out],
        ),
    ]

simple_gen = rule(
    implementation = _simple_gen_impl,
    attrs = {
        "importpath": attr.string(
            mandatory = True,
        ),
        "_go_config": attr.label(default = "@rules_go//:go_config"),
        "_stdlib": attr.label(default = "@rules_go//:stdlib"),
    } | CGO_ATTRS,
    fragments = CGO_FRAGMENTS,
    toolchains = ["@rules_go//go:toolchain"] + CGO_TOOLCHAINS,
)
