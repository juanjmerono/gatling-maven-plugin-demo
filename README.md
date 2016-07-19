Sakai Gatling Stress Test
=========================

This is a Simple Stress Test for Sakai 11+ instances.

To test it out, simply execute the following command:

    $mvn gatling:execute

Each time you run this you'll get a result in target/sakaisimulation-xxxxxxx folder.

Stress Test Use Case
====================

This test is really simple, I hope we can add more complex cases soon.

Use two different type of users "random" and "exahustive".

The random users follow these steps:
	- Go to the portal
	- Login
	- Go to one random site
	- Got to one random tool
	- Logout 

The exahustive users follow these steps:
	- Go to the portal
	- Login
	- Go to each site
	- Got to each tool on each site
	- Logout 

Both types of users are running at the same time, so during the test you've got the same number of users of both types.

Setting Up Your Test
====================

There are several things you can tune in this test:

- Target URL: You can change the URL for the test by typing

	$mvn gatling:execute -Dtesturl=https://my-sakai-instance
	
- Concurrent Users and RampUp time: You can change the number of concurrent users and the time to rampup them by typing

	$mvn gatling:execute -DtargetUsers=<Users> -DrampUpTime=<Seconds>
	
- Loops: You are able to repeat the test in different ways
	
	$mvn gatling:execute -DuserLoop=U -DsiteLoop=S -DtoolLoop=T

	* userLoop: The test case is repeated N times (all the steps)
	* siteLoop: For random users pick S random sites
	* toolLoop: For random users pick T tools on each random site
	
Exploring the results
=====================

Gatling return an HTML report that gives you lot of information about the stress test.

Please go to http://gatling.io/ to know more about it.
