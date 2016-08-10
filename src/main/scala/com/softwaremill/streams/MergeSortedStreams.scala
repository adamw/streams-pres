package com.softwaremill.streams

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl.{RunnableGraph, Sink, Source, GraphDSL}
import akka.stream.scaladsl.GraphDSL.Implicits._
import akka.stream.stage.{InHandler, GraphStageLogic, GraphStage}
import org.scalacheck.{Prop, Gen, Properties}

import scala.concurrent.Await
import scalaz.stream.{Tee, tee, Process}

import scala.concurrent.duration._

trait MergeSortedStreams {
  def merge[T: Ordering](l1: List[T], l2: List[T]): List[T]
}

//
// AKKA
//
class SortedMerge[T: Ordering] extends GraphStage[FanInShape2[T, T, T]] {
  private val left = Inlet[T]("left")
  private val right = Inlet[T]("right")
  private val out = Outlet[T]("out")

  override val shape = new FanInShape2(left, right, out)

  override def createLogic(attr: Attributes) = new GraphStageLogic(shape) {
    import Ordering.Implicits._

    setHandler(left, ignoreTerminateInput)
    setHandler(right, ignoreTerminateInput)
    setHandler(out, eagerTerminateOutput)

    def dispatch(l: T, r: T): Unit =
      if (l < r) {
        emit(out, l, () => readL(r))
      } else {
        emit(out, r, () => readR(l))
      }

    def emitAndPass(in: Inlet[T], other: T) =
      () => emit(out, other, () => pullAndPassAlong(in, out))

    def readL(other: T) = readAndThen(left)(dispatch(_, other))(emitAndPass(right, other))
    def readR(other: T) = readAndThen(right)(dispatch(other, _))(emitAndPass(left, other))

    override def preStart() = readAndThen(left)(readR){ () =>
      pullAndPassAlong(right, out)
    }

    // helper methods
    def pullAndPassAlong[Out, In <: Out](from: Inlet[In], to: Outlet[Out]): Unit = {
      if (!isClosed(from)) {
        if (!hasBeenPulled(from)) pull(from)
        passAlong(from, to, doFinish = true, doFail = true)
      } else {
        completeStage()
      }
    }

    def readAndThen[U](in: Inlet[U])(andThen: U => Unit)(onFinish: () => Unit): Unit = {
      if (isClosed(in)) {
        onFinish()
      } else {
        val previous = getHandler(in)
        read(in)({ t =>
          setHandler(in, previous)
          andThen(t)
        }, onFinish)
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

object MergeSortedStreamsRunner extends Properties("MergeSortedStreams") {
  val sortedList = Gen.listOf(Gen.choose(0, 20)).map(_.sorted)

  import Prop._

  def addPropertyFor(name: String, mss: MergeSortedStreams): Unit = {
    property(s"merge-$name") = forAll(sortedList, sortedList) { (l1: List[Int], l2: List[Int]) =>
      val result   = mss.merge(l1, l2)
      val expected = (l1 ++ l2).sorted
      result == expected
    }
  }

  //addPropertyFor("scalaz", ScalazStreamsMergeSortedStreams)
  //addPropertyFor("akka", AkkaStreamsMergeSortedStreams)
}
