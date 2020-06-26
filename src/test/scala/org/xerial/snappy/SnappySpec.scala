package org.xerial.snappy

import org.scalatest._
import wvlet.log.LogFormatter.SourceCodeLogFormatter
import wvlet.log.{LogSupport, Logger}
import wvlet.log.io.Timer

/**
  */
trait SnappySpec extends WordSpec with Matchers with GivenWhenThen with OptionValues with BeforeAndAfter with Timer with LogSupport {
  Logger.setDefaultFormatter(SourceCodeLogFormatter)

  implicit def toTag(s: String): Tag = Tag(s)
}
