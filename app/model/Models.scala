package model

import play.api.libs.json._
import org.joda.time.DateTime
import play.api.Logger

trait JsonFields {
  val json: JsObject
  
  private lazy val jsonFields = json.fieldSet.toMap
  
  def fields(fieldName: String): JsValue = {
    try {
      jsonFields(fieldName)
    } catch {
      case e: Exception => Logger.error("Could not parse " + fieldName + " from " + json.toString)
        throw e;
    }
  }
}

case class Question(val json: JsObject) extends JsonFields {
  val key = fields("key").as[JsNumber].value
  val name = fields("name").as[JsString].value
  val locale = fields("locale").as[JsString].value
}

case class Survey(val json: JsObject) extends JsonFields {
  val key = fields("key").as[JsNumber].value
  val question = Question(fields("question").as[JsObject])
  val locale = fields("timeZone").as[JsString].value
  val activePeriods = fields("activePeriods").as[JsArray].value
  val surveyType = fields("surveyType").as[JsString].value
}

case class Location(val json: JsObject) extends JsonFields {
  val key = fields("key").as[JsNumber].value
  val name = fields("name").as[JsString].value
  val surveys = fields("surveys").as[JsArray].value.map(js=>Survey(js.as[JsObject]))
}

case class Response(val json: JsObject) extends JsonFields {
  lazy val ts = new DateTime(fields("ts").as[JsString].value)
  lazy val results: Array[Int] = fields("data").as[JsArray].value.map(n=> n.as[JsNumber].value.toInt).toArray
  
  def veryHappy = results(3)
  def happy = results(2)
  def sad = results(1)
  def verySad = results(0)
  
  def weekday = ts.getDayOfWeek
  def hourOfDay = ts.getHourOfDay
  def month = ts.getMonthOfYear
  def year = ts.getYear
}

trait Col {
  def name: String
  def resolve(location: Location, survey: Survey, response: Response): String
}

object Col {
  val listOfCols: List[Col] = List(
        loc("locationId", (l)=>l.key.toString),
        loc("locationName", (l)=>l.name),
        survey("surveyId", (s)=>s.key.toString),
        survey("surveyLocale", (s)=>s.locale),
        survey("surveyType", (s)=>s.surveyType),
        q("questionId", (q)=>q.key.toString),
        q("questionName", (q)=>q.name),
        q("questionLocale", (q)=>q.locale),
        res("timeStamp", (r)=>r.ts.toString),
        res("weekday", (r)=>r.weekday.toString),
        res("hourOfDay", (r)=>r.hourOfDay.toString),
        res("month", (r)=>r.month.toString),
        res("year", (r)=>r.year.toString),
        res("veryHappy", (r)=>r.veryHappy.toString),
        res("happy", (r)=>r.happy.toString),
        res("sad", (r)=>r.sad.toString),
        res("verySad", (r)=>r.verySad.toString)
      )
      
      
  def res(colName: String, fn: (Response) => String) = 
    toCol(colName, (l, s, r) => fn(r))
      
  def q(colName: String, fn: (Question) => String) = 
    toCol(colName, (l, s, r) => fn(s.question))
      
  def survey(colName: String, fn: (Survey) => String) = 
    toCol(colName, (l, s, r) => fn(s))
      
  def loc(colName: String, fn: (Location) => String) = 
    toCol(colName, (l, s, r) => fn(l))
      
  def toCol(colName: String, fn: (Location, Survey, Response) => String) = 
    new Col {
      val name = colName
      def resolve(l: Location, s: Survey, r: Response): String = fn(l, s, r)
    }
  
  def renderHeader : String = listOfCols.map(_.name).mkString(""""""", """","""", """"""")
}
