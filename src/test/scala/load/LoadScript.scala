package load

import io.gatling.core.scenario.Simulation
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import load.Conf._

import scala.concurrent.duration._

class LoadScript extends Simulation {

  val httpProtocol = http
    .baseUrl(baseUrl)
    .inferHtmlResources()
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("ru,en-US;q=0.7,en;q=0.3")
    .upgradeInsecureRequestsHeader("1")
    .userAgentHeader("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.102 Safari/537.36 Edge/18.18362")
    .check(status is 200)


  /**
    * Сценарии нагрузки
    */
  val searchScn = scenario("Search").exec(Search.search)
  val browseScn = scenario("Browse").exec(Browse.browse)
  val editScn = scenario("Edit").exec(Edit.edit)
  val deleteScn = scenario("Delete").exec(Delete.delete)
  val loadScn = scenario("LoadScenario").exec(Home.home, Edit.edit, Delete.delete)

  //val test = scenario("TestScenario").exec(Search.search)

  //setUp(test.inject(atOnceUsers(1))).protocols(httpProtocol)

  /**
    * Профиль нагрузки поиска максимума
    * Этот профиль с постепенным пошаговым увеличением нагрузки позволяет точнее определить, при какой нагрузке
    * достигаем максимальной производительности и пронаблюдать потребление ресурсов на каждом шаге.
    * По результатам теста определяем не только максимальную нагрузку, но и нормальную производительность ПО / АС
    * Указываем шаг добавления интенсивности, например, 10 пользователями -- incrementUsersPerSec(10)
    * Указываем кол-во шагов -- .times(5)
    * Указываем длительность теста на каждом уровне нагрузки -- .eachLevelLasting(30 minutes)
    * Указываем время, за которое произойдет увеличение нагрузки -- .separatedByRampsLasting(15 seconds)
    * Указываем начальное кол-во пользователей -- .startingFrom(10)
    */
  setUp(
    loadScn.inject(
      incrementUsersPerSec(10)
        .times(5)
        .eachLevelLasting(30 minutes)
        .separatedByRampsLasting(15 seconds)
        .startingFrom(10)
    ).protocols(httpProtocol)
  )

  /**
    * Профиль тестирования стабильности или надежности
    * За конкретную интенсивность берем ожидаемый уровень нагрузки (100 tps) и подвергаем на длительный тест (12 часов)
    */
  setUp(
    loadScn.inject(
      constantUsersPerSec(100) during (12 hours)
    ).protocols(httpProtocol)
  )
}

object Home {

  val home = exec(
    http("Home")
    .get("/")
    .check(status is 200)
  )
}

object Search {

  val feeder = csv("search.csv").circular

  val search = exec(
    http("Home")
    .get("/"))
    .feed(feeder)
    .exec(http("Search")
      .get("/computers?f=${searchCriterion}")
      .check(css("a:contains('${searchComputerName}')", "href").saveAs("computerURL")))
    .doIf(session => session("computerURL").asOption[String].isDefined) {
      exec(http("Select")
        .get("${computerURL}")
      )
    }
}

object Browse {

  val browse = repeat(5, "n") {
    exec(http("Page ${n}")
      .get("/computers?p=${n}")
      .check(status is 200)
    )
  }
}

object Edit {

  val feeder = csv("data.csv").circular

  val edit = exec(http("Form")
    .get("/computers/new"))
    .feed(feeder)
    .exec(http("Post")
      .post("/computers")
      .formParam("name", "${name}")
      .formParam("introduced", "${introduced}")
      .formParam("discontinued", "${discontinued}")
      .formParam("company", "${company}")
      .check(status is 200)
    )
}

object Delete {

  val feeder = csv("data.csv").circular

  val delete = scenario("Delete")
    // Search
    .exec(http("Home")
      .get("/"))
    .feed(feeder)
    .exec(http("Search")
      .get("/computers?f=${name}")
      .check(css("a:contains('${name}')", "href").saveAs("computerURL")))
    // Delete
    .doIf(session => session("computerURL").asOption[String].isDefined) {
      exec(http("Delete")
        .post("${computerURL}/delete")
        .check(status is 200)
      )
    }
}


