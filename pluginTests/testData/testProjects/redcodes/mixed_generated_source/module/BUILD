load("@rules_java//java:java_library.bzl", "java_library")

genrule(
    name = "gen_src",
    outs = [
        "com/example/gen/Gen1.java",
        "com/example/gen/Gen2.java",
    ],
    cmd = """
cat > $(location com/example/gen/Gen1.java) << 'EOF'
package com.example.gen;

public class Gen1 {
    public static String value() {
      return "Hello ";
    }
}
EOF

cat > $(location com/example/gen/Gen2.java) << 'EOF'
package com.example.gen;

public class Gen2 {
    public static String value() {
      return "World!";
    }
}
EOF
"""
)

java_library(
    name = "lib",
    srcs = ["src/main/java/com/example/App.java", ":gen_src"],
)
