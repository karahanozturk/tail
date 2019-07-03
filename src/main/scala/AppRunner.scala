import java.io.File

import com.typesafe.config.{Config, ConfigFactory}
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client

import scala.concurrent.ExecutionContext

object AppRunner {

  def main(args: Array[String]): Unit = {
    run()
  }

  def run() = {
    val conf = ConfigFactory.load()
    val file = new File(conf.getString("filePath"))
    val s3Publisher = Assembler.assembleS3Publisher(conf)
    implicit lazy val context: ExecutionContext = new NonDeamonContext

    PublishingTail.tail(file, s3Publisher, () => Thread.sleep(conf.getLong("fileReadDelayInMS")))
  }

  class NonDeamonContext extends ExecutionContext {
    override def execute(runnable:Runnable) = {
      val t = new Thread(runnable)
      t.setDaemon(false)
      t.start()
    }
    override def reportFailure(t:Throwable) = t.printStackTrace()
  }

}

object Assembler {

  def assembleS3Publisher: Config => LinesPublisher = { conf =>
    val s3 = assembleS3Client(conf)
    new S3Publisher(s3, conf.getString("aws.s3.bucketName"))
  }

  def assembleS3Client: Config => S3Client = { conf =>
    val awsCreds = AwsBasicCredentials.create(conf.getString("aws.accessKey"), conf.getString("aws.secretKey"))
    val region = Region.of(conf.getString("aws.region"))
    S3Client.builder.credentialsProvider(StaticCredentialsProvider.create(awsCreds)).region(region).build
  }
}