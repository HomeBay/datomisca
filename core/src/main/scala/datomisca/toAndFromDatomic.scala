/*
 * Copyright 2012 Pellucid and Zenexity
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package datomisca

import scala.annotation.implicitNotFound


/** Injective form of DatomicData to Scala converter :
  * - 1 DD => 1 Scala type
  * - used when precise type inference by compiler
  */
@implicitNotFound("There is no unique conversion from Datomic data type ${DD} to type ${A}")
sealed trait FromDatomicInj[DD <: AnyRef, A] {
  def from(dd: DD): A
}

object FromDatomicInj extends FromDatomicInjImplicits {
  def apply[DD <: AnyRef, A](f: DD => A) = new FromDatomicInj[DD, A]{
    def from(dd: DD): A = f(dd)
  }
}


/** Surjective for DatomicData to Scala converter :
  * - n DD => 1 Scala type
  */
@implicitNotFound("There is no conversion from Datomic data type ${DD} to type ${A}. Consider implementing an instance of the FromDatomic type class.")
trait FromDatomic[DD <: AnyRef, A] {
  def from(dd: DD): A
}

object FromDatomic extends FromDatomicImplicits {
  def apply[DD <: AnyRef, A](f: DD => A) = new FromDatomic[DD, A]{
    def from(dd: DD): A = f(dd)
  }
}

/** Generic DatomicData to Scala type 
  * Multi-valued "function" (not real function actually) 
  * which inverse is surjective ToDatomic or ToDatomicCast
  * 1 DatomicData -> n Scala type
  */
@implicitNotFound("There is no cast available from Datomic data to type ${A}")
trait FromDatomicCast[A] {
  def from(dd: AnyRef): A
}

object FromDatomicCast extends FromDatomicCastImplicits {
  def apply[A](f: AnyRef => A) = new FromDatomicCast[A] {
    def from(dd: AnyRef): A = f(dd)
  }
}

/** Injective form of Scala to Specific DatomicData converters
  * 1 Scala type => 1 DD
  */
@implicitNotFound("There is no unique conversion from type ${A} to Datomic data type ${DD}")
sealed trait ToDatomicInj[DD <: AnyRef, A] {
  def to(a: A): DD
}

object ToDatomicInj extends ToDatomicInjImplicits {
  def apply[DD <: AnyRef, A](f: A => DD) = new ToDatomicInj[DD, A] {
    def to(a: A) = f(a)
  }
}

/** Surjective form of Scala to Specific DatomicData converters
  * n Scala type => 1 DD
  */
@implicitNotFound("There is no conversion from type ${A} to Datomic data type ${DD}. Consider implementing an instance of the ToDatomic type class.")
trait ToDatomic[DD <: AnyRef, A] {
  def to(a: A): DD
}

object ToDatomic extends ToDatomicImplicits{
  def apply[DD <: AnyRef, A](f: A => DD) = new ToDatomic[DD, A] {
    def to(a: A) = f(a)
  }
}

/** Scala type to Generic DatomicData (surjective)
  * n Scala type -> DatomicData
  */
@implicitNotFound("There is no cast available from type ${A} to Datomic data")
trait ToDatomicCast[A] {
  def to(a: A): AnyRef
}

object ToDatomicCast extends ToDatomicCastImplicits {
  def apply[A](f: A => AnyRef) = new ToDatomicCast[A] {
    def to(a: A): AnyRef = f(a)
  }
}



import java.{lang => jl}
import java.math.{BigInteger => JBigInt, BigDecimal => JBigDecimal}
import java.{util => ju}
import java.util.{Date, UUID}
import java.net.URI
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZonedDateTime

/**
  * Think of FromDatomicInj[DD, T] as a type-level function: DD => T
  * The implicits here construct a multi-parameter type class,
  * and there is a functional dependency from DD to T: DD uniquely
  * determines T. In fact, this is an injective function, as there
  * is at most one FromDatomicInj for each DatomicData subtype, and each
  * map to distinct Scala/Java types. As a consequence, its inverse
  * is a partial function.
  */
