"""JVM flags that report which flag source set the precedence_a and precedence_b --define values."""

FLAG_SOURCES = [
    "build_flags",
    "sync_flags",
    "debug_flags",
    "run_configuration",
]

PRECEDENCE_JVM_FLAGS = select({
    "//flag_precedence:precedence_%s_%s" % (a, b): [
        "-Dprecedence.a=" + a,
        "-Dprecedence.b=" + b,
    ]
    for a in FLAG_SOURCES
    for b in FLAG_SOURCES
} | {"//conditions:default": []})
