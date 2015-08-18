/*
 * =========================================================================================
 * Copyright © 2015 the khronus project <https://github.com/hotels-tech/khronus>
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

package com.searchlight.khronus.model

import com.searchlight.khronus.store.MetaSupport
import com.searchlight.khronus.util.log.Logging
import com.searchlight.khronus.util.{ FutureSupport, SameThreadExecutionContext }

import scala.concurrent.Future
import scala.util.Failure

class TimeWindowChain extends TimeWindowsSupport with Logging with MetaSupport with FutureSupport {

  implicit val executionContext = SameThreadExecutionContext

  def process(metrics: Seq[Metric]): Future[Unit] = {
    val tick = currentTick()
    serializeFutures(metrics) { metric ⇒
      process(metric, tick)
    } flatMap (metrics ⇒ metaStore.update(metrics, tick.endTimestamp))
  }

  private def process(metric: Metric, currentTick: Tick): Future[Metric] = {
    val windowsToProcessInThisTick = windows(metric.mtype).filter(mustExecuteInThisTick(_, metric, currentTick))

    serializeFutures(windowsToProcessInThisTick) { window ⇒
      window.process(metric, currentTick) andThen {
        case Failure(reason) ⇒ log.error(s"Fail to process window ${window.duration} for $metric", reason)
      }
    } flatMap (_ ⇒ Future.successful(metric))

  }

  private def mustExecuteInThisTick(timeWindow: TimeWindow[_, _], metric: Metric, tick: Tick): Boolean = {
    if ((tick.endTimestamp.ms % timeWindow.duration.toMillis) == 0) {
      true
    } else {
      log.trace(s"${p(metric, timeWindow.duration)} Excluded to run")
      false
    }
  }

  def currentTick(): Tick = Tick()

}