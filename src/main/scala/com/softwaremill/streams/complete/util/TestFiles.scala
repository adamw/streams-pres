package com.softwaremill.streams.complete.util

import java.io.File

object TestFiles {
  val BasePath = "/Users/adamw/projects/streams-tests/files/"

  def testFile(sizeMB: Int) = new File(s"$BasePath/$sizeMB.txt")
}
