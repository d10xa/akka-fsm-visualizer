package ui.components

import org.scalajs.dom
import scala.scalajs.js
import utils.DomUtils
import config.AppConfig

/**
 * Mermaid diagram renderer component
 * Single Responsibility: Mermaid diagram rendering and lifecycle
 */
class MermaidRenderer {
  
  private var isInitialized = false
  
  /**
   * Initialize Mermaid with theme configuration
   */
  def initialize(): Unit = {
    if (!isInitialized) {
      try {
        val mermaid = dom.window.asInstanceOf[js.Dynamic].mermaid
        
        // Detect current theme
        val isDarkTheme = dom.document.documentElement.getAttribute("data-theme") == "dark"
        val themeConfig = if (isDarkTheme) "dark" else "default"
        
        mermaid.initialize(js.Dynamic.literal(
          startOnLoad = false,
          theme = themeConfig,
          securityLevel = "loose",
          fontFamily = "arial",
          deterministicIds = true,
          maxTextSize = 50000,
          themeVariables = if (isDarkTheme) {
            js.Dynamic.literal(
              background = "#2d2d2d",
              primaryColor = "#4dabf7",
              primaryTextColor = "#e0e0e0",
              primaryBorderColor = "#3d3d3d",
              lineColor = "#b0b0b0",
              secondaryColor = "#3d3d3d",
              tertiaryColor = "#2d2d2d"
            )
          } else {
            js.undefined
          }
        ))
        
        isInitialized = true
      } catch {
        case ex: Exception =>
          dom.console.error(s"Failed to initialize Mermaid: ${ex.getMessage}")
      }
    }
  }
  
  /**
   * Render Mermaid diagram in container
   */
  def renderDiagram(
    mermaidCode: String, 
    container: dom.Element,
    onSuccess: () => Unit = () => {},
    onError: String => Unit = _ => {}
  ): Unit = {
    if (!isInitialized) initialize()
    
    try {
      val mermaid = dom.window.asInstanceOf[js.Dynamic].mermaid
      
      // Clear container
      container.innerHTML = ""
      
      // Create unique ID for this diagram
      val diagramId = s"mermaid-${System.currentTimeMillis()}"
      
      // Render the diagram
      mermaid.render(diagramId, mermaidCode).`then` { (result: js.Dynamic) =>
        try {
          container.innerHTML = result.svg.asInstanceOf[String]
          onSuccess()
        } catch {
          case ex: Exception =>
            val errorMsg = s"Failed to render Mermaid diagram: ${ex.getMessage}"
            container.innerHTML = s"""<div class="error-message">$errorMsg</div>"""
            onError(errorMsg)
        }
      }.`catch` { (error: js.Any) =>
        val errorMsg = s"Mermaid rendering error: ${error.toString}"
        container.innerHTML = s"""<div class="error-message">$errorMsg</div>"""
        onError(errorMsg)
      }
      
    } catch {
      case ex: Exception =>
        val errorMsg = s"Failed to render diagram: ${ex.getMessage}"
        container.innerHTML = s"""<div class="error-message">$errorMsg</div>"""
        onError(errorMsg)
    }
  }
  
  /**
   * Update Mermaid theme
   */
  def updateTheme(isDark: Boolean): Unit = {
    try {
      val mermaid = dom.window.asInstanceOf[js.Dynamic].mermaid
      val mermaidTheme = if (isDark) "dark" else "default"
      
      mermaid.initialize(js.Dynamic.literal(
        startOnLoad = false,
        theme = mermaidTheme,
        securityLevel = "loose",
        fontFamily = "arial",
        deterministicIds = true,
        maxTextSize = 50000,
        themeVariables = if (isDark) {
          js.Dynamic.literal(
            background = "#2d2d2d",
            primaryColor = "#4dabf7",
            primaryTextColor = "#e0e0e0",
            primaryBorderColor = "#3d3d3d",
            lineColor = "#b0b0b0",
            secondaryColor = "#3d3d3d",
            tertiaryColor = "#2d2d2d"
          )
        } else {
          js.undefined
        }
      ))
    } catch {
      case ex: Exception =>
        dom.console.error(s"Failed to update Mermaid theme: ${ex.getMessage}")
    }
  }
  
  /**
   * Export diagram as SVG string
   */
  def exportSvg(container: dom.Element): Option[String] = {
    try {
      Option(container.querySelector("svg")).map { svg =>
        // Get the SVG element
        val svgElement = svg.asInstanceOf[dom.svg.SVG]
        
        // Create a proper SVG string with XML declaration
        val svgContent = svgElement.outerHTML
        svgContent
      }
    } catch {
      case ex: Exception =>
        dom.console.error(s"Failed to export SVG: ${ex.getMessage}")
        None
    }
  }
  
  /**
   * Export diagram as PNG (using canvas)
   */
  def exportPng(
    container: dom.Element, 
    scale: Double = 2.0,
    onSuccess: String => Unit,
    onError: String => Unit
  ): Unit = {
    try {
      exportSvg(container) match {
        case Some(svgString) =>
          convertSvgToPng(svgString, scale, onSuccess, onError)
        case None =>
          onError("No SVG found to export")
      }
    } catch {
      case ex: Exception =>
        onError(s"PNG export failed: ${ex.getMessage}")
    }
  }
  
  private def convertSvgToPng(
    svgString: String, 
    scale: Double,
    onSuccess: String => Unit,
    onError: String => Unit
  ): Unit = {
    val canvas = dom.document.createElement("canvas").asInstanceOf[dom.HTMLCanvasElement]
    val ctx = canvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
    
    val img = dom.document.createElement("img").asInstanceOf[dom.HTMLImageElement]
    
    img.onload = { _: dom.Event =>
      try {
        canvas.width = (img.width * scale).toInt
        canvas.height = (img.height * scale).toInt
        
        ctx.scale(scale, scale)
        ctx.drawImage(img, 0, 0)
        
        val dataUrl = canvas.toDataURL("image/png")
        onSuccess(dataUrl)
      } catch {
        case ex: Exception =>
          onError(s"Canvas conversion failed: ${ex.getMessage}")
      }
    }
    
    img.addEventListener("error", { _: dom.Event =>
      onError("Failed to load SVG for PNG conversion")
    })
    
    // Convert SVG to data URL
    val svgBlob = new dom.Blob(js.Array(svgString), dom.BlobPropertyBag(`type` = "image/svg+xml;charset=utf-8"))
    val url = dom.URL.createObjectURL(svgBlob)
    img.src = url
  }
}