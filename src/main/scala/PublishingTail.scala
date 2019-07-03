import java.io.{File, RandomAccessFile}

import scala.concurrent.{ExecutionContext, Future}

object PublishingTail {

  private val ReadOnlyAccess = "r"

  def tail(file: File,
           publisher: LinesPublisher,
           delay: () => Unit = { () => Thread.sleep(1000) },
           offset: Long = 0)(implicit ec: ExecutionContext): Future[Unit] = {

    if (file.isFile && file.exists()) {
      Future {
        val randomAccessFile = new RandomAccessFile(file, ReadOnlyAccess)
        val lines = readLinesFrom(randomAccessFile, offset)
        if (lines.nonEmpty) publisher.publish(lines)

        val lastPosition = randomAccessFile.getFilePointer
        randomAccessFile.close()

        delay()
        tail(file, publisher, delay, lastPosition)
      }
    } else {
      Future.failed(new RuntimeException("No file found"))
    }
  }

  private def readLinesFrom(raf: RandomAccessFile, offset: Long): Iterator[String] = {
    raf.seek(offset)
    Iterator.continually(raf.readLine()).takeWhile(_ != null)
  }
}
