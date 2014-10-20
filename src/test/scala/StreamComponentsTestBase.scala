package scalaxy.streams
package test

import org.junit._
import org.junit.Assert._

import scala.collection.mutable.ListBuffer
import scala.tools.reflect.ToolBox
import scala.tools.reflect.FrontEnd

case class CompilerMessages(
  infos: List[String] = Nil,
  warnings: List[String] = Nil,
  errors: List[String] = Nil)

trait StreamComponentsTestBase extends Utils
{
  val global = scala.reflect.runtime.universe
  val commonOptions = "-usejavacp -optimise -Yclosure-elim -Yinline "//-Ybackend:GenBCode"
  import scala.reflect.runtime.currentMirror

  def typecheck(t: global.Tree): global.Tree = {
    val toolbox = currentMirror.mkToolBox(options = commonOptions)
    toolbox.typecheck(t.asInstanceOf[toolbox.u.Tree]).asInstanceOf[global.Tree]
  }

  def compile(source: String): (() => Any, CompilerMessages) = {
    val infosBuilder = ListBuffer[String]()
    val warningsBuilder = ListBuffer[String]()
    val errorsBuilder = ListBuffer[String]()
    val frontEnd = new FrontEnd {
      override def display(info: Info) {
        val builder: ListBuffer[String] = info.severity match {
          case INFO => infosBuilder
          case WARNING => warningsBuilder
          case ERROR => errorsBuilder
        }

        builder += info.msg
      }
      override def interactive() {}
    }
    val toolbox = currentMirror.mkToolBox(frontEnd = frontEnd, options = commonOptions)
    import toolbox.u._

    try {
      val tree = toolbox.parse(source);
      val compilation = toolbox.compile(tree)

      (
        compilation,
        CompilerMessages(
          infos = infosBuilder.result,
          warnings = warningsBuilder.result,
          errors = errorsBuilder.result)
      )
    } catch { case ex: Throwable =>
      throw new RuntimeException(s"Failed to compile:\n$source", ex)
    }
  }

  def optimizedCode(source: String, strategy: OptimizationStrategy): String = {
    val src = s"{ import ${strategy.fullName} ; scalaxy.streams.optimize { $source } }"
    // println(src)
    src
  }

  def testMessages(source: String, expectedMessages: CompilerMessages)
                  (implicit strategy: OptimizationStrategy = scalaxy.streams.strategy.aggressive) {

    def filterWarnings(warnings: List[String]) =
      warnings.filterNot(_ == "a pure expression does nothing in statement position; you may be omitting necessary parentheses")

    val actualMessages = try {
      assertMacroCompilesToSameValue(
        source,
        strategy = strategy)
    } catch { case ex: Throwable =>
      ex.printStackTrace()
      if (ex.getCause != null)
        ex.getCause.printStackTrace()
      throw new RuntimeException(ex)
    }

    assertEquals(expectedMessages.infos, actualMessages.infos)
    assertEquals(filterWarnings(expectedMessages.warnings).toSet,
      filterWarnings(actualMessages.warnings).toSet)
    assertEquals(expectedMessages.errors, actualMessages.errors)
  }

  def assertPluginCompilesSnippetFine(source: String) {
    val sourceFile = {
      import java.io._

      val f = File.createTempFile("test-", ".scala")
      val out = new PrintStream(f)
      out.println(s"object Test { $source }")
      out.close()

      f
    }

    val args = Array(sourceFile.toString)
    StreamsCompiler.compile(args, StreamsCompiler.consoleReportGetter)
  }

  def assertMacroCompilesToSameValue(source: String, strategy: OptimizationStrategy): CompilerMessages = {
    val (unoptimized, unoptimizedMessages) = compile(source)
    val (optimized, optimizedMessages) = compile(optimizedCode(source, strategy))

    assertEqualValues(source, unoptimized(), optimized())

    assertEquals("Unexpected messages during unoptimized compilation",
      CompilerMessages(), unoptimizedMessages)

    optimizedMessages
  }

  def assertEqualValues(message: String, expected: Any, actual: Any) = {
    (expected, actual) match {
      case (expected: Array[Int], actual: Array[Int]) =>
        assertArrayEquals(message, expected, actual)
      case (expected: Array[Short], actual: Array[Short]) =>
        assertArrayEquals(message, expected, actual)
      case (expected: Array[Byte], actual: Array[Byte]) =>
        assertArrayEquals(message, expected, actual)
      case (expected: Array[Char], actual: Array[Char]) =>
        assertArrayEquals(message, expected, actual)
      case (expected: Array[Float], actual: Array[Float]) =>
        assertArrayEquals(message, expected, actual, 0)
      case (expected: Array[Double], actual: Array[Double]) =>
        assertArrayEquals(message, expected, actual, 0)
      case (expected: Array[Boolean], actual: Array[Boolean]) =>
        assertArrayEquals(message, expected.map(_.toString: AnyRef), actual.map(_.toString: AnyRef))
      case (expected, actual) =>
        assertEquals(message, expected, actual)
        assertEquals(message + " (different classes)",
          Option(expected).map(_.getClass).orNull,
          Option(actual).map(_.getClass).orNull)
    }
  }
}
