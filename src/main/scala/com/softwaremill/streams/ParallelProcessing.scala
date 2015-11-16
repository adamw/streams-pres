package com.softwaremill.streams

import akka.actor.ActorSystem
import akka.stream.stage.{OutHandler, InHandler, GraphStageLogic, GraphStage}
import akka.stream._
import akka.stream.scaladsl._
import akka.stream.scaladsl.FlowGraph.Implicits._

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

    var pending: Option[(T, Outlet[T])] = None
    var initialized = false

    setHandler(in, new InHandler {
      override def onPush() = {
        val elAndOut = splitFn(grab(in)).fold((_, out0), (_, out1))
        pending = Some(elAndOut)
        tryPush()
      }

      override def onUpstreamFinish() = {
        if (pending.isEmpty) {
          completeStage()
        }
      }
    })

    List(out0, out1).foreach {
      setHandler(_, new OutHandler {
        override def onPull() = {
          if (!initialized) {
            initialized = true
            tryPull(in)
          }

          tryPush()
        }
      })
    }

    private def tryPush(): Unit = {
      pending.foreach { case (el, out) =>
        if (isAvailable(out)) {
          push(out, el)
          tryPull(in)
          pending = None

          if (isClosed(in)) {
            completeStage()
          }
        }
      }
    }
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