private[datomisca] trait FromDatomicInjImplicits {

  implicit val DString2String:          FromDatomicInj[String,      String]      = FromDatomicInj(identity)
  implicit val DBoolean2Boolean:        FromDatomicInj[jl.Boolean,  Boolean]     = FromDatomicInj(b => b)
  implicit val DLong2Long:              FromDatomicInj[jl.Long,     Long]        = FromDatomicInj(l => l)
  implicit val DDouble2Double:          FromDatomicInj[jl.Double,   Double]      = FromDatomicInj(d => d)
  implicit val DFloat2Float:            FromDatomicInj[jl.Float,    Float]       = FromDatomicInj(f => f)
  implicit val DBigInt2BigInt:          FromDatomicInj[JBigInt,     BigInt]      = FromDatomicInj(i => new BigInt(i))
  implicit val DBigDec2BigDec:          FromDatomicInj[JBigDecimal, BigDecimal]  = FromDatomicInj(d => new BigDecimal(d))
  implicit val DInstant2Date:           FromDatomicInj[Date,        Date]        = FromDatomicInj(identity)
  implicit val DUuid2UUID:              FromDatomicInj[UUID,        UUID]        = FromDatomicInj(identity)
  implicit val DUri2URI:                FromDatomicInj[URI,         URI]         = FromDatomicInj(identity)
  implicit val DBytes2Bytes:            FromDatomicInj[Array[Byte], Array[Byte]] = FromDatomicInj(identity)
  implicit val DKeyword2Keyword:        FromDatomicInj[Keyword, Keyword]         = FromDatomicInj(identity)

  implicit val entity2Entity: FromDatomicInj[datomic.Entity, Entity] = FromDatomicInj(e => new Entity(e))

}

/**
  * A multi-valued function, or relation, from DD => T,
  * So the type T is no longer uniquely determined by DD.
  * For example, DLong maps to DLong, Long, Int, Short,
  * Char, and Byte.
  */
trait FromDatomicImplicits {

  implicit def FromDatomicInj2FromDatomic[DD <: AnyRef, T]
      (implicit fd: FromDatomicInj[DD, T]): FromDatomic[DD, T] = 
      FromDatomic[DD, T](fd.from(_))

  implicit val DLong2Int:               FromDatomic[jl.Long,     Int]         = FromDatomic(_.toInt)
  implicit val DLong2Char:              FromDatomic[jl.Long,     Short]       = FromDatomic(_.toShort)
  implicit val DLong2Short:             FromDatomic[jl.Long,     Char]        = FromDatomic(_.toChar)
  implicit val DLong2Byte:              FromDatomic[jl.Long,     Byte]        = FromDatomic(_.toByte)
  implicit val DBigInt2JBigInt:         FromDatomic[JBigInt,     JBigInt]     = FromDatomic(identity)
  implicit val DBigDec2JBigDec:         FromDatomic[JBigDecimal, JBigDecimal] = FromDatomic(identity)
  implicit val Date2Instant:            FromDatomic[Date,        Instant]     = FromDatomic(_.toInstant)

  // implicit def DD2DD[DD <: DatomicData] = FromDatomic[DD, DD]( dd => dd )


  /**
    * Implicit to convert Datomic tuples, which are expressed simply as Java lists of `Object` proper Scala tuples. 
    * There is one of these implicits for every size tuple that datomic allows, which is 2-8. 
    *
    * @tparam A the type of the first element in the tuple
    * @tparam B the type of the second element in the tuple
    * @param conv1 converter to create the first element in the tuple
    * @param conv2 converter to create the second element in the tuple
    * 
    * @return a converter from type `java.util.List[AnyRef]` to a tuple of type `(A,B)`
    */
  implicit def javaListToTuple2[A,B](implicit conv1: FromDatomicCast[A], 
                                              conv2: FromDatomicCast[B]): FromDatomic[ju.List[AnyRef], (A,B)] = new FromDatomic[ju.List[AnyRef], (A,B)] {

    override def from(l: ju.List[AnyRef]) = (conv1.from(l.get(0)), conv2.from(l.get(1)))
  }

