package scorex.transaction

import java.util.concurrent.locks.ReentrantReadWriteLock

import com.wavesplatform.history.HistoryWriterImpl
import com.wavesplatform.state2._
import org.scalatest.{FunSuite, Matchers}
import scorex.lagonaki.mocks.TestBlock

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import vsys.db.openDB

class HistoryWriterTest extends FunSuite with Matchers with HistoryTest {

  test("concurrent access to lastBlock doesn't throw any exception") {
    val db = openDB("./test/historywriter/data", true)
    val history = new HistoryWriterImpl(db, new ReentrantReadWriteLock(), true)
    appendGenesisBlock(history)

    (1 to 1000).foreach { _ =>
      appendTestBlock(history)
    }

    @volatile var failed = false

    def tryAppendTestBlock(history: HistoryWriterImpl): Either[ValidationError, BlockDiff] =
      history.appendBlock(TestBlock.withReference(history.lastBlock.get.uniqueId))(Right(BlockDiff.empty))

    (1 to 1000).foreach { _ =>
      Future(tryAppendTestBlock(history)).recover[Any] { case e => e.printStackTrace(); failed = true }
      Future(history.discardBlock()).recover[Any] { case e => e.printStackTrace(); failed = true }
    }
    Thread.sleep(1000)

    failed shouldBe false
  }
}
