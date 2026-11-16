"""The module extension that downloads the projects of the CLion tests."""

_NAME_PREFIX = "cc_test_project_"

_GITHUB_URL_TPL = "https://github.com/{repo}/archive/{commit}.zip"

def _archive_repo_impl(rctx):
    # the archive stays packed, since the test fixture unpacks it
    rctx.download(
        url = rctx.attr.urls,
        output = "archive",
        sha256 = rctx.attr.sha256,
    )

    rctx.file("BUILD.bazel", 'exports_files(["archive"], visibility = ["//visibility:public"])')

_archive_repo = repository_rule(
    implementation = _archive_repo_impl,
    attrs = {
        "sha256": attr.string(mandatory = True),
        "urls": attr.string_list(mandatory = True),
    },
)

_archive = tag_class(
    attrs = {
        "name": attr.string(mandatory = True),
        "sha256": attr.string(mandatory = True),
        "urls": attr.string_list(mandatory = True),
    },
)

_github = tag_class(
    attrs = {
        "repo": attr.string(mandatory = True),
        "commit": attr.string(mandatory = True),
        "sha256": attr.string(mandatory = True),
    },
)

def _registry_impl(mctx):
    for module in mctx.modules:
        for tag in module.tags.archive:
            _archive_repo(
                name = _NAME_PREFIX + tag.name,
                sha256 = tag.sha256,
                urls = tag.urls,
            )

        for tag in module.tags.github:
            _archive_repo(
                name = _NAME_PREFIX + tag.repo.replace("/", "_"),
                sha256 = tag.sha256,
                urls = [_GITHUB_URL_TPL.format(
                    repo = tag.repo,
                    commit = tag.commit,
                )],
            )

    return mctx.extension_metadata(
        reproducible = True,
        root_module_direct_deps = "all",
        root_module_direct_dev_deps = [],
    )

registry = module_extension(
    implementation = _registry_impl,
    tag_classes = {
        "archive": _archive,
        "github": _github,
    },
)