  /**
    * Implicit to convert Datomic tuples, which are expressed simply as Java lists of `Object` proper Scala tuples. 
    * There is one of these implicits for every size tuple that datomic allows, which is 2-8. 
    *
    * @tparam A the type of the first element in the tuple
    * @tparam B the type of the second element in the tuple
    * @tparam C the type of the third element in the tuple
    * @param conv1 converter to create the first element in the tuple
    * @param conv2 converter to create the second element in the tuple
    * @param conv3 converter to create the third element in the tuple
    *
    * @return a converter from type `java.util.List[AnyRef]` to a tuple of type `(A,B,C,D)`
    */
  implicit def javaListToTuple3[A,B,C](implicit conv1: FromDatomicCast[A], 
                                                conv2: FromDatomicCast[B],
                                                conv3: FromDatomicCast[C]): FromDatomic[ju.List[AnyRef], (A,B,C)] = new FromDatomic[ju.List[AnyRef], (A,B,C)] {

    override def from(l: ju.List[AnyRef]) = (conv1.from(l.get(0)), conv2.from(l.get(1)), conv3.from(l.get(2)))
  }

  implicit def javaListToTuple4[A,B,C,D](implicit conv1: FromDatomicCast[A], 
                                                  conv2: FromDatomicCast[B],
                                                  conv3: FromDatomicCast[C],
                                                  conv4: FromDatomicCast[D]): FromDatomic[ju.List[AnyRef], (A,B,C,D)] = new FromDatomic[ju.List[AnyRef], (A,B,C,D)] {

    override def from(l: ju.List[AnyRef]) = (conv1.from(l.get(0)), conv2.from(l.get(1)), conv3.from(l.get(2)), conv4.from(l.get(3)))
  }

  implicit def javaListToTuple5[A,B,C,D,E](implicit conv1: FromDatomicCast[A], 
                                                    conv2: FromDatomicCast[B],
                                                    conv3: FromDatomicCast[C],
                                                    conv4: FromDatomicCast[D], 
                                                    conv5: FromDatomicCast[E]): FromDatomic[ju.List[AnyRef], (A,B,C,D,E)] = new FromDatomic[ju.List[AnyRef], (A,B,C,D,E)] {

    override def from(l: ju.List[AnyRef]) = (conv1.from(l.get(0)), conv2.from(l.get(1)), conv3.from(l.get(2)), conv4.from(l.get(3)), conv5.from(l.get(4)))
  }

    implicit def javaListToTuple6[A,B,C,D,E,F](implicit conv1: FromDatomicCast[A], 
                                                        conv2: FromDatomicCast[B],
                                                        conv3: FromDatomicCast[C],
                                                        conv4: FromDatomicCast[D], 
                                                        conv5: FromDatomicCast[E],
                                                        conv6: FromDatomicCast[F]): FromDatomic[ju.List[AnyRef], (A,B,C,D,E,F)] = new FromDatomic[ju.List[AnyRef], (A,B,C,D,E,F)] {

    override def from(l: ju.List[AnyRef]) = (conv1.from(l.get(0)), 
                                             conv2.from(l.get(1)), 
                                             conv3.from(l.get(2)), 
                                             conv4.from(l.get(3)), 
                                             conv5.from(l.get(4)), 
                                             conv6.from(l.get(5)))
  }

  implicit def javaListToTuple7[A,B,C,D,E,F,G](implicit conv1: FromDatomicCast[A], 
                                                        conv2: FromDatomicCast[B],
                                                        conv3: FromDatomicCast[C],
                                                        conv4: FromDatomicCast[D], 
                                                        conv5: FromDatomicCast[E],
                                                        conv6: FromDatomicCast[F], 
                                                        conv7: FromDatomicCast[G]): FromDatomic[ju.List[AnyRef], (A,B,C,D,E,F,G)] = new FromDatomic[ju.List[AnyRef], (A,B,C,D,E,F,G)] {

    override def from(l: ju.List[AnyRef]) = (conv1.from(l.get(0)), 
                                             conv2.from(l.get(1)), 
                                             conv3.from(l.get(2)), 
                                             conv4.from(l.get(3)), 
                                             conv5.from(l.get(4)), 
                                             conv6.from(l.get(5)), 
                                             conv7.from(l.get(6)))
  }

