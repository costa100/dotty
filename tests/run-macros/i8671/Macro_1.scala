import scala.quoted._

case class FileName private(name: String)

object FileName {
  def unsafe(s: String) = new FileName(s)

  implicit inline def ToFileName(inline s: String): FileName =
    ${createFileName('{s})}

  def fileNameFromString(s: String): Either[String, FileName] =
    Right(FileName.unsafe(s))

  def createFileName(fileName: Expr[String])(using Quotes): Expr[FileName] =
    fileName match {
      case e@Const(s) =>
        fileNameFromString(s) match {
            case Right(fn) =>
              '{FileName.unsafe(${Expr(fn.name)})} // Or `Expr(fn)` if there is a `Liftable[FileName]`
            case Left(_) =>
              report.throwError(s"$s is not a valid file name! It must not contain a /", fileName)
         }

      case _ =>
        report.throwError(s"$fileName is not a valid file name. It must be a literal string", fileName)
    }
}

