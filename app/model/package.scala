import java.util.UUID

import play.api.libs.json.Json

/**
 * Created by mwielocha on 04/06/15.
 */
package object model {

  case class Todo(id: Option[UUID], name: String, done: Boolean = false)

  object Todo {

    implicit val reads = Json.reads[Todo]
    implicit val writes = Json.writes[Todo]

  }
}
