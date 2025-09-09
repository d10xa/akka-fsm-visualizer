ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.13.16"

// Template processing helper function
def processTemplate(templateFile: File, cssLink: String, jsScript: String): String = {
  val template = IO.read(templateFile)
  template
    .replace("{{CSS_LINK}}", cssLink)
    .replace("{{JS_SCRIPT}}", jsScript)
}

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
      
      // Generate index.html from template
      val templateFile = baseDir / "src" / "templates" / "base.html"
      if (templateFile.exists()) {
        val cssLink = """<link rel="stylesheet" href="style.css">"""
        val jsScript = """<script type="text/javascript" src="akka-fsm-visualizer-fastopt.js"></script>"""
        val indexContent = processTemplate(templateFile, cssLink, jsScript)
        IO.write(devDir / "index.html", indexContent)
        log.info("Generated index.html from template")
      }
      
      // Copy CSS to dev folder
      val srcWebDir = baseDir / "src" / "web"
      IO.copyFile(srcWebDir / "style.css", devDir / "style.css")
      log.info("Copied static assets to dist/dev/")
    },
    
    // Development task that builds and copies assets
    TaskKey[Unit]("dev") := {
      val log = streams.value.log
      val baseDir = baseDirectory.value
      
      // First build JS
      val jsResult = (Compile / fastOptJS).value
      
      // Generate index.html from template
      val devDir = baseDir / "dist" / "dev"
      IO.createDirectory(devDir)
      val templateFile = baseDir / "src" / "templates" / "base.html"
      if (templateFile.exists()) {
        val cssLink = """<link rel="stylesheet" href="style.css">"""
        val jsScript = """<script type="text/javascript" src="akka-fsm-visualizer-fastopt.js"></script>"""
        val indexContent = processTemplate(templateFile, cssLink, jsScript)
        IO.write(devDir / "index.html", indexContent)
        log.info("Generated index.html from template")
      }
      
      // Copy CSS
      val srcWebDir = baseDir / "src" / "web"
      IO.copyFile(srcWebDir / "style.css", devDir / "style.css")
      
      log.info("Development build complete! Files ready in dist/dev/")
    },
    
    // Jekyll build settings
    TaskKey[Unit]("jekyllBuild") := {
      val log = streams.value.log
      val baseDir = baseDirectory.value
      val jekyllDir = baseDir / "dist" / "jekyll"
      
      // Create jekyll directory structure: apps/ with assets/
      IO.createDirectory(jekyllDir)
      val appsDir = jekyllDir / "apps"
      val jsDir = appsDir / "assets" / "js"
      val cssDir = appsDir / "assets" / "css"
      IO.createDirectory(jsDir)
      IO.createDirectory(cssDir)
      
      // Run fullOptJS first
      (Compile / fullOptJS).value
      
      // Copy optimized JS from dist/prod
      val jsFile = baseDir / "dist" / "prod" / "akka-fsm-visualizer-opt.js"
      if (jsFile.exists()) {
        IO.copyFile(jsFile, jsDir / "akka-fsm-visualizer.js")
        log.info("Copied optimized JS to jekyll/apps/assets/js/")
      }
      
      // Copy CSS from src/web
      val cssFile = baseDir / "src" / "web" / "style.css"
      if (cssFile.exists()) {
        IO.copyFile(cssFile, cssDir / "akka-fsm-visualizer.css")
        log.info("Copied CSS to jekyll/apps/assets/css/")
      }
      
      // Generate Jekyll HTML from template
      val templateFile = baseDir / "src" / "templates" / "base.html"
      val frontMatterFile = baseDir / "src" / "templates" / "jekyll-frontmatter.yaml"
      if (templateFile.exists() && frontMatterFile.exists()) {
        val frontMatter = IO.read(frontMatterFile)
        val cssLink = """<link rel="stylesheet" href="{{ '/apps/assets/css/akka-fsm-visualizer.css' | relative_url }}">"""
        val jsScript = """<script type="text/javascript" src="{{ '/apps/assets/js/akka-fsm-visualizer.js' | relative_url }}"></script>"""
        val htmlContent = processTemplate(templateFile, cssLink, jsScript)
        val jekyllContent = frontMatter + "\n" + htmlContent
        IO.write(appsDir / "akka-fsm-visualizer.html", jekyllContent)
        log.info("Generated Jekyll HTML from template with Liquid syntax")
      }
      
      log.info(s"Jekyll build complete! Files ready in: $jekyllDir")
      log.info("Structure: apps/akka-fsm-visualizer.html with assets/css/ and assets/js/")
      log.info("You can now copy the dist/jekyll/ folder contents to your Jekyll blog")
    },
  )