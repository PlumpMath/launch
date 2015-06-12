package com.outr.launch

import java.io.{FileOutputStream, File}
import java.net.URL

import org.powerscala.IO
import org.powerscala.log.Logging

import scala.io.Source

/**
 * @author Matt Hicks <matt@outr.com>
 */
class Updates(jarDirectory: File, baseURL: String) extends Logging {
  private val latestVersionURL = s"$baseURL/latest.version"
  private lazy val latestVersionFile = new File(jarDirectory, "latest.version")

  def remoteLatestVersion = Source.fromURL(latestVersionURL).mkString.trim

  def hasUpdate = {
    val localLatest = if (latestVersionFile.exists()) IO.copy(latestVersionFile).trim else ""
    if (remoteLatestVersion != localLatest) {
      true
    } else {
      false
    }
  }

  def download(handler: UpdateProgress => Unit) = {
    val filename = remoteLatestVersion
    val url = new URL(s"$baseURL/$filename")
    val connection = url.openConnection()
    val fileSize = connection.getContentLength
    val input = connection.getInputStream
    try {
      val file = new File(jarDirectory, filename)
      val output = new FileOutputStream(file)
      try {
        val buf = new Array[Byte](8192)
        var written = 0L
        while (written < fileSize) {
          val len = input.read(buf)
          written += len
          output.write(buf, 0, len)
          handler(UpdateProgress(file, written, fileSize))
        }
        IO.copy(filename, latestVersionFile)
      } finally {
        output.flush()
        output.close()
      }
    } finally {
      input.close()
    }
  }
}

case class UpdateProgress(file: File, written: Long, size: Long) {
  def percent = written.toDouble / size.toDouble
}

object Updates {
  def main(args: Array[String]): Unit = {
    val updates = new Updates(new File("jars"), "http://captiveimagination.com/download/test")
    if (updates.hasUpdate) {
      println("Update availabe, downloading...")
      updates.download {
        case p => println(s"Progress: ${p.file.getName}, Percent: ${(p.percent * 100.0).toInt}, Written: ${p.written}, Size: ${p.size}")
      }
    } else {
      println("Already have the latest version.")
    }
  }
}
