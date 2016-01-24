package components

import java.util.UUID
import javax.inject.{Inject, Singleton}

import com.datastax.driver.core.Row
import com.websudos.phantom.CassandraTable
import com.websudos.phantom.dsl.{BooleanColumn, LongColumn, StringColumn, TimeUUIDColumn, context => _, _}
import com.websudos.phantom.keys.{PartitionKey, PrimaryKey}
import logging.LoggingComponent
import model.Todo
import uuid.UUIDHelper.newTimeUUID

import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by mwielocha on 04/06/15.
 */
@Singleton
class TodoRepository @Inject() (val cassandraConnection: CassandraConnection)
                               (implicit val ec: ExecutionContext) extends LoggingComponent {

  import cassandraConnection._

  sealed class Todos


  def findAll(userId: Long, limit: Int): Future[List[Todo]] = {
    ???
  }

  def find(userId: Long, id: UUID): Future[Option[Todo]] = {
    ???
  }

  def delete(userId: Long, id: UUID): Future[UUID] = {
    ???
  }

  def addOrUpdate(userId: Long, todo: Todo): Future[Todo] = {
    ???
  }
}
