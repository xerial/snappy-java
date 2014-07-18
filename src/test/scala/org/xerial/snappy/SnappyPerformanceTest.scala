package org.xerial.snappy

import java.io.{ByteArrayOutputStream, ByteArrayInputStream}

import xerial.core.log.LogLevel

import scala.util.Random


/**
 *
 */
class SnappyPerformanceTest extends SnappySpec {

  lazy val data = {
    val a = new Array[Byte](32 * 1024 * 1024)

    for (i <- (0 until a.length).par) {
      a(i) = Math.sin(i * 0.01).toByte
    }
    a
  }


  "SnappyOutputStream" should {

    "improve output performance" taggedAs("out") in {

      val input = data

      time("compression", repeat=100, logLevel = LogLevel.INFO) {
        block("default") {
          val out = new ByteArrayOutputStream()
          val sout = new SnappyOutputStream(out)
          sout.write(input)
          out.close()
        }

      }

      //info(f"compressed size: ${compressed.length}%,d, input: ${data.length}%,d")
    }



  }

}
