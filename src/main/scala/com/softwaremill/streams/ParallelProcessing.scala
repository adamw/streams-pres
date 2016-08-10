package com.softwaremill.streams

import akka.actor.ActorSystem
import akka.stream.stage.{GraphStageLogic, GraphStage}
import akka.stream._
import akka.stream.scaladsl._
import akka.stream.scaladsl.GraphDSL.Implicits._

import scala.concurrent.Await
import scala.concurrent.duration._
import scalaz.\/-
import scalaz.concurrent.Task
import scalaz.stream.{wye, async, Process}
import com.softwaremill.streams.complete.util.Timed._

trait ParallelProcessing {
  def run(in: List[Int]): List[Int]
}

//
// AKKA
//
class SplitStage[T](splitFn: T => Either[T, T]) extends GraphStage[FanOutShape2[T, T, T]] {

  val in   = Inlet[T]("SplitStage.in")
  val out0 = Outlet[T]("SplitStage.out0")
  val out1 = Outlet[T]("SplitStage.out1")

  override def shape = new FanOutShape2[T, T, T](in, out0, out1)

  override def createLogic(inheritedAttributes: Attributes) = new GraphStageLogic(shape) {

    setHandler(in, ignoreTerminateInput)
    setHandler(out0, eagerTerminateOutput)
    setHandler(out1, eagerTerminateOutput)

    def doRead(): Unit = {
      if (isClosed(in)) {
        completeStage()
      } else {
        setHandler(in, eagerTerminateInput)
        read(in)({ el =>
          setHandler(in, ignoreTerminateInput)
          splitFn(el).fold(doEmit(out0, _), doEmit(out1, _))
        }, () => ())
      }
    }

    def doEmit(out: Outlet[T], el: T): Unit = emit(out, el, doRead _)

    override def preStart() = doRead()
  }
}

//
// SCALAZ
//

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
