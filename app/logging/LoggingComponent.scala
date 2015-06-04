package logging

/**
 * author mikwie
 *
 */
trait LoggingComponent extends Logging {

  logger.info(s"*** [CREATE] - ${getClass.getSimpleName}")

}
