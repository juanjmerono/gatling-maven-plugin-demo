
import scala.concurrent.duration._

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._

class SakaiSimulation extends Simulation {

	val successStatus: Int = 200
	val pauseMin: Int = 1
	val pauseMax: Int = 1
	val httpProtocol = http
		.baseURL("https://trunk-mysql.nightly.sakaiproject.org")
		.inferHtmlResources(BlackList(""".*(\.css|\.js|\.png|\.jpg|\.gif|thumb).*"""), WhiteList())
		.userAgentHeader("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36")

	val headers = Map(
		"Accept" -> "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
		"Accept-Encoding" -> "gzip, deflate, sdch, br",
		"Accept-Language" -> "es-ES,es;q=0.8,en;q=0.6",
		"Cache-Control" -> "max-age=0",
		"Connection" -> "keep-alive",
		"Upgrade-Insecure-Requests" -> "1")

	object Login {
		val login = group("Login") {
			exec(http("Portal")
				.get("/portal")
				.headers(headers)
				.check(status.is(successStatus)))
			.pause(pauseMin,pauseMax)
			.exec(http("XLogin")
				.post("/portal/xlogin")
				.headers(headers)
				.formParam("eid", "instructor")
				.formParam("pw", "sakai")
				.formParam("submit", "Log+In")
				.check(status.is(successStatus))
				.check(css("div.fav-title > a","href").findAll.saveAs("siteUrls"))
				.check(css("div.fav-title > a","title").findAll.saveAs("siteTitles")))
			.pause(pauseMin,pauseMax)
			.exec(session => { 
				val mySites: Vector[(String,String)] = (session("siteTitles").as[Vector[String]] zip session("siteUrls").as[Vector[String]])
				session.set("sites", mySites)
			})
		} 
	}

	object GoSite {
		val go = group("Sites") {
			/**.exec(http("FavSites")
				.get("/portal/favorites/list")
				.headers(headers)
				.check(status.is(successStatus)))
			.pause(pauseMin,pauseMax)
			.exec(session => {
				print(session)
				session 
			})*/
			foreach("${sites}","site") {
				group("${site._1}") {
					exec(http("/site/${site._1}")
						.get("${site._2}")
						.headers(headers)
						.check(status.is(successStatus))
						.check(css("title:contains('Sakai : ${site._1} :')").exists)
						.check(css("a.Mrphs-toolsNav__menuitem--link","href").findAll.saveAs("toolUrls"))
						.check(css("span.Mrphs-toolsNav__menuitem--title").findAll.saveAs("toolNames")))
					.pause(pauseMin,pauseMax)
					.exec(session => { 
						val myTools: Vector[(String,String)] = (session("toolNames").as[Vector[String]] zip session("toolUrls").as[Vector[String]])
						session.set("tools", myTools)
					})
					.foreach("${tools}","tool") {
						doIf(session => !session("tool").as[(String,String)]._2.contains("/portal/help/main")) {
							exec(http("/tool/${tool._1}")
								.get("${tool._2}")
								.headers(headers)
								.check(status.is(successStatus))
								.check(css("title").is("Sakai : ${site._1} : ${tool._1}")))
							.pause(pauseMin,pauseMax)
						}
					}
				}
			}
		} 
	}

	object Logout {
		val logout = group("Logout") {
			exec(http("Logout")
				.get("/portal/logout")
				.headers(headers)
				.check(status.is(successStatus)))
		}
	}

	val scn = scenario("SakaiSimulation").exec(Login.login,GoSite.go,Logout.logout)

	setUp(scn.inject(atOnceUsers(1))).protocols(httpProtocol)
}