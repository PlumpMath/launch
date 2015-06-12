package com.outr.launch

import java.io.File

import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.xfer.FileSystemFile
import org.powerscala.IO
import org.powerscala.log.Logging

/**
 * @author Matt Hicks <matt@outr.com>
 */
class Deploy(jar: File) extends Logging {
  def upload(remoteHost: String,
             remotePath: String,
             remoteUsername: String,
             remotePassword: String) = {
    val ssh = new SSHClient()
    ssh.loadKnownHosts()
    info(s"Connecting to $remoteHost")
    ssh.connect(remoteHost)
    try {
      info("Authenticating...")
      ssh.authPassword(remoteUsername, remotePassword)
      info("Negotiating compression...")
      ssh.useCompression()

      info(s"Uploading ${jar.getName}...")
      ssh.newSCPFileTransfer().upload(new FileSystemFile(jar), s"$remotePath/${jar.getName}")
      val temp = File.createTempFile("latest.", ".version")
      IO.copy(jar.getName, temp)
      info("Uploading latest.version...")
      ssh.newSCPFileTransfer().upload(new FileSystemFile(temp), s"$remotePath/latest.version")

      info("Completed successfully")
    } finally {
      ssh.disconnect()
    }
  }
}