package zio.cache

import java.util.concurrent.TimeUnit

import scala.collection.immutable.Range

import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole

import zio.{ BootstrapRuntime, ZIO }
import zio.internal.Platform
import scalacache.{ Cache => SCache, _ }
import scalacache.caffeine._

/**
  * (Baseline)            ChurnBenchmark.zioCacheChurn   10000  thrpt    3  113.703 ∩┐╜ 2.392  ops/s
  * (Pending | Complete): ChurnBenchmark.zioCacheChurn   10000  thrpt    3  105.944 ∩┐╜ 5.873  ops/s
  * 
  *  0.020 ops/s
  */
@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class ChurnBenchmark extends BootstrapRuntime {
 override val platform: Platform = Platform.benchmark

  @Param(Array("10000"))
  var size: Int = _

  var newEntries: Array[String] = _
  var cache: Cache[String, Nothing, String] = _

  val identityLookup = Lookup[String, Any, Nothing, String](ZIO.succeed(_))
 
  @Setup(Level.Trial)
  def initialize() = {
    newEntries = (size until (2 * size)).map(_.toString).toArray
    val strings = (0 until size).map(_.toString).toArray

    cache = unsafeRun(
      for {
        cache <- Cache.make(size, CachingPolicy.byLastAccess, identityLookup)
        _     <- ZIO.foreach_(strings)(cache.get(_))
      } yield cache 
    )
  }

  @Benchmark
  def zioCacheChurn() = {
    unsafeRun(
      for {
        _       <- ZIO.foreach_(newEntries)(cache.get(_))
        // evicted <- cache.contains("0")
        // _       <- ZIO.effect(Predef.assert(!evicted))
      } yield ()
    )
  }
}