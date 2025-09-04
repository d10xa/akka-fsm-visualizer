ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.13.16"

lazy val root = (project in file("."))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "akka-fsm-visualizer",
    
    libraryDependencies ++= Seq(
      "org.scalameta" %%% "scalameta" % "4.13.9",
      "org.scala-js" %%% "scalajs-dom" % "2.8.1"
    ),
    // Scala.js configuration
    scalaJSUseMainModuleInitializer := true,
    
    // Enable source maps for debugging
    Compile / fastOptJS / scalaJSLinkerConfig ~= {
      _.withSourceMap(true)
    },
    
    // Development settings - output to docs folder for easy serving
    Compile / fastOptJS / crossTarget := baseDirectory.value / "docs",
    Compile / fullOptJS / crossTarget := baseDirectory.value / "docs"
  )