  implicit def javaListToTuple8[A,B,C,D,E,F,G,H](implicit conv1: FromDatomicCast[A], 
                                                          conv2: FromDatomicCast[B],
                                                          conv3: FromDatomicCast[C],
                                                          conv4: FromDatomicCast[D], 
                                                          conv5: FromDatomicCast[E],
                                                          conv6: FromDatomicCast[F], 
                                                          conv7: FromDatomicCast[G], 
                                                          conv8: FromDatomicCast[H]): FromDatomic[ju.List[AnyRef], (A,B,C,D,E,F,G,H)] = new FromDatomic[ju.List[AnyRef], (A,B,C,D,E,F,G,H)] {

    override def from(l: ju.List[AnyRef]) = (conv1.from(l.get(0)), 
                                             conv2.from(l.get(1)), 
                                             conv3.from(l.get(2)), 
                                             conv4.from(l.get(3)), 
                                             conv5.from(l.get(4)), 
                                             conv6.from(l.get(5)), 
                                             conv7.from(l.get(6)), 
                                             conv8.from(l.get(7)))
  }



  implicit def JavaSetToScalaSet[T](implicit conv: FromDatomicCast[T]): FromDatomic[ju.Set[AnyRef], Set[T]] = new FromDatomic[ju.Set[AnyRef], Set[T]] {
    override def from(l: ju.Set[AnyRef]) = {
      val builder = Set.newBuilder[T]
      val iter = l.iterator
      while (iter.hasNext) {
        builder += conv.from(iter.next())
      }
      builder.result
    }
  }

  implicit def JavaListToScalaSeq[T](implicit conv: FromDatomicCast[T]): FromDatomic[ju.List[AnyRef], Seq[T]] = new FromDatomic[ju.List[AnyRef], Seq[T]] {
    override def from(l: ju.List[AnyRef]) = {
      val builder = Seq.newBuilder[T]
      val iter = l.iterator
      while (iter.hasNext) {
        builder += conv.from(iter.next())
      }
      builder.result
    }
  }

}

/**
  * FromDatomicCast fixes the source type
  * of FromDatomic as DatomicData
  * Trivially, is a multi-valued function
  * from DatomicData => T
  */
trait FromDatomicCastImplicits {
  implicit def FromDatomic2FromDatomicCast[DD <: AnyRef, A](implicit fdat: FromDatomic[DD, A]) =
    FromDatomicCast{ (dd: Any) => fdat.from(dd.asInstanceOf[DD]) }
}


/**
  * Think of ToDatomicInj[DD, T] as a type-level function: T => DD
  * The implicits here construct a multi-parameter type class,
  * and there is a functional dependency from T to DD: T uniquely
  * determines DD.  In fact, this is an injective function, as there
  * is at most one ToDatomicInj for any Scala type, and each
  * map to distinct DatomicData subtypes. As a consequence, its inverse
  * is a partial function.
  */
trait ToDatomicInjImplicits {
  implicit val String2DString   = ToDatomicInj[String,      String](identity)
  implicit val Boolean2DBoolean = ToDatomicInj[jl.Boolean,  Boolean](identity)
  implicit val Long2DLong       = ToDatomicInj[jl.Long,     Long](identity)
  implicit val Double2DDouble   = ToDatomicInj[jl.Double,   Double](identity)
  implicit val Float2DFloat     = ToDatomicInj[jl.Float,    Float](identity)
  implicit val BigInt2DBigInt   = ToDatomicInj[JBigInt,     BigInt]((i: BigInt) => i.bigInteger)
  implicit val BigDec2DBigDec   = ToDatomicInj[JBigDecimal, BigDecimal]((i: BigDecimal) => i.bigDecimal)
  implicit val Date2DDate       = ToDatomicInj[Date,        Date](identity)
  implicit val UUID2DUuid       = ToDatomicInj[UUID,        UUID](identity)
  implicit val URI2DUri         = ToDatomicInj[URI,         URI](identity)
  implicit val Bytes2DBytes     = ToDatomicInj[Array[Byte], Array[Byte]](identity)
  implicit val Keyword2DKeyword = ToDatomicInj[Keyword,     Keyword](identity)

}

/**
  * ToDatomic extends ToDatomicInj by widening the domain
  * and also destroying the injectivity property
  * (both Long and Int map to DLong)
  * But it is still a function (unlike FromDatomic)
  */
