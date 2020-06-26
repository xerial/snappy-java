package org.xerial.snappy

import java.io.{ByteArrayOutputStream, ByteArrayInputStream}

import scala.util.Random

/**
  */
class SnappyPerformanceTest extends SnappySpec {

  lazy val data = {
    val a = new Array[Byte](128 * 1024 * 1024)

    for (i <- (0 until a.length).par) {
      a(i) = Math.sin(i * 0.01).toByte
    }
    a
  }

  "SnappyOutputStream" should {

    "improve output performance" taggedAs ("out") in {

      val input = data

      time("compression", repeat = 100) {
        // 0.037 sec. => 0.026
        block("default") {
          val out  = new ByteArrayOutputStream()
          val sout = new SnappyOutputStream(out)
          sout.write(input)
          sout.close()
          out.close()
        }

      }

      //info(f"compressed size: ${compressed.length}%,d, input: ${data.length}%,d")
    }

  }

}
