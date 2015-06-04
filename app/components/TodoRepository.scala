package components

import java.util.UUID
import javax.inject.{Singleton, Inject}

import com.datastax.driver.core.Row
import com.websudos.phantom.CassandraTable
import com.websudos.phantom.dsl.{BooleanColumn, StringColumn, TimeUUIDColumn, LongColumn}
import com.websudos.phantom.keys.{PrimaryKey, PartitionKey}
import logging.LoggingComponent
import model.Todo
import com.websudos.phantom.dsl.{context => _, _}
import uuid.UUIDHelper.newTimeUUID
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

/**
 * Created by mwielocha on 04/06/15.
 */
@Singleton
class TodoRepository @Inject() (cassandraConnection: CassandraConnection) extends LoggingComponent {

  import cassandraConnection._

  sealed class Todos extends CassandraTable[Todos, Todo] {

    object userId extends LongColumn[Todos, Todo](this)
      with PartitionKey[Long]

    object done extends BooleanColumn[Todos, Todo](this)
      with PrimaryKey[Boolean]
      with ClusteringOrder[Boolean]
      with Ascending

    object id extends TimeUUIDColumn[Todos, Todo](this)
      with PrimaryKey[UUID]
      with ClusteringOrder[UUID]
      with Ascending

    object name extends StringColumn[Todos, Todo](this)

    override def fromRow(r: Row): Todo = Todo(Some(id(r)), name(r), done(r))
  }

  object Todos extends Todos

  logger.info(Todos.create.queryString)

  def findUndoneByUser(userId: Long, limit: Int): Future[List[Todo]] = {
    Todos.select
      .where(_.userId eqs userId)
      .and(_.done eqs false)
      .limit(limit)
      .fetch()
  }

  def find(userId: Long, id: UUID, done: Boolean): Future[Option[Todo]] = {
    Todos.select
      .where(_.userId eqs userId)
      .and(_.done eqs done)
      .and(_.id eqs id)
      .one
  }

  def delete(userId: Long, id: UUID, done: Boolean): Future[UUID] = {
    Todos.delete
      .where(_.userId eqs userId)
      .and(_.done eqs done)
      .and(_.id eqs id)
      .future().map(_ => id)
  }

  def addOrUpdate(userId: Long, todo: Todo): Future[Todo] = {
    Todos.insert()
      .value(_.userId, userId)
      .value(_.id, todo.id.getOrElse(newTimeUUID))
      .value(_.name, todo.name)
      .value(_.done, todo.done)
      .future().map(_ => todo)
  }
}
