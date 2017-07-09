package io.jp

import scala.concurrent.duration._

import io.gatling.app.Gatling
import io.gatling.core.Predef._
import io.gatling.core.config.GatlingPropertiesBuilder
// 2
import io.gatling.http.Predef._

class BasicSimulation extends Simulation {
  // 3

  val httpConf = http // 4
    .baseURLs("http://localhost:8080", "http://localhost:8081")


  val scn = scenario("BasicSimulation") // 7
    .exec(http("request_1") // 8
    .get("/?name=Peter")) // 9

  setUp(// 11
    scn.inject(rampUsersPerSec(1000) to (10000) during (1 minutes)) // 12
    ).protocols(httpConf) // 13


}