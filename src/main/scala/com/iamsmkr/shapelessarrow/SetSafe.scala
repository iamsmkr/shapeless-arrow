package com.iamsmkr.shapelessarrow

import org.apache.arrow.vector._
import org.apache.arrow.vector.complex.ListVector
import org.apache.arrow.vector.complex.impl.UnionListWriter
import shapeless.{::, Generic, HList, HNil}

trait SetSafe[T, R] {
  def setSafe(vector: T, row: Int, value: R): Unit
}

object SetSafe {
  def apply[T, R](implicit derivative: SetSafe[T, R]): SetSafe[T, R] =
    derivative

  def instance[T, R](func: (T, Int, R) => Unit): SetSafe[T, R] =
    new SetSafe[T, R] {
      override def setSafe(vector: T, row: Int, value: R): Unit =
        func(vector, row, value)
    }

  implicit class FetchWriter[A <: ListVector](value: A) {
    def getListVectorWriter(vector: ListVector): UnionListWriter = {
      listVectorToWriter.putIfAbsent(vector, value.getWriter)
      listVectorToWriter.get(vector)
    }
  }

  implicit val intVectorSetSafe: SetSafe[IntVector, Int] =
    SetSafe.instance[IntVector, Int] { case (vector, row, value) => vector.setSafe(row, value) }

  implicit val bigIntVectorSetSafe: SetSafe[BigIntVector, Long] =
    SetSafe.instance[BigIntVector, Long] { case (vector, row, value) => vector.setSafe(row, value) }

  implicit val varCharVectorSetSafe: SetSafe[VarCharVector, String] =
    SetSafe.instance[VarCharVector, String] { case (vector, row, value) => vector.setSafe(row, value.getBytes) }

  implicit val bitVectorSetSafe: SetSafe[BitVector, Boolean] =
    SetSafe.instance[BitVector, Boolean] { case (vector, row, value) => vector.setSafe(row, if (value) 1 else 0) }

  implicit val listVectorSetSafe: SetSafe[ListVector, List[Int]] =
    SetSafe.instance[ListVector, List[Int]] { case (vector, row, value) =>
      val writer = vector.getListVectorWriter(vector)
      writer.startList()
      writer.setPosition(row)
      for (j <- value.indices) writer.writeInt(value(j))
      writer.endList()
    }

  implicit val boollistVectorSetSafe: SetSafe[ListVector, List[Boolean]] =
    SetSafe.instance[ListVector, List[Boolean]] { case (vector, row, value) =>
      val writer = vector.getListVectorWriter(vector)
      writer.startList()
      writer.setPosition(row)
      for (j <- value.indices) {
        if (value(j)) writer.writeBit(1)
        else writer.writeBit(0)
      }
      writer.endList()
    }

  implicit def hNilSetSafe: SetSafe[HNil, HNil] =
    SetSafe.instance[HNil, HNil] { case (vector, row, value) => () }

  implicit def hListSetSafe[H, T <: HList, P, Q <: HList](
                                                           implicit
                                                           hSetSafe: SetSafe[H, P],
                                                           tSetSafe: SetSafe[T, Q]
                                                         ): SetSafe[H :: T, P :: Q] =
    SetSafe.instance { case (h :: t, row, p :: q) =>
      hSetSafe.setSafe(h, row, p)
      tSetSafe.setSafe(t, row, q)
    }

  implicit def genericSetSafe[A, ARepr <: HList, B, BRepr <: HList](
                                                                     implicit
                                                                     genA: Generic.Aux[A, ARepr],
                                                                     genB: Generic.Aux[B, BRepr],
                                                                     derivative: SetSafe[ARepr, BRepr]
                                                                   ): SetSafe[A, B] = {
    SetSafe.instance { case (vectors, row, values) =>
      derivative.setSafe(genA.to(vectors), row, genB.to(values))
    }
  }

}
