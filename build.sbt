ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.13.16"

lazy val root = (project in file("."))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "akka-fsm-visualizer",
    
    libraryDependencies ++= Seq(
      "org.scalameta" %%% "scalameta" % "4.13.9",
      "org.scala-js" %%% "scalajs-dom" % "2.8.1",
      "org.scalameta" %%% "munit" % "1.0.3" % Test
    ),
    // Scala.js configuration
    scalaJSUseMainModuleInitializer := true,
    
    // Enable source maps for debugging
    Compile / fastOptJS / scalaJSLinkerConfig ~= {
      _.withSourceMap(true)
    },
    
    // Development settings - output to docs folder for easy serving
    Compile / fastOptJS / crossTarget := baseDirectory.value / "docs",
    Compile / fullOptJS / crossTarget := baseDirectory.value / "docs",
    
    // Jekyll build settings - output to jekyll folder
    TaskKey[Unit]("jekyllBuild") := {
      val log = streams.value.log
      val baseDir = baseDirectory.value
      val jekyllDir = baseDir / "jekyll"
      
      // Create jekyll directory
      IO.createDirectory(jekyllDir)
      
      // Run fullOptJS first
      (Compile / fullOptJS).value
      
      // Copy optimized JS to jekyll/assets/js/
      val jsDir = jekyllDir / "assets" / "js"
      IO.createDirectory(jsDir)
      
      val jsFile = baseDir / "docs" / "akka-fsm-visualizer-opt.js"
      if (jsFile.exists()) {
        IO.copyFile(jsFile, jsDir / "akka-fsm-visualizer.js")
        log.info("Copied optimized JS to jekyll/assets/js/")
      }
      
      // Copy CSS to jekyll/assets/css/
      val cssDir = jekyllDir / "assets" / "css"
      IO.createDirectory(cssDir)
      val cssFile = baseDir / "docs" / "style.css"
      if (cssFile.exists()) {
        IO.copyFile(cssFile, cssDir / "akka-fsm-visualizer.css")
        log.info("Copied CSS to jekyll/assets/css/")
      }
      
      // Copy Jekyll template as HTML page
      val templateFile = baseDir / "jekyll-template.html"
      if (templateFile.exists()) {
        IO.copyFile(templateFile, jekyllDir / "akka-fsm-visualizer.html")
        log.info("Copied Jekyll template to jekyll/akka-fsm-visualizer.html")
      }
      
      log.info(s"Jekyll build complete! Files copied to: $jekyllDir")
      log.info("You can now copy the jekyll/ folder contents to your Jekyll blog")
    }
  )