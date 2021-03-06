import scala.quoted._

object Macros {

  inline def dealias[T]: String = ${ impl[T] }

  def impl[T: Type](using Quotes) : Expr[String] = {
    import qctx.reflect._
    Expr(TypeRepr.of[T].dealias.show)
  }
}
