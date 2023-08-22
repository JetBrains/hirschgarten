package org.jetbrains.bsp.bazel.extension

// TODO we will do it later
// class BazelBspConnectionDetailsGenerator : BspConnectionDetailsGeneratorExtension      {
//    override fun name(): String = "bazel"
//
//    override fun canGenerateBspConnectionDetailsFile(projectPath: VirtualFile): Boolean =
//        projectPath.children.any { it.name == "WORKSPACE" }
//
//    override fun generateBspConnectionDetailsFile(projectPath: VirtualFile, outputStream: OutputStream): VirtualFile {
//        executeAndWait("cs launch org.jetbrains.bsp:bazel-bsp:2.1.0 -M org.jetbrains.bsp.bazel.install.Install", projectPath, outputStream)
//        return getChild(projectPath, listOf(".bsp", "bazelbsp.json"))!!
//    }
//
// }
