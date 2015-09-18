package com.softwaremill.streams

import akka.stream.scaladsl.FlexiRoute.{DemandFromAll, RouteLogic}
import akka.stream.{Attributes, UniformFanOutShape}
import akka.stream.scaladsl.FlexiRoute
import com.softwaremill.streams.complete.util.Timed._

trait ParallelProcessing {
  def run(in: List[Int]): List[Int]
}

//
// SCALAZ
//

//
// AKKA
//
class SplitRoute[T](splitFn: T => Either[T, T]) extends FlexiRoute[T, UniformFanOutShape[T, T]](new UniformFanOutShape(2), Attributes.name("SplitRoute")) {

  override def createRouteLogic(s: UniformFanOutShape[T, T]) = new RouteLogic[T] {
    override def initialState = State[Unit](DemandFromAll(s.out(0), s.out(1))) { (ctx, _, el) =>
      splitFn(el) match {
        case Left(e) => ctx.emit(s.out(0))(e)
        case Right(e) => ctx.emit(s.out(1))(e)
      }
      SameState
    }

    override def initialCompletionHandling = eagerClose
  }
}

//
// RUNNER
//

object ParallelProcessingRunner extends App {
  val impls = List[(String, ParallelProcessing)](
//    ("scalaz", ScalazStreamsParallelProcessing),
//    ("akka", AkkaStreamsParallelProcessing)
  )

  for ((name, impl) <- impls) {
    val (r, time) = timed { impl.run(List(1, 2, 3, 4, 5)) }
    println(f"$name%-10s $r%-35s ${time/1000.0d}%4.2fs")
  }
}
