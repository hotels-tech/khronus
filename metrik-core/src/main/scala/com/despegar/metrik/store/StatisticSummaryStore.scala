/*
 * =========================================================================================
 * Copyright © 2014 the metrik project <https://github.com/hotels-tech/metrik>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 * =========================================================================================
 */

package com.despegar.metrik.store

import com.despegar.metrik.model.StatisticSummary
import com.despegar.metrik.util.{ KryoSerializer, Logging, Settings }

import scala.concurrent.duration._

case class ColumnRange(from: Long, to: Long, reversed: Boolean, count: Int)

trait StatisticSummarySupport extends SummaryStoreSupport[StatisticSummary] {
  override def summaryStore: SummaryStore[StatisticSummary] = CassandraStatisticSummaryStore
}

object CassandraStatisticSummaryStore extends SummaryStore[StatisticSummary] with Logging {
  //create column family definition for every bucket duration
  val windowDurations: Seq[Duration] = Settings().Histogram.WindowDurations

  val serializer: KryoSerializer[StatisticSummary] = new KryoSerializer("statisticSummary", List(StatisticSummary.getClass))

  override def getColumnFamilyName(duration: Duration) = s"statisticSummary${duration.length}${duration.unit}"

  override def serializeSummary(summary: StatisticSummary): Array[Byte] = {
    serializer.serialize(summary)
  }

  override def deserialize(bytes: Array[Byte]): StatisticSummary = {
    serializer.deserialize(bytes)
  }

  override def ttl(windowDuration: Duration): Int = Settings().Histogram.SummaryRetentionPolicy

}