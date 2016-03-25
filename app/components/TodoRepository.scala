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

  sealed class Todos extends CassandraTable[Todos, Todo] {

    object userId extends LongColumn[Todos, Todo](this)
      with PartitionKey[Long]

    object id extends TimeUUIDColumn[Todos, Todo](this)
      with PrimaryKey[UUID]
      with ClusteringOrder[UUID]
      with Descending

    object done extends BooleanColumn[Todos, Todo](this)

    object name extends StringColumn[Todos, Todo](this)

    override def fromRow(r: Row): Todo = Todo(Some(id(r)), name(r), done(r))
  }

  object Todos extends Todos

  logger.info(Todos.create.queryString)

  def findAll(userId: Long, limit: Int): Future[List[Todo]] = {
    Todos.select
      .where(_.userId eqs userId)
      .orderBy(_.id.desc)
      .limit(limit)
      .fetch()
  }

  def find(userId: Long, id: UUID): Future[Option[Todo]] = {
    Todos.select
      .where(_.userId eqs userId)
      .and(_.id eqs id)
      .one
  }

  def delete(userId: Long, id: UUID): Future[UUID] = {
    Todos.delete
      .where(_.userId eqs userId)
      .and(_.id eqs id)
      .future().map(_ => id)
  }
  
  def addOrUpdate(userId: Long, todo: Todo): Future[Todo] = {

    val id = todo.id.getOrElse(newTimeUUID)

    Todos.insert()
      .value(_.userId, userId)
      .value(_.id, id)
      .value(_.name, todo.name)
      .value(_.done, todo.done)
      .future().map(_ => todo.copy(id = Some(id)))
  }
}
