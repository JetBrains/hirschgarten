package org.jetbrains.bazel.sonatype

import org.apache.commons.codec.digest.DigestUtils
import org.sonatype.spice.zapper.fs.DirectoryIOSource
import org.sonatype.spice.zapper.{Path, ZFile}

import java.io.{BufferedWriter, File, IOException, OutputStreamWriter}
import java.nio.file.{Files, Paths}
import java.util
import scala.jdk.CollectionConverters._

class DirectoryIOSourceMaven(filesPaths: List[Path]) extends DirectoryIOSource(new File("").getCanonicalFile) {

  def processFiles(filesPaths: List[Path]): List[Path] =
    filesPaths
      .map(file => Paths.get(file.stringValue()))
      .flatMap(file => {
        val signed = Paths.get(s"${file.toString}.asc")
        sign(file, signed)
        List(file, signed)
      })
      .flatMap(file => {
        val toHash   = Files.readAllBytes(file)
        val md5Path  = Paths.get(s"${file.toString}.md5")
        val sha1Path = Paths.get(s"${file.toString}.sha1")
        Files.deleteIfExists(md5Path)
        Files.deleteIfExists(sha1Path)
        val md5  = Files.createFile(md5Path)
        val sha1 = Files.createFile(sha1Path)
        Files.write(md5, toMd5(toHash))
        Files.write(sha1, toSha1(toHash))
        List(file, md5, sha1)
      })
      .map(file => new Path(file.toString))

  override def scanDirectory(dir: File, zfiles: util.List[ZFile]): Int = {
    val processedFiles = processFiles(filesPaths)
    val files = processedFiles.map { path =>
      {
        createZFile(path)
      }
    }.asJava

    zfiles.addAll(files)
    0
  }

  private def toSha1(toHash: Array[Byte]): Array[Byte] = DigestUtils.sha(toHash)

  private def toMd5(toHash: Array[Byte]): Array[Byte] = DigestUtils.md5(toHash)

  @throws[IOException]
  @throws[InterruptedException]
  private def sign(toSign: java.nio.file.Path, signed: java.nio.file.Path): Unit = {
    // Ideally, we'd use BouncyCastle for this, but for now brute force by assuming
    // the gpg binary is on the path

    val gpg = new ProcessBuilder(
      "gpg",
      "--use-agent",
      "--armor",
      "--detach-sign",
      "--batch",
      "--passphrase-fd",
      "0",
      "--no-tty",
      "-o",
      signed.toAbsolutePath.toString,
      toSign.toAbsolutePath.toString
    ).start

    val writer = new BufferedWriter(new OutputStreamWriter(gpg.getOutputStream))
    writer.write(sys.env.getOrElse("PGP_PASSPHRASE", "''"))
    writer.flush()
    writer.close()
    gpg.waitFor
    if (gpg.exitValue != 0) throw new IllegalStateException("Unable to sign: " + toSign)

    // Verify the signature
    val verify = new ProcessBuilder(
      "gpg",
      "--verify",
      "--verbose",
      "--verbose",
      signed.toAbsolutePath.toString,
      toSign.toAbsolutePath.toString
    ).inheritIO.start
    verify.waitFor
    if (verify.exitValue != 0) throw new IllegalStateException("Unable to verify signature of " + toSign)
  }
}
