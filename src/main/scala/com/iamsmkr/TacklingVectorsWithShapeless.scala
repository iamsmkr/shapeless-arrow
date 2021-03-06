package com.iamsmkr

import com.iamsmkr.shapelessarrow._
import org.apache.arrow.memory._
import org.apache.arrow.vector._
import org.apache.arrow.vector.complex.ListVector
import org.apache.arrow.vector.types.pojo._

import java.util
import scala.jdk.CollectionConverters._

object TacklingVectorsWithShapeless extends App {

  private val allocator: BufferAllocator = new RootAllocator()

  private val schema: Schema =
    new Schema(List(
      new Field("ints", new FieldType(false, new ArrowType.Int(32, true), null), null),
      new Field("longs", new FieldType(false, new ArrowType.Int(64, true), null), null),
      new Field("strs", new FieldType(false, new ArrowType.Utf8(), null), null),
      new Field("bools", new FieldType(false, new ArrowType.Bool(), null), null),
      new Field("list", FieldType.notNullable(ArrowType.List.INSTANCE),
        util.Arrays.asList(new Field("intelems", FieldType.notNullable(new ArrowType.Int(32, true)), null))),
      new Field("boollist", FieldType.notNullable(ArrowType.List.INSTANCE),
        util.Arrays.asList(new Field("boolelems", FieldType.notNullable(ArrowType.Bool.INSTANCE), null)))
    ).asJava)

  private val vectorSchemaRoot = VectorSchemaRoot.create(schema, allocator)

  val ints = vectorSchemaRoot.getVector("ints").asInstanceOf[IntVector]
  val longs = vectorSchemaRoot.getVector("longs").asInstanceOf[BigIntVector]
  val strs = vectorSchemaRoot.getVector("strs").asInstanceOf[VarCharVector]
  val bools = vectorSchemaRoot.getVector("bools").asInstanceOf[BitVector]
  val list = vectorSchemaRoot.getVector("list").asInstanceOf[ListVector]
  val boollist = vectorSchemaRoot.getVector("boollist").asInstanceOf[ListVector]

  case class MixArrowFlightMessage(
                                    int: Int = 0,
                                    long: Long = 0L,
                                    str: String = "",
                                    bool: Boolean = false,
                                    list: List[Int] = Nil,
                                    boolist: List[Boolean] = Nil
                                  )

  case class MixArrowFlightMessageVectors(
                                           ints: IntVector,
                                           longs: BigIntVector,
                                           strs: VarCharVector,
                                           bools: BitVector,
                                           list: ListVector,
                                           boollist: ListVector
                                         )

  private val vectors =
    MixArrowFlightMessageVectors(
      ints,
      longs,
      strs,
      bools,
      list,
      boollist
    )

  private val mixMessage =
    MixArrowFlightMessage(
      900,
      2000L,
      "One",
      true,
      List(1, 2, 3, 4, 5),
      List(true, false)
    )

  // allocate new buffers
  AllocateNew[MixArrowFlightMessageVectors].allocateNew(vectors)

  // set values to vectors
  SetSafe[MixArrowFlightMessageVectors, MixArrowFlightMessage].setSafe(vectors, 0, mixMessage)

  SetSafe[MixArrowFlightMessageVectors, MixArrowFlightMessage].setSafe(vectors, 1,
    MixArrowFlightMessage(
      1000,
      3000L,
      "Two",
      false,
      List(11, 12, 13, 14, 15),
      List(false, true)
    ))

  // set value count
  SetValueCount[MixArrowFlightMessageVectors].setValueCount(vectors, 2)

  // check if values are set against a given row
  assert(IsSet[MixArrowFlightMessageVectors].isSet(vectors, 0).forall(_ == 1))

  // get values against a row
  val encoded = Get[MixArrowFlightMessageVectors, MixArrowFlightMessage].invokeGet(vectors, 0)
  assert(encoded == mixMessage)

  println(encoded)
  println(Get[MixArrowFlightMessageVectors, MixArrowFlightMessage].invokeGet(vectors, 1))

  // close resources
  Close[MixArrowFlightMessageVectors].close(vectors)
  vectorSchemaRoot.close()

}
