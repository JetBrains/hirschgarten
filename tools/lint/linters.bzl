load("@aspect_rules_lint//lint:ktlint.bzl", "lint_ktlint_aspect")
load("@aspect_rules_lint//lint:lint_test.bzl", "lint_test")


ktlint = lint_ktlint_aspect(
    binary = "@@com_github_pinterest_ktlint//file",
    editorconfig = "@@//:.editorconfig",
    baseline_file = "@@//:ktlint_baseline.xml",
)

ktlint_test = lint_test(aspect = ktlint)