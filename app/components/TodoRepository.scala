package components

import java.util.UUID
import javax.inject.{Singleton, Inject}

import com.datastax.driver.core.Row
import com.websudos.phantom.{dsl, CassandraTable}
import com.websudos.phantom.dsl.{BooleanColumn, StringColumn, TimeUUIDColumn, LongColumn}
import com.websudos.phantom.keys.{PrimaryKey, PartitionKey}
import logging.LoggingComponent
import model.Todo
import com.websudos.phantom.dsl.{context => _, _}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import uuid.UUIDHelper.newTimeUUID

import scala.concurrent.{Await, Future}

/**
 * Created by mwielocha on 04/06/15.
 */
@Singleton
class TodoRepository @Inject() (cassandraConnection: CassandraConnection) extends LoggingComponent {

  import cassandraConnection._

  sealed class Todos extends CassandraTable[Todos, Todo] {

    object userId extends LongColumn[Todos, Todo](this) with PartitionKey[Long]

    object id extends TimeUUIDColumn[Todos, Todo](this) with PrimaryKey[UUID]
    object done extends BooleanColumn[Todos, Todo](this) with PrimaryKey[Boolean]
    object name extends StringColumn[Todos, Todo](this)

    override def fromRow(r: Row): Todo = Todo(Some(id(r)), name(r), done(r))
  }

  object Todos extends Todos

  logger.info(Todos.create.queryString)

  def findUndoneByUser(userId: Long, limit: Int): Future[List[Todo]] = {
    Todos.select.where(_.userId eqs userId).limit(limit).fetch()
  }

  def addOrUpdate(userId: Long, todo: Todo): Future[Todo] = {
    todo.id match {
      case None =>
        addOrUpdate(userId,
          todo.copy(id = Some(newTimeUUID)))

      case Some(id) =>
        Todos.insert()
          .value(_.userId, userId)
          .value(_.id, id)
          .value(_.name, todo.name)
          .value(_.done, todo.done)
          .future().map(_ => todo)
    }
  }
}
