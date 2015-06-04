package uuid

/**
 * Created by mwielocha on 04/06/15.
 */
object UUIDHelper {

  def newTimeUUID = {
    java.util.UUID.fromString {
      new com.eaio.uuid.UUID().toString
    }
  }
}