trait ToDatomicImplicits {
  implicit def ToDatomicInj2ToDatomic[DD <: AnyRef, T]
      (implicit tdat: ToDatomicInj[DD, T]): ToDatomic[DD, T] = 
      ToDatomic[DD, T](tdat.to(_))

  implicit val Int2DLong        = ToDatomic[jl.Long, Int](_.toLong)
  implicit val Short2DLong      = ToDatomic[jl.Long, Short](_.toLong)
  implicit val Char2DLong       = ToDatomic[jl.Long, Char](_.toLong)
  implicit val Byte2DLong       = ToDatomic[jl.Long, Byte](_.toLong)
  implicit val JBigInt2DBigInt  = ToDatomic[JBigInt,  JBigInt](identity)
  implicit val JBigDec2DBigDec  = ToDatomic[JBigDecimal,  JBigDecimal](identity)

  // Converters for java.time classes
  implicit val Instant2Date       = ToDatomic[Date, Instant](Date.from)
  implicit val OffsetDateTime2Date = ToDatomic[Date, OffsetDateTime](odt => Date.from(odt.toInstant))
  implicit val ZonedDateTime2Date  = ToDatomic[Date, ZonedDateTime](zdt => Date.from(zdt.toInstant))


  // Converters for the various tuple types. Datomic only supports  

  /**
    * Converts a Scala tuple to a Datomic tuple, which is expressed in the Java API simply as a 
    * `java.util.List[AnyRef]`. This function simultaniously converts each element in the list to its corresponding 
    * Datomic type, as well as wrapps everything up into a Java List.
    * 
    * Note that there is one of these implicit definitions for each sized tuple that Datomic suppors (2-8).
    *
    * @param conv1 needed to convert the first element in the tuple to its corresponding Datomic type
    * @param conv2 needed to convert the second element in the tuple to its corresponding Datomic type
    * 
    * @return the implicit converter
    */
  implicit def tuple2ToList[A,B](implicit conv1: ToDatomicCast[A], conv2: ToDatomicCast[B]) = new ToDatomic[ju.List[AnyRef], (A,B)] {

    override def to(c: (A,B)) = {

      datomic.Util.list(conv1.to(c._1), conv2.to(c._2)).asInstanceOf[ju.List[AnyRef]]
    }
  }

  implicit def tuple3ToList[A,B,C](implicit conv1: ToDatomicCast[A], 
                                            conv2: ToDatomicCast[B], 
                                            conv3: ToDatomicCast[C]) = new ToDatomic[ju.List[AnyRef], (A,B,C)] {

    override def to(c: (A,B,C)) = {

      datomic.Util.list(conv1.to(c._1), conv2.to(c._2), conv3.to(c._3)).asInstanceOf[ju.List[AnyRef]]
    }
  }

  implicit def tuple4ToList[A,B,C,D](implicit conv1: ToDatomicCast[A], 
                                            conv2: ToDatomicCast[B], 
                                            conv3: ToDatomicCast[C], 
                                            conv4: ToDatomicCast[D]) = new ToDatomic[ju.List[AnyRef], (A,B,C,D)] {

    override def to(c: (A,B,C,D)) = {

      datomic.Util.list(conv1.to(c._1), conv2.to(c._2), conv3.to(c._3), conv4.to(c._4)).asInstanceOf[ju.List[AnyRef]]
    }
  }

  implicit def tuple5ToList[A,B,C,D,E](implicit conv1: ToDatomicCast[A], 
                                            conv2: ToDatomicCast[B], 
                                            conv3: ToDatomicCast[C], 
                                            conv4: ToDatomicCast[D], 
                                            conv5: ToDatomicCast[E]) = new ToDatomic[ju.List[AnyRef], (A,B,C,D,E)] {

    override def to(c: (A,B,C,D,E)) = {

      datomic.Util.list(conv1.to(c._1), conv2.to(c._2), conv3.to(c._3), conv4.to(c._4), conv5.to(c._5)).asInstanceOf[ju.List[AnyRef]]
    }
  }

