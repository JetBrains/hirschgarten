http_archive(
    name = "io_bazel_rules_kotlin",
    urls = ["https://github.com/bazelbuild/rules_kotlin/releases/download/v%s/rules_kotlin_release.tgz" % rules_kotlin_version],
    sha256 = rules_kotlin_sha,
    <error descr="An argument is already passed for this parameter">name</error> = "io_bazel_rules_kotlin_duplicate"
)

function(arg1 = "",arg2,<error descr="An argument is already passed for this parameter">arg1</error>=1)
