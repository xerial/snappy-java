package org.xerial.snappy

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, DataOutputStream}
import java.util.zip.GZIPOutputStream

import scala.util.Random

/**
  *
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

    "improve int compression ratio with bit-shuffle" taggedAs("int") in {
      val N = 10000
      val d = (0 until N).map(x => Random.nextInt(100)).toArray[Int]

      time("int-compression", repeat = 1, blockRepeat = 1) {
        block("bitshuffle") {
          val shuffled = BitShuffle.shuffle(d)
          val compressed = Snappy.compress(shuffled)
          logger.info(s"compressed size: ${compressed.length}")
          val decompressed = BitShuffle.unshuffleIntArray(Snappy.uncompress(compressed))
        }
        block("default") {
          val compressed = Snappy.compress(d)
          logger.info(s"compressed size: ${compressed.length}")
          val decompress = Snappy.uncompress(compressed)

        }
        block("gzip") {
          val out = new ByteArrayOutputStream()
          val o = new DataOutputStream(new GZIPOutputStream(out))
          d.foreach(x => o.writeInt(x))
          o.close()
          val compressed = out.toByteArray
          logger.info(s"compressed size: ${compressed.length}")
        }
      }
    }

    "improve long compression ratio with bit-shuffle" taggedAs("long") in {
      val N = 10000
      val d = (0 until N).map(x => Random.nextInt(10000).toLong).toArray[Long]

      time("long-compression", repeat = 1, blockRepeat = 1) {
        block("bitshuffle") {
          val shuffled = BitShuffle.shuffle(d)
          val compressed = Snappy.compress(shuffled)
          logger.info(s"compressed size: ${compressed.length}")
          val decompressed = BitShuffle.unshuffleLongArray(Snappy.uncompress(compressed))
        }
        block("default") {
          val compressed = Snappy.compress(d)
          logger.info(s"compressed size: ${compressed.length}")
          val decompress = Snappy.uncompress(compressed)
        }
        block("gzip") {
          val out = new ByteArrayOutputStream()
          val o = new DataOutputStream(new GZIPOutputStream(out))
          d.foreach(x => o.writeLong(x))
          o.close()
          val compressed = out.toByteArray
          logger.info(s"compressed size: ${compressed.length}")
        }
      }
    }

    "improve double compression ratio with bit-shuffle" taggedAs("double") in {
      val N = 10000
      val d = (0 until N).map(x => Random.nextInt(1000).toDouble).toArray[Double]

      time("double-compression", repeat = 1, blockRepeat = 1) {
        block("bitshuffle") {
          val shuffled = BitShuffle.shuffle(d)
          val compressed = Snappy.compress(shuffled)
          logger.info(s"compressed size: ${compressed.length}")
          val decompressed = BitShuffle.unshuffleDoubleArray(Snappy.uncompress(compressed))
        }
        block("default") {
          val compressed = Snappy.compress(d)
          logger.info(s"compressed size: ${compressed.length}")
          val decompress = Snappy.uncompress(compressed)

        }
        block("gzip") {
          val out = new ByteArrayOutputStream()
          val o = new DataOutputStream(new GZIPOutputStream(out))
          d.foreach(x => o.writeDouble(x))
          o.close()
          val compressed = out.toByteArray
          logger.info(s"compressed size: ${compressed.length}")
        }

      }
    }

  }

}
