import java.io.File
import java.nio.file.{Files, StandardOpenOption}

import com.typesafe.config.ConfigFactory
import org.scalatest.concurrent.Eventually
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpec}
import software.amazon.awssdk.services.s3.model.{GetObjectRequest, ListObjectsV2Request}

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext
import scala.io.Source

class PublishingTailIT extends WordSpec with Matchers with BeforeAndAfterEach with Eventually {

  implicit val ec = ExecutionContext.global
  val tmpFile = new File("tmp.txt")

  override def afterEach() = {
    tmpFile.delete()
  }

  "PublishingTail" should {

    "read appended lines and publish into AWS S3" in {
      val config = ConfigFactory.load("test.conf")
      val s3 = Assembler.assembleS3Client(config)
      val publisher = Assembler.assembleS3Publisher(config)

      val bucket = config.getString("aws.s3.bucketName")
      val listObjectsReq = ListObjectsV2Request.builder()
        .bucket(bucket)
        .maxKeys(10)
        .build()
      val initialObjectSize = s3.listObjectsV2(listObjectsReq).contents().size()

      Files.write(tmpFile.toPath, "first line".getBytes)
      PublishingTail.tail(tmpFile, publisher)

      eventually(timeout(Span(5, Seconds)), interval(Span(500, Millis))) {
        val contents = s3.listObjectsV2(listObjectsReq).contents.asScala
        contents.size should be(initialObjectSize + 1)
        val content = Source.fromInputStream(s3.getObject(GetObjectRequest.builder.bucket(bucket).key(contents.last.key).build)).mkString
        content should be("first line")
      }

      Files.write(tmpFile.toPath, "second line".getBytes, StandardOpenOption.APPEND)
      eventually(timeout(Span(5, Seconds)), interval(Span(500, Millis))) {
        s3.listObjectsV2(listObjectsReq).contents().size() should be(initialObjectSize + 2)
      }
    }
  }
}