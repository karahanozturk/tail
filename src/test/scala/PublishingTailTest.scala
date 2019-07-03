import java.io.File
import java.nio.file.{Files, StandardOpenOption}

import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.ExecutionContext

class PublishingTailTest extends WordSpec with Matchers with ScalaFutures with Eventually {

  implicit val ec = ExecutionContext.global

  "PublishingTail" should {

    "read all lines in the file and publish" in new Context {
      val content =
        """|11
           |22
           |33""".stripMargin

      Files.write(tmpFile.toPath, content.getBytes)
      PublishingTail.tail(tmpFile, publisher)

      eventually(timeout(Span(2, Seconds)), interval(Span(200, Millis))) {
        publisher.result.toList should be(List("11", "22", "33"))
      }

      tmpFile.delete()
    }

    "read appended lines and publish" in new Context {
      Files.write(tmpFile.toPath, "first line".getBytes)
      PublishingTail.tail(tmpFile, publisher)

      eventually(timeout(Span(2, Seconds)), interval(Span(200, Millis))) {
        publisher.result.toList should be(List("first line"))
      }

      Files.write(tmpFile.toPath, "second line".getBytes, StandardOpenOption.APPEND)
      eventually(timeout(Span(2, Seconds)), interval(Span(200, Millis))) {
        publisher.result.toList should be(List("first line", "second line"))
      }

      Files.write(tmpFile.toPath, "third line".getBytes, StandardOpenOption.APPEND)
      eventually(timeout(Span(2, Seconds)), interval(Span(200, Millis))) {
        publisher.result.toList should be(List("first line", "second line", "third line"))
      }

      tmpFile.delete()
    }

    "not publish empty content" in new Context {
      val content = ""

      Files.write(tmpFile.toPath, content.getBytes)
      PublishingTail.tail(tmpFile, publisher)

      Thread.sleep(100)
      publisher.publishCalled should be(false)

      tmpFile.delete()
    }

    "not read directories" in {
      val publisher = new InMemoryPublisher
      val dir = new File("./tmp")
      dir.mkdirs()

      whenReady(PublishingTail.tail(dir, publisher).failed) { e =>
        e.getMessage should be("No file found")
        publisher.publishCalled should be(false)
      }

      dir.delete()
    }
  }
}

trait Context {
  val r = scala.util.Random
  val tmpFile = new File(s"${r.nextInt}.txt")
  val publisher = new InMemoryPublisher
}

class InMemoryPublisher extends LinesPublisher {
  val result = scala.collection.mutable.ListBuffer[String]()
  var publishCalled = false

  override def publish(lines: Iterator[String]) = {
    publishCalled = true
    result ++= lines.toSeq
  }
}
