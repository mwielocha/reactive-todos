package components

import javax.inject.{Inject, Singleton}

import com.datastax.driver.core.Cluster
import com.websudos.phantom.dsl._
import logging.LoggingComponent
import play.api.Configuration

@Singleton
class CassandraConnection @Inject() (configuration: Configuration) extends LoggingComponent {

  private val contactPoints = configuration
    .getString("cassandra.cluster.seeds")
    .getOrElse("localhost")
    .split(",")

  private val keyspace = configuration
    .getString("cassandra.cluster.keyspace")
    .getOrElse("todos")

  logger.info(s"Connecting to ${contactPoints.mkString(", ")}...")

  val cluster: Cluster = Cluster.builder()
    .addContactPoints(contactPoints: _*)
    .build()

  implicit val session: Session = cluster.newSession()

  implicit val keySpace = KeySpace(keyspace)
}
