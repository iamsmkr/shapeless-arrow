package com.iamsmkr.shapelessarrow

import org.apache.arrow.vector._
import shapeless.{::, Generic, HList, HNil}

trait AllocateNew[T] {
  def allocateNew(vector: T): Unit
}

object AllocateNew {
  def apply[T](implicit derivative: AllocateNew[T]): AllocateNew[T] =
    derivative

  def instance[T](func: T => Unit): AllocateNew[T] =
    new AllocateNew[T] {
      override def allocateNew(vector: T): Unit = func(vector)
    }

  implicit val intVectorAllocateNew: AllocateNew[IntVector] =
    AllocateNew.instance[IntVector](_.allocateNew())

  implicit val bigIntVectorAllocateNew: AllocateNew[BigIntVector] =
    AllocateNew.instance[BigIntVector](_.allocateNew())

  implicit val varCharVectorAllocateNew: AllocateNew[VarCharVector] =
    AllocateNew.instance[VarCharVector](_.allocateNew())

  implicit val bitVectorAllocateNew: AllocateNew[BitVector] =
    AllocateNew.instance[BitVector](_.allocateNew())

  implicit def hNilAllocateNew: AllocateNew[HNil] =
    AllocateNew.instance[HNil](_ => ())

  implicit def hListAllocateNew[H, T <: HList](
                                                implicit
                                                hAllocateNew: AllocateNew[H],
                                                tAllocateNew: AllocateNew[T]
                                              ): AllocateNew[H :: T] =
    AllocateNew.instance { case h :: t =>
      hAllocateNew.allocateNew(h)
      tAllocateNew.allocateNew(t)
    }

  implicit def genericAllocateNew[A, R](
                                         implicit
                                         gen: Generic.Aux[A, R],
                                         derivative: AllocateNew[R]
                                       ): AllocateNew[A] = {
    AllocateNew.instance { adt =>
      derivative.allocateNew(gen.to(adt))
    }
  }
}
