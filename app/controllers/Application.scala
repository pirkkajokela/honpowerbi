package controllers

import play.api._
import play.api.mvc._
import scala.concurrent.Future
import play.api.libs.json.JsValue
import com.google.inject.Inject
import play.api.libs.ws.ning.WSClientProvider
import play.api.libs.ws.WSClient
import play.api.libs.ws.WSAuthScheme
import com.typesafe.config.ConfigException.Missing
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.json.JsArray
import play.api.libs.json.JsObject
import model._
import play.api.libs.json.JsString

class Application @Inject() (
    wsClient: WSClient,
    configuration: Configuration
) extends Controller {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }
  
  def load(survey: String) = Action.async {
    val jsonF = loadHappyOrNotData(s"/surveys/$survey/results")
    jsonF.map { json => 
      (Ok(json))
    }
  }

  def foldersRoot() = Action.async {
    val jsonF = loadHappyOrNotData(s"/folders")
    jsonF.map { json => 
      (Ok(json))
    }
  }

  def folders(parent: String) = Action.async {
    val jsonF = loadHappyOrNotData(s"/folders/$parent")
    jsonF.map { json => 
      (Ok(json))
    }
  }

  def foldersWithSurveys(parent: String) = Action.async {
    val jsonF = loadHappyOrNotData(s"/folders/$parent?surveys=true")
    jsonF.map { json => 
      (Ok(json))
    }
  }

  def folderExport(parent: String) = Action.async {
    val jsonF = loadHappyOrNotData(s"/folders/$parent?surveys=true")
    
    val resultsF : Future[Seq[(Location, Survey, Response)]] = jsonF.flatMap { json =>
      val surveys = (json \ "folders").as[JsArray].value.flatMap{ js => 
        val jsonSurvey = js.as[JsObject]
        val location = Location(jsonSurvey)
        location.surveys.map(s=>(location, s))
      }
      
      val resultFutures: Seq[Future[JsValue]] = 
        surveys.map{case (l,s)=> loadHappyOrNotData(
            s"/surveys/${s.key}/results?date_start=2015-01-01T00:00:00.000Z&date_end=2015-12-31T23:59:59.000Z&truncate=hour")}
      val resultsFuture: Future[Seq[JsValue]] = Future.sequence(resultFutures)
      
      resultsFuture.map { seq => 
        val responses = seq.map { res =>
          res.as[JsArray].value.map(js=>Response(js.as[JsObject]))
        }
        surveys.zip(responses).flatMap { case ((l, s), responses) =>
          responses.map(r=>(l,s,r))
        }
      }
    }
    
    val formattedF = resultsF.map { resultsList => 
      val complete: String = resultsList.map { case (location, survey, response) => 
        // Print out all columns for now:
        val cols = Col.listOfCols.map { col => col.resolve(location, survey, response) }
        cols.mkString(""""""", """","""", """"""")
      }.mkString("\n")
      
      Col.renderHeader + "\n" + complete
    }
    
    formattedF.map { formatted => 
      (Ok(formatted))
    }
  }

  private def loadHappyOrNotData(queryPath: String): Future[JsValue] = {
    val username = configuration.getString("happy-or-not.api.username").getOrElse(throw new Missing("happy-or-not.api.username"))
    val password = configuration.getString("happy-or-not.api.password").getOrElse(throw new Missing("happy-or-not.api.password"))
    
    val result = wsClient.url(s"https://api.happy-or-not.com/v1/$queryPath")
      .withAuth(username, password, WSAuthScheme.BASIC)
      .get()
    
    result.onFailure{
      case e: Exception => Logger.error(e.getMessage, e)
        throw e
    }
    result.map{ x =>
      try {
        x.json
      } catch {
        case e: Exception => Logger.error(e.getMessage, e)
        Logger.error("Failed to parse body: " + x.body)
        throw e
      }
    }
  }
}
