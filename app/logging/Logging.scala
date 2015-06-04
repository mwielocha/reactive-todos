package logging

import play.api.Logger

/**
 * author mikwie
 *
 */
trait Logging {

  lazy val logger: play.api.Logger = Logger(getClass)

}
