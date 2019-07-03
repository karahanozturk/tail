import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest

trait LinesPublisher {
  def publish(lines: Iterator[String])
}

class S3Publisher(s3: S3Client, bucketName: String) extends LinesPublisher {

  override def publish(lines: Iterator[String]) = {
    val key = System.currentTimeMillis.toString
    s3.putObject(PutObjectRequest.builder.bucket(bucketName).key(key).build, RequestBody.fromString(lines.mkString("\n")))
  }
}