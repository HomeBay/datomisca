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

import DatomicMapping._
import Queries._

import scala.language.reflectiveCalls

import org.specs2.mutable._

import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.implicitConversions

class TupleMappingSpec extends Specification {
  sequential

  val uri = "datomic:mem://TupleMappingSpec"

  
  "Datomic" should {

    "Handle a heterogenous tuple of size 2" in {

      case class Point2D(name: String, point: (Long, Long))

      object Point2D {

        object Schema {

          val namespace = Namespace("point2d")

          val name  = Attribute(namespace / "name", SchemaType.string, Cardinality.one)
          val point = Attribute(namespace / "point", SchemaType.tuple, Cardinality.one).withTupleTypes(SchemaType.long, SchemaType.long)

          val all = List(name, point)
        }

        implicit val reader = (
          Schema.name.read[String] and 
          Schema.point.read[(Long, Long)]
        )(Point2D.apply _)

      }

      val tempId = DId(Partition.USER)
      val insert: AddEntity = (
        SchemaEntity.newBuilder
          += (Point2D.Schema.name  -> "test1")
          += (Point2D.Schema.point -> (1, 2))
        ) withId tempId

      Datomic.createDatabase(uri)
      implicit val conn = Datomic.connect(uri)

      Await.result(for{
        _        <- Datomic.transact(Point2D.Schema.all)
        txReport <- Datomic.transact(insert) 
      } yield {

        val db = txReport.dbAfter
        val entity = db.entity(txReport.resolve(tempId))
        val point2d = DatomicMapping.fromEntity[Point2D](entity)

        point2d.point === (1,2)
      }, Duration("2 seconds"))
    }


    "Handle a heterogenous tuple of size 5" in {

      case class Place(name: String, address: (String, String, Place.State, Long))

      object Place {


        sealed abstract class State extends DatomicEnum(Schema.ns.place.state) // 1, 7 
        object State extends DatomicEnumSet[State] {  // 4 10

          case object CA extends State  // 6
          case object NY extends State
          case object TX extends State

          override val all: List[State] = List(CA, NY, TX) // 5
        }

        object Schema {  // 2 8 

          object ns {
            val place = new Namespace("place"){
              val state = Namespace("place.state")
            }
          }  

          val name    = Attribute(ns.place / "name", SchemaType.string, Cardinality.one)
          val address = Attribute(ns.place / "adddress", SchemaType.tuple, Cardinality.one).withTupleTypes(SchemaType.string, 
                                                                                                           SchemaType.string, 
                                                                                                           SchemaType.keyword,
                                                                                                           SchemaType.long)

          def all = State.idents ++ List(name, address) // 3 9 
        }

        implicit val reader = (
          Schema.name.read[String] and 
          Schema.address.read[(String, String, Keyword, Long)].map(a =>  a.copy(_3 = State.kwToEnum(a._3)))
        )(Place.apply _)

      }

      val tempId = DId(Partition.USER)
      val insert: AddEntity = (
        SchemaEntity.newBuilder
          += (Place.Schema.name  -> "test1")
          += (Place.Schema.address -> ("123 Main St", "Santa Barbara",  Place.State.CA.keyword, 93101) )
        ) withId tempId

      Datomic.createDatabase(uri)
      implicit val conn = Datomic.connect(uri)

      Await.result(for{
        _        <- Datomic.transact(Place.Schema.all)
        txReport <- Datomic.transact(insert) 
      } yield {

        val db = txReport.dbAfter
        val entity = db.entity(txReport.resolve(tempId))
        val place = DatomicMapping.fromEntity[Place](entity)

        place.address === ("123 Main St", "Santa Barbara", Place.State.CA, 93101)
      }, Duration("2 seconds"))
    }


    "Handle a composite tuple of size 2" in {

      case class Point(x: Long, y: Long, point: (Long, Long))

      object Point {

        object Schema {

          val namespace = Namespace("point")

          val x  = Attribute(namespace / "x", SchemaType.long, Cardinality.one)
          val y  = Attribute(namespace / "y", SchemaType.long, Cardinality.one)
          // create a unique composite of x and y such that new two identical points can exist at the same time in the DB
          val point = Attribute(namespace / "point", SchemaType.tuple, Cardinality.one).withTupleAttrs(x, y).withUnique(Unique.identity)

          val all = List(x, y, point)
        }

        implicit val reader = (
          Schema.x.read[Long] and 
          Schema.y.read[Long] and 
          Schema.point.read[(Long, Long)]
        )(Point.apply _)

      }

      val tempId = DId(Partition.USER)
      def insert(id: DId): AddEntity = (
        SchemaEntity.newBuilder
          += (Point.Schema.x  -> 1)
          += (Point.Schema.y  -> 2)
        ) withId id

      Datomic.createDatabase(uri)
      implicit val conn = Datomic.connect(uri)

      Await.result(for{
        _        <- Datomic.transact(Point.Schema.all)
        txReport <- Datomic.transact(insert(tempId)) 
      } yield {

        val db = txReport.dbAfter
        val entity = db.entity(txReport.resolve(tempId))
        val point = DatomicMapping.fromEntity[Point](entity)

        point.point === (1,2)
      }, Duration("2 seconds"))

      // insert another one but this time make sure it throws an exception due to the uniqque constraint
      Await.result(for{_ <- Datomic.transact(insert(DId(Partition.USER))) } yield (), Duration("2 seconds")) must throwA[Exception]
    }
  } // end should
}


abstract class DatomicEnum(namespace: Namespace) {

  val name = this.toString

  def keyword: Keyword = namespace / this.toString
}


trait DatomicEnumSet[E <: DatomicEnum]  {

  val all: List[E]

  def apply(str: String): E = fromString(str).get

  def fromString(str: String): Option[E] = all.find(_.toString.equalsIgnoreCase(str))

  def idents: List[AddIdent] = all.map(e => AddIdent(e.keyword)).toList
  
  implicit def kwToEnum(kw: Keyword): E = {

    fromString(kw.getName) match { 
      case Some(e) => e
      case None => throw new Exception(s"Failed to convert enum keyword ${kw} to enum value, and no default was provided")
    }
  }

  implicit def enumToKw(enum: E): Keyword = enum.keyword
}