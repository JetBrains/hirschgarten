genrule(
    name = "genrule",
    outs = ["should_not_be_built.txt"],
    cmd = "echo 'Genrule should not be built during Build and Sync'; touch $@",
)

java_library(
    name = "main",
    srcs = ["Main.java"],
)
