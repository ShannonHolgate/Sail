package models

import com.novus.salat._
import play.api.Play
import play.api.Play.current

/**
 * Created with IntelliJ IDEA.
 * User: Shannon
 * Date: 05/11/13
 * Time: 22:42
 */
package object MongoContext {
  implicit val context = {
    val context = new Context {
      val name = "global"
      override val typeHintStrategy = StringTypeHintStrategy(when = TypeHintFrequency.WhenNecessary, typeHint = "_t")
    }
    context.registerGlobalKeyOverride(remapThis = "id", toThisInstead = "_id")
    context.registerClassLoader(Play.classloader)
    context
  }
}