  implicit def tuple6ToList[A,B,C,D,E,F](implicit conv1: ToDatomicCast[A], 
                                            conv2: ToDatomicCast[B], 
                                            conv3: ToDatomicCast[C], 
                                            conv4: ToDatomicCast[D], 
                                            conv5: ToDatomicCast[E], 
                                            conv6: ToDatomicCast[F]) = new ToDatomic[ju.List[AnyRef], (A,B,C,D,E,F)] {

    override def to(c: (A,B,C,D,E,F)) = {

      datomic.Util.list(conv1.to(c._1), 
                        conv2.to(c._2), 
                        conv3.to(c._3), 
                        conv4.to(c._4), 
                        conv5.to(c._5), 
                        conv6.to(c._6)).asInstanceOf[ju.List[AnyRef]]
    }
  }

  implicit def tuple7ToList[A,B,C,D,E,F,G](implicit conv1: ToDatomicCast[A], 
                                            conv2: ToDatomicCast[B], 
                                            conv3: ToDatomicCast[C], 
                                            conv4: ToDatomicCast[D], 
                                            conv5: ToDatomicCast[E], 
                                            conv6: ToDatomicCast[F], 
                                            conv7: ToDatomicCast[G]) = new ToDatomic[ju.List[AnyRef], (A,B,C,D,E,F,G)] {

    override def to(c: (A,B,C,D,E,F,G)) = {

      datomic.Util.list(conv1.to(c._1), 
                        conv2.to(c._2), 
                        conv3.to(c._3), 
                        conv4.to(c._4), 
                        conv5.to(c._5), 
                        conv6.to(c._6), 
                        conv7.to(c._7)).asInstanceOf[ju.List[AnyRef]]
    }
  }

  implicit def tuple8ToList[A,B,C,D,E,F,G,H](implicit conv1: ToDatomicCast[A], 
                                            conv2: ToDatomicCast[B], 
                                            conv3: ToDatomicCast[C], 
                                            conv4: ToDatomicCast[D], 
                                            conv5: ToDatomicCast[E], 
                                            conv6: ToDatomicCast[F], 
                                            conv7: ToDatomicCast[G], 
                                            conv8: ToDatomicCast[H]) = new ToDatomic[ju.List[AnyRef], (A,B,C,D,E,F,G,H)] {

    override def to(c: (A,B,C,D,E,F,G,H)) = {

      datomic.Util.list(conv1.to(c._1), 
                        conv2.to(c._2), 
                        conv3.to(c._3), 
                        conv4.to(c._4), 
                        conv5.to(c._5), 
                        conv6.to(c._6), 
                        conv7.to(c._7),
                        conv8.to(c._8)).asInstanceOf[ju.List[AnyRef]]
    }
  }


  implicit def DColl2SetWrites[C, A](implicit ev: C <:< Iterable[A], conv: ToDatomicCast[A]) = new ToDatomic[ju.List[AnyRef], C] {
    override def to(c: C) = {
      val builder = Seq.newBuilder[AnyRef]
      for (e <- c) builder += conv.to(e)
      datomic.Util.list(builder.result: _*).asInstanceOf[ju.List[AnyRef]]
    }
  }

  implicit val dbConv = ToDatomic[datomic.Database, Database](_.underlying)
  implicit val datomConv = ToDatomic[datomic.Datom, Datom](_.underlying)
  implicit val rulesConv = ToDatomic[clojure.lang.IPersistentCollection, QueryRules](_.edn)
  implicit val logConv = ToDatomic[datomic.Log, Log](_.log)

}

/**
  * ToDatomicCast fixes the return type of ToDatomic as DatomicData
  */
trait ToDatomicCastImplicits {
  implicit def DDWriter2ToDatomicCast[DD <: AnyRef, A](implicit tdat: ToDatomic[DD, A]) = 
    ToDatomicCast[A] { (a: A) => tdat.to(a): AnyRef }

  implicit def DIdCast[I <: DId] = ToDatomicCast[I] { (i: I) => i.toDatomicId }
  implicit def KeywordIdentified2DRef[I <: KeywordIdentified] = ToDatomicCast[I] { (i: I) => i.ident }
  implicit def TempIdentified2DRef   [I <: TempIdentified]    = ToDatomicCast[I] { (i: I) => i.id.toDatomicId }
  implicit def FinalIdentified2DRef  [I <: FinalIdentified]   = ToDatomicCast[I] { (i: I) => i.id }

  implicit val JavaListCast = ToDatomicCast[ju.List[AnyRef]](identity)
}
