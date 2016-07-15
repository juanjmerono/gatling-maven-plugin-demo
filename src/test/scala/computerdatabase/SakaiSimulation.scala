
import scala.concurrent.duration._

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._
import java.net._

class SakaiSimulation extends Simulation {

	val successStatus: Int = 200
	val pauseMin: Int = Integer.getInteger("min-pause",1)
	val pauseMax: Int = Integer.getInteger("max-pause",1)
	val httpProtocol = http
		.baseURL(System.getProperty("test-url"))
		.inferHtmlResources(BlackList(""".*(\.css|\.js|\.png|\.jpg|\.gif|thumb).*"""), WhiteList())
		.userAgentHeader("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36")

	val headers = Map(
		"Accept" -> "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
		"Accept-Encoding" -> "gzip, deflate, sdch, br",
		"Accept-Language" -> "es-ES,es;q=0.8,en;q=0.6",
		"Cache-Control" -> "max-age=0",
		"Connection" -> "keep-alive",
		"Upgrade-Insecure-Requests" -> "1")

	val users = csv("user_credentials.csv").random

	object Login {
		val login = group("Login") {
			exec(http("Portal")
				.get("/portal")
				.headers(headers)
				.check(status.is(successStatus)))
			.pause(pauseMin,pauseMax)
			.feed(users)
			.exec(http("XLogin [${username}]")
				.post("/portal/xlogin")
				.headers(headers)
				.formParam("eid", "${username}")
				.formParam("pw", "${password}")
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
						val myTools: Vector[(String,String)] = (session("toolNames").as[Vector[String]] zip session("toolUrls").as[Vector[String]].map(s => URLDecoder.decode(s,"UTF-8")))
						session.set("tools", myTools)
					})
					.foreach("${tools}","tool") {
						/** Do not process help tool */
						doIf(session => !session("tool").as[(String,String)]._2.contains("/portal/help/main")) {
							exec(http("/tool/${tool._1}")
								.get("${tool._2}")
								.headers(headers)
								.check(status.is(successStatus))
								.check(css("title").is("Sakai : ${site._1} : ${tool._1}"))
								.check(css("iframe","src").findAll.optional.saveAs("frameUrls"))
								.check(css("iframe","title").findAll.optional.saveAs("frameNames")))
							.pause(pauseMin,pauseMax)
							/** Take care of iframed tools */
							.doIf("${frameUrls.exists()}") {
								exec(session => { 
									val myFrames: Vector[(String,String)] = (session("frameNames").as[Vector[String]] zip session("frameUrls").as[Vector[String]].map(s => URLDecoder.decode(s,"UTF-8")))
									session.set("frames", myFrames)
								})
								.foreach("${frames}","frame") {
									exec(http("/frame/${frame._1}")
										.get("${frame._2}")
										.headers(headers)
										.check(status.is(successStatus)))
									.pause(pauseMin,pauseMax)
								}
							}
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

	setUp(scn.inject(atOnceUsers(25))).protocols(httpProtocol)
}