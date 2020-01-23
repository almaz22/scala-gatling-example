name := "scala-gatling-example"

version := "0.1"

scalaVersion := "2.12.10"

enablePlugins(GatlingPlugin)
scalacOptions := Seq(
  "-encoding", "UTF-8", "-target:jvm-1.8", "-deprecation",
  "-feature", "-unchecked", "-language:implicitConversions", "-language:postfixOps")
libraryDependencies += "io.gatling.highcharts" % "gatling-charts-highcharts" % "3.3.1" % "test,it"
libraryDependencies += "io.gatling"            % "gatling-test-framework"    % "3.3.1" % "test,it"
javaOptions in Gatling := overrideDefaultJavaOptions("-Xss10m", "-Xms2G", "-Xmx8G")