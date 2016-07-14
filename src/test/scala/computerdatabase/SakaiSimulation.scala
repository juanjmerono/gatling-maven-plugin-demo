
import scala.concurrent.duration._

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._

class SakaiSimulation extends Simulation {

	val successStatus: Int = 200
	val httpProtocol = http
		.baseURL("https://trunk-mysql.nightly.sakaiproject.org")
		.inferHtmlResources(BlackList(""".*(\.css|\.js|\.png|\.jpg).*"""), WhiteList())
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
			.pause(2)
			.exec(http("XLogin")
				.post("/portal/xlogin")
				.headers(headers)
				.formParam("eid", "instructor")
				.formParam("pw", "sakai")
				.formParam("submit", "Log+In")
				.check(status.is(successStatus)))
			.pause(2)
		} 
	}

	object GoSite {
		val go = group("Site") {
			exec(http("GetSites")
				.get("/portal")
				.headers(headers)
				.check(status.is(successStatus))
				.check(css("div.fav-title > a","href").findAll.saveAs("siteUrls")))
			.pause(1)
			.exec(http("FavSites")
				.get("/portal/favorites/list")
				.headers(headers)
				.check(status.is(successStatus)))
			.pause(1)
			.foreach("${siteUrls}","url") {
				exec(http("GoToSite ${url}")
					.get("${url}")
					.headers(headers)
					.check(status.is(successStatus)))
				.pause(2)
			}
		} 
	}

	object GoTool {
		val go = group("Tool") {
			exec(http("GoToTool")
				.get("/portal/site/c20d9953-c71f-4989-be2a-21e67d937151/tool/d5dcaa3b-561e-4823-b732-95ac97a86c59")
				.headers(headers)
				.check(status.is(successStatus)))
			.pause(3)
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

	val scn = scenario("SakaiSimulation").exec(Login.login,GoSite.go,GoTool.go,Logout.logout)

	setUp(scn.inject(atOnceUsers(1))).protocols(httpProtocol)
}