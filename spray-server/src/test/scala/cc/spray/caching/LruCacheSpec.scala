package cc.spray.caching

import org.specs.Specification
import akka.util.duration._
import akka.actor.Actor

class LruCacheSpec extends Specification {
  noDetailedDiffs()

  "An LruCache" should {
    "be initially empty" in {
      LruCache().store.toString mustEqual "Map()"
    }
    "store uncached values" in {
      val cache = LruCache[String]()
      cache(1)("A").get mustEqual "A"
      cache.store.toString mustEqual "Map(1 -> A)"
    }
    "return stored values upon cache hit on existing values" in {
      val cache = LruCache[String]()
      cache(1)("A").get mustEqual "A"
      cache(1)("").get mustEqual "A"
      cache.store.toString mustEqual "Map(1 -> A)"
    }
    "return Futures on uncached values during evaluation and replace these with the value afterwards" in {
      val cache = LruCache[String]()
      val future1 = cache(1) { completableFuture =>
        Actor.spawn {
          Thread.sleep(10)
          completableFuture.completeWithResult("A")
        }
      }
      val future2 = cache(1)("")
      cache.store.toString mustEqual "Map(1 -> pending)"
      future1.get mustEqual "A"
      future2.get mustEqual "A"
      cache.store.toString mustEqual "Map(1 -> A)"
    }
    "properly limit capacity" in {
      val cache = LruCache[String](maxEntries = 3)
      cache(1)("A").get mustEqual "A"
      cache(2)("B").get mustEqual "B"
      cache(3)("C").get mustEqual "C"
      cache.store.toString mustEqual "Map(1 -> A, 2 -> B, 3 -> C)"
      cache(4)("D")
      cache.store.toString mustEqual "Map(2 -> B, 3 -> C, 4 -> D)"
    }
    "honor the dropFraction param during resizing" in {
      val cache = LruCache[String](maxEntries = 3, dropFraction = 0.4)
      cache(1)("A").get mustEqual "A"
      cache(2)("B").get mustEqual "B"
      cache(3)("C").get mustEqual "C"
      cache.store.toString mustEqual "Map(1 -> A, 2 -> B, 3 -> C)"
      cache(4)("D")
      cache.store.toString mustEqual "Map(3 -> C, 4 -> D)"
    }
    "expire old entries" in {
      val cache = LruCache[String](ttl = 20.millis)
      cache(1)("A").get mustEqual "A"
      cache(2)("B").get mustEqual "B"
      Thread.sleep(10)
      cache(3)("C").get mustEqual "C"
      cache.store.toString mustEqual "Map(1 -> A, 2 -> B, 3 -> C)"
      Thread.sleep(10)
      cache.get(2) must beNone // triggers clean up, also of earlier entries
      cache.store.toString mustEqual "Map(3 -> C)"
    }
    "refresh an entries expiration time on cache hit" in {
      val cache = LruCache[String]()
      cache(1)("A").get mustEqual "A"
      cache(2)("B").get mustEqual "B"
      cache(3)("C").get mustEqual "C"
      cache(1)("").get mustEqual "A" // refresh
      cache.store.toString mustEqual "Map(2 -> B, 3 -> C, 1 -> A)"
    }
  }
}
