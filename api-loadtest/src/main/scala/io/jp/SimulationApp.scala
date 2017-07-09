package io.jp

import io.gatling.app.Gatling
import io.gatling.core.config.GatlingPropertiesBuilder

/**
  * Created by johnnypark on 27.06.17.
  */
object SimulationApp extends App {

  val simClass = classOf[BasicSimulation].getName
  val props = new GatlingPropertiesBuilder
  props.simulationClass(simClass)
  Gatling.fromMap(props.build)

}
