load("@rules_go//go:def.bzl", "go_library")

genrule(
    name = "gen",
    outs = ["lib.go"],
    cmd = """
cat <<EOF > "$@"
package lib

func Bar() {}
EOF
""",
)

go_library(
    name = "lib",
    srcs = [":gen"],
    importpath = "github.com/example/internal/lib",
    visibility = ["//foo:__pkg__"],
)
