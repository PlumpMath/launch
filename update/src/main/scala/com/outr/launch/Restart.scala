package com.outr.launch

import java.io.File
import java.lang.management.ManagementFactory

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer
import scala.io.Source

/**
 * @author Matt Hicks <matt@outr.com>
 */
object Restart {
  def apply(jar: File, args: Array[String]) = {
    val javaPath = s"${System.getProperty("java.home")}/bin/java"
    val runtimeMxBean = ManagementFactory.getRuntimeMXBean
    val currentVmArgs = runtimeMxBean.getInputArguments.asScala.toList
    println(System.getProperty("sun.java.command"))

    val b = ListBuffer.empty[String]
    b += javaPath
    val vmArgs = new File(".vmargs")
    if (vmArgs.exists()) {
      val source = Source.fromFile(vmArgs)
      try {
        source.getLines().foreach(s => b += s)
      } finally {
        source.close()
      }
    }
    currentVmArgs.foreach {
      case arg => b += arg
    }
    b += "-cp"
    b += jar.getCanonicalPath
    System.getProperty("sun.java.command").split(" ").foreach {
      case s => b += s
    }
    args.foreach {
      case arg => b += arg
    }
    val fileArgs = new File(".args")
    if (fileArgs.exists()) {
      val source = Source.fromFile(fileArgs)
      try {
        source.getLines().foreach(s => b += s)
      } finally {
        source.close()
      }
    }
    println(b.toList.mkString(" "))
    val builder = new ProcessBuilder(b.toList: _*).inheritIO()
    builder.start()
    System.exit(0)
  }
}