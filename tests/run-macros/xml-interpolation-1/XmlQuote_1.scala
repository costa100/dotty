import scala.quoted._


import scala.language.implicitConversions

case class Xml(parts: String, args: List[Any])

object XmlQuote {

  implicit class SCOps(ctx: StringContext) {
    inline def xml(args: => Any*): Xml = ${XmlQuote.impl('this, 'args)}
  }

  def impl(receiver: Expr[SCOps], args: Expr[Seq[Any]])
          (using Quotes) : Expr[Xml] = {
    import qctx.reflect._

    // for debugging purpose
    def pp(tree: Tree): Unit = {
      println(tree.showExtractors)
      println(tree.show)
    }

    def liftListOfAny(lst: List[Term]): Expr[List[Any]] = lst match {
      case x :: xs  =>
        val head = x.asExpr
        val tail = liftListOfAny(xs)
        '{ $head :: $tail }
      case Nil => '{Nil}
    }

    def isStringConstant(tree: Term) = tree match {
      case Literal(_) => true
      case _ => false
    }

    def isSCOpsConversion(tree: Term) =
      tree.symbol.fullName == "XmlQuote$.SCOps"

    def isStringContextApply(tree: Term) =
      tree.symbol.fullName == "scala.StringContext$.apply"

    // XmlQuote.SCOps(StringContext.apply([p0, ...]: String*)
    val parts = Term.of(receiver).underlyingArgument match {
      case Apply(conv, List(Apply(fun, List(Typed(Repeated(values, _), _)))))
          if isSCOpsConversion(conv) &&
             isStringContextApply(fun) &&
             values.forall(isStringConstant) =>
        values.collect { case Literal(Constant.String(value)) => value }
      case tree =>
        report.error(s"String literal expected, but ${tree.showExtractors} found")
        return '{ ??? }
    }

    // [a0, ...]: Any*
    val Typed(Repeated(args0, _), _) = Term.of(args).underlyingArgument

    val string = parts.mkString("??")
    '{new Xml(${Expr(string)}, ${liftListOfAny(args0)})}
  }
}
