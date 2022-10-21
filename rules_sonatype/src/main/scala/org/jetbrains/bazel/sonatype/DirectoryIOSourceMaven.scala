package org.jetbrains.bazel.sonatype

import org.sonatype.spice.zapper.fs.DirectoryIOSource
import org.sonatype.spice.zapper.{Path, ZFile}

import java.io.{BufferedWriter, File, IOException, OutputStreamWriter}
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import java.security.{MessageDigest, NoSuchAlgorithmException}
import java.util
import java.util.logging.Logger
import scala.jdk.CollectionConverters._
import scala.sys.process.ProcessLogger

class DirectoryIOSourceMaven(filesPaths: List[Path]) extends DirectoryIOSource(new File("").getCanonicalFile) {

  private val LOG = Logger.getLogger(classOf[DirectoryIOSourceMaven].getName)

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
        Files.write(md5, toMd5(toHash).getBytes(StandardCharsets.UTF_8))
        Files.write(sha1, toSha1(toHash).getBytes(StandardCharsets.UTF_8))
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

  private def toSha1(toHash: Array[Byte]) = toHexS("%040x", "SHA-1", toHash)

  private def toMd5(toHash: Array[Byte]) = toHexS("%032x", "MD5", toHash)

  private def toHexS(fmt: String, algorithm: String, toHash: Array[Byte]) = try {
    val digest = MessageDigest.getInstance(algorithm)
    digest.update(toHash)
    String.format(fmt, new BigInteger(1, digest.digest))
  } catch {
    case e: NoSuchAlgorithmException =>
      throw new RuntimeException(e)
  }

  @throws[IOException]
  @throws[InterruptedException]
  private def sign(toSign: java.nio.file.Path, signed: java.nio.file.Path): Unit = {
    // Ideally, we'd use BouncyCastle for this, but for now brute force by assuming
    // the gpg binary is on the path
    import scala.sys.process._

    val proclog = ProcessLogger.apply(LOG.info, LOG.warning)
    val io = BasicIO(withIn = false, proclog).withInput { out =>
      val writer = new BufferedWriter(new OutputStreamWriter(out))
      writer.write(sys.env.getOrElse("GPG_PASSPHRASE", "''"))
      writer.flush()
      writer.close()
    }

    val gpgSign = Seq(
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
    ).run(io)
    if (gpgSign.exitValue() != 0) throw new IllegalStateException("Unable to sign: " + toSign)

    // Verify the signature
    val gpgVerify = Seq(
      "gpg",
      "--verify",
      "--verbose",
      "--verbose",
      signed.toAbsolutePath.toString,
      toSign.toAbsolutePath.toString
    ).run(proclog)
    if (gpgVerify.exitValue() != 0) throw new IllegalStateException("Unable to verify signature of " + toSign)
  }
}
