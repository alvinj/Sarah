name := "Sarah"

version := "0.1"

scalaVersion := "2.9.1"

// including dispatch (http://dispatch.databinder.net/Project+Setup.html)
libraryDependencies ++= Seq(
  "net.databinder" %% "dispatch-http" % "0.8.7"
)

libraryDependencies ++= Seq( "net.databinder" %% "dispatch-core" % "0.8.7" )

libraryDependencies ++= Seq( "net.databinder" %% "dispatch-json" % "0.8.7" )

libraryDependencies ++= Seq( "net.databinder" %% "dispatch-http-json" % "0.8.7" )

libraryDependencies ++= Seq( "net.databinder" %% "dispatch-oauth" % "0.8.7" )




