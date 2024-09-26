package org.jetbrains.bazel.sonatype

import org.sonatype.spice.zapper.fs.DirectoryIOSource
import org.sonatype.spice.zapper.ZFile
import org.sonatype.spice.zapper.Path as ZPath // Alias for zapper.Path
import java.io.BufferedWriter
import java.io.File
import java.io.IOException
import java.io.OutputStreamWriter
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

class DirectoryIOSourceMaven(private val filesPaths: List<ZPath>) : DirectoryIOSource(File("").canonicalFile) {

  private val logger: Logger = LogManager.getLogger(DirectoryIOSourceMaven::class.java)

  fun processFiles(filesPaths: List<ZPath>): List<ZPath> =
    filesPaths
      .map { file -> Paths.get(file.stringValue()) } // java.nio.file.Path
      .flatMap { jPath ->
        val signed = Paths.get("${jPath.toString()}.asc") // java.nio.file.Path
        sign(jPath, signed) // Passing java.nio.file.Path
        listOf(ZPath(jPath.toString()), ZPath(signed.toString()))
      }
      .flatMap { zPath ->
        val toHash = Files.readAllBytes(Paths.get(zPath.stringValue())) // java.nio.file.Path
        val md5Path = Paths.get("${zPath.stringValue()}.md5") // java.nio.file.Path
        val sha1Path = Paths.get("${zPath.stringValue()}.sha1") // java.nio.file.Path
        Files.deleteIfExists(md5Path)
        Files.deleteIfExists(sha1Path)
        val md5 = Files.createFile(md5Path)
        val sha1 = Files.createFile(sha1Path)
        Files.write(md5, toMd5(toHash).toByteArray(StandardCharsets.UTF_8))
        Files.write(sha1, toSha1(toHash).toByteArray(StandardCharsets.UTF_8))
        listOf(ZPath(md5.toString()), ZPath(sha1.toString()))
      }

  override fun scanDirectory(dir: File, zfiles: MutableList<ZFile>): Int {
    val processedFiles = processFiles(filesPaths)
    val files = processedFiles.map { createZFile(it) }
    zfiles.addAll(files)
    return 0
  }

  private fun toSha1(toHash: ByteArray): String = toHexS("%040x", "SHA-1", toHash)

  private fun toMd5(toHash: ByteArray): String = toHexS("%032x", "MD5", toHash)

  private fun toHexS(fmt: String, algorithm: String, toHash: ByteArray): String {
    return try {
      val digest = MessageDigest.getInstance(algorithm)
      digest.update(toHash)
      String.format(fmt, BigInteger(1, digest.digest()))
    } catch (e: NoSuchAlgorithmException) {
      throw RuntimeException(e)
    }
  }

  @Throws(IOException::class, InterruptedException::class)
  private fun sign(toSign: java.nio.file.Path, signed: java.nio.file.Path) {
    // Ideally, we'd use BouncyCastle for this, but for now brute force by assuming
    // the gpg binary is on the path
    val passphrase = System.getenv("GPG_PASSPHRASE") ?: "''"

    val processBuilder = ProcessBuilder(
      "gpg",
      "--verbose",
      "--verbose",
      "--use-agent",
      "--armor",
      "--detach-sign",
      "--batch",
      "--passphrase-fd",
      "0",
      "--no-tty",
      "-o",
      signed.toAbsolutePath().toString(),
      toSign.toAbsolutePath().toString()
    )

    val process = processBuilder.start()

    // Write passphrase to stdin
    BufferedWriter(OutputStreamWriter(process.outputStream)).use { writer ->
      writer.write(passphrase)
      writer.flush()
    }

    val exitCode = process.waitFor()
    if (exitCode != 0) throw SigningException("Unable to sign: $toSign")

    // Verify the signature
    val verifyBuilder = ProcessBuilder(
      "gpg",
      "--verify",
      "--verbose",
      "--verbose",
      signed.toAbsolutePath().toString(),
      toSign.toAbsolutePath().toString()
    )

    val verifyProcess = verifyBuilder.start()
    val verifyExitCode = verifyProcess.waitFor()
    if (verifyExitCode != 0) throw SigningException("Unable to verify signature of $toSign")
  }
}

class SigningException(message: String) : IOException(message)
