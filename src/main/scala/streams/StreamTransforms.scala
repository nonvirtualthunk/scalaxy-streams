package scalaxy.streams

private[streams] trait StreamTransforms
  extends Streams
  with StreamSources
  with StreamSinks
  with StreamOps
  with Strategies
  with Reporters
{
  import global._

  /**
   * Transforms the a stream if it can, or returns None if it can't.
   *
   * Recurses in to stream's subTrees with recur.
   */
  def transformStream(tree: Tree,
                      strategy: OptimizationStrategy,
                      fresh: String => String,
                      currentOwner: Symbol,
                      recur: Tree => Tree,
                      typecheck: Tree => Tree): Option[Tree]
  = tree match {
    case tree @ SomeStream(stream) if !hasKnownLimitationOrBug(stream) =>
      if (isWorthOptimizing(stream, strategy)) {
        // println(s"stream = $stream")

        info(
          tree.pos,
          Optimizations.optimizedStreamMessage(stream.describe(), strategy),
          force = impl.verbose)

        try {
          val result: Tree = stream
            .emitStream(
              n => TermName(fresh(n)),
              recur,
              currentOwner = currentOwner,
              typed = typecheck)
            .compose(typecheck)

          if (impl.debug) {
            info(
              tree.pos,
              Optimizations.messageHeader + s"Result for ${stream.describe()}:\n$result",
              force = impl.verbose)
          }
          Some(result)

        } catch {
          case ex: Throwable =>
            logException(tree.pos, ex, warning)
            None
        }
      } else {
        if (impl.veryVerbose && !stream.isDummy && !impl.quietWarnings) {
          info(
            tree.pos,
            Optimizations.messageHeader + s"Stream ${stream.describe()} is not worth optimizing with strategy $strategy",
            force = impl.verbose)
        }
        None
      }

    case _ =>
      None
  }
}
