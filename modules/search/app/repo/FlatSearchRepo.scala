package repo

import java.util.Date
import javax.inject.{Inject,Singleton}

import model.b2c.FlatSearchResult
import play.api.Configuration
import play.api.libs.ws.WSClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
  * Created by oginskis on 18/05/2017.
  */
@Singleton
class FlatSearchRepo @Inject()(ws: WSClient, configuration: Configuration) {

  def searchFlats(searchString: String): List[FlatSearchResult] = {
    val request = ws.url(configuration.get[String](FlatSearchRepo.AzureSearchBaseUrl) + searchString + "*&" +
      "&$filter=lastSeenAtEpoch ge " +
      ((new Date().getTime / 1000) - configuration.underlying.getInt(FlatSearchRepo.AzureSearchNotOlderThan)) + "&$top=10")
      .withHttpHeaders("Accept" -> "application/json",
        "api-key" -> configuration.get[String](FlatSearchRepo.AzureSearchApiKey))
      .withRequestTimeout(5.second)
    val future: Future[List[FlatSearchResult]] = request.get().map {
      response =>
        (response.json \ "value").as[List[FlatSearchResult]]
    }
    Await.result(future, 10.second)
  }
}

object FlatSearchRepo {
  val AzureSearchBaseUrl = "azureSearch.searchBaseUrl"
  val AzureSearchApiKey = "azureSearch.apiKey"
  val AzureSearchNotOlderThan = "azureSearch.notOlderThan"
}
