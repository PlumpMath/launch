package com.outr.launch

import java.io.File
import java.net.URLClassLoader

import scala.collection.mutable.ListBuffer
import scala.io.Source

/**
 * @author Matt Hicks <matt@outr.com>
 */
object Launcher {
  private val jarDir = new File("jars")
  private val fileSeparator = System.getProperty("file.separator")

  private def usage() = {
    println(s"Usage: Launcher <process|classloader> <main-class> <args...>")
    System.exit(1)
  }

  def main(args: Array[String]): Unit = {
    if (args.length < 2) {
      usage()
    } else {
      val useProcess = args(0).toLowerCase match {
        case "process" => true
        case "classloader" => false
        case _ => {
          usage()
          false
        }
      }
      val mainClassName = args(1)
      val jars = jarDir.listFiles().toList.filter(f => f.getName.endsWith(".jar")).map(f => JARFile(f)).sorted.reverse
      val jar = jars.head
      jars.tail.foreach(j => j.file.delete())

      val extension = if (fileSeparator == "/") "" else "w.exe"
      val javaPath = s"${System.getProperty("java.home")}${fileSeparator}bin${fileSeparator}java$extension"

      if (useProcess) {
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
        b += "-cp"
        b += jar.file.getCanonicalPath
        b += mainClassName
        args.tail.tail.foreach {
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
        val builder = new ProcessBuilder(b.toList: _*).inheritIO()
        builder.start()
        System.exit(0)
      } else {
        val classLoader = new URLClassLoader(Array(jar.file.toURI.toURL), null)
        Thread.currentThread().setContextClassLoader(classLoader)
        val mainClass = classLoader.loadClass(mainClassName)
        val mainMethod = mainClass.getMethod("main", classOf[Array[String]])
        mainMethod.invoke(null, args.tail.tail)
      }
    }
  }
}

case class JARFile(file: File) extends Comparable[JARFile] {
  private val VersionRegex = """(.+)[-](\d+)[.](\d+)[.](\d+)[.]jar""".r

  val (major, minor, maintenance) = file.getName match {
    case VersionRegex(name, maj, min, maint) => (maj.toInt, min.toInt, maint.toInt)
  }

  override def compareTo(o: JARFile) = major.compareTo(o.major) match {
    case 0 => minor.compareTo(o.minor) match {
      case 0 => maintenance.compareTo(o.maintenance)
      case v => v
    }
    case v => v
  }
}