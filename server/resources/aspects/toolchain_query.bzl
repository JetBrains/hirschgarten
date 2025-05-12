def format(target):
    p = providers(target)
    if "ToolchainInfo" not in p or "java" not in dir(p["ToolchainInfo"]):
        fail("Unable to obtain the java toolchain info")
    toolchain = p["ToolchainInfo"].java
    javaHome = toolchain.java_runtime.java_home
    toolchainPath = toolchain._javabuilder.tool.executable.path
    toolchainOpts = toolchain.jvm_opt.to_list()
    return {"java_home": javaHome, "toolchain_path": toolchainPath, "jvm_opts": toolchainOpts}
