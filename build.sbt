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
    
    // Development settings - output to dist/dev folder
    Compile / fastOptJS / crossTarget := baseDirectory.value / "dist" / "dev",
    
    // Production settings - output to dist/prod folder  
    Compile / fullOptJS / crossTarget := baseDirectory.value / "dist" / "prod",
    
    // Task to copy static assets to dev build
    TaskKey[Unit]("copyAssetsDev") := {
      val log = streams.value.log
      val baseDir = baseDirectory.value
      val devDir = baseDir / "dist" / "dev"
      
      // Create dev directory if it doesn't exist
      IO.createDirectory(devDir)
      
      // Copy HTML and CSS to dev folder
      val srcWebDir = baseDir / "src" / "web"
      IO.copyFile(srcWebDir / "index.html", devDir / "index.html")
      IO.copyFile(srcWebDir / "style.css", devDir / "style.css")
      log.info("Copied static assets to dist/dev/")
    },
    
    // Development task that builds and copies assets
    TaskKey[Unit]("dev") := {
      val log = streams.value.log
      val baseDir = baseDirectory.value
      
      // First build JS
      val jsResult = (Compile / fastOptJS).value
      
      // Then copy static assets
      val devDir = baseDir / "dist" / "dev"
      IO.createDirectory(devDir)
      val srcWebDir = baseDir / "src" / "web"
      IO.copyFile(srcWebDir / "index.html", devDir / "index.html")
      IO.copyFile(srcWebDir / "style.css", devDir / "style.css")
      
      log.info("Development build complete! Files ready in dist/dev/")
    },
    
    // Jekyll build settings
    TaskKey[Unit]("jekyllBuild") := {
      val log = streams.value.log
      val baseDir = baseDirectory.value
      val jekyllDir = baseDir / "dist" / "jekyll"
      
      // Create jekyll directory structure
      IO.createDirectory(jekyllDir)
      val jsDir = jekyllDir / "assets" / "js"
      val cssDir = jekyllDir / "assets" / "css"
      IO.createDirectory(jsDir)
      IO.createDirectory(cssDir)
      
      // Run fullOptJS first
      (Compile / fullOptJS).value
      
      // Copy optimized JS from dist/prod
      val jsFile = baseDir / "dist" / "prod" / "akka-fsm-visualizer-opt.js"
      if (jsFile.exists()) {
        IO.copyFile(jsFile, jsDir / "akka-fsm-visualizer.js")
        log.info("Copied optimized JS to jekyll/assets/js/")
      }
      
      // Copy CSS from src/web
      val cssFile = baseDir / "src" / "web" / "style.css"
      if (cssFile.exists()) {
        IO.copyFile(cssFile, cssDir / "akka-fsm-visualizer.css")
        log.info("Copied CSS to jekyll/assets/css/")
      }
      
      // Copy Jekyll template
      val templateFile = baseDir / "src" / "web" / "jekyll.html"
      if (templateFile.exists()) {
        IO.copyFile(templateFile, jekyllDir / "akka-fsm-visualizer.html")
        log.info("Copied Jekyll template to jekyll/akka-fsm-visualizer.html")
      }
      
      log.info(s"Jekyll build complete! Files ready in: $jekyllDir")
      log.info("You can now copy the dist/jekyll/ folder contents to your Jekyll blog")
    },
  )