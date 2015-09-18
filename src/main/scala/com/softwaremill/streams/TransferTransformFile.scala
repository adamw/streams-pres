package com.softwaremill.streams

import java.io.File

import com.softwaremill.streams.complete.util.TestFiles
import com.softwaremill.streams.complete.util.Timed._

trait TransferTransformFile {
  /**
   * @return Number of bytes written
   */
  def run(from: File, to: File): Long
}

//
// SCALAZ
//

//
// AKKA
//

//
// RUNNER
//
object TransferTransformFileRunner extends App {
  def runTransfer(ttf: TransferTransformFile, sizeMB: Int): String = {
    val output = File.createTempFile("fft", "txt")
    try {
      ttf.run(TestFiles.testFile(sizeMB), output).toString
    } finally output.delete()
  }

  /*
  val tests = List(
    (ScalazStreamsTransferTransformFile, 10),
    (ScalazStreamsTransferTransformFile, 100),
    (ScalazStreamsTransferTransformFile, 500),
    (AkkaStreamsTransferTransformFile, 10),
    (AkkaStreamsTransferTransformFile, 100),
    (AkkaStreamsTransferTransformFile, 500)
  )

  runTests(tests.map { case (ttf, sizeMB) =>
    (s"${if (ttf == ScalazStreamsTransferTransformFile) "scalaz" else "akka"}, $sizeMB MB",
      () => runTransfer(ttf, sizeMB))
  }, 3)

  AkkaStreamsTransferTransformFile.shutdown()
  */
}
