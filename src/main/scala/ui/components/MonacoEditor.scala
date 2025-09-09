package ui.components

import org.scalajs.dom
import scala.scalajs.js
import utils.DomUtils
import config.AppConfig

/**
 * Monaco Editor component wrapper
 * Single Responsibility: Monaco Editor lifecycle and configuration
 */
class MonacoEditor(containerId: String) {
  
  private var editor: Option[js.Dynamic] = None
  private var isInitialized = false
  
  /**
   * Initialize Monaco Editor with GitHub-like dark theme
   */
  def initialize(
    initialValue: String = "",
    theme: String = "vs",
    onChange: String => Unit = _ => {}
  ): Option[js.Dynamic] = {
    DomUtils.getElementById[dom.HTMLDivElement](containerId).flatMap { container =>
      try {
        val monaco = dom.window.asInstanceOf[js.Dynamic].monaco
        
        // Define GitHub-like dark theme for better syntax highlighting
        monaco.editor.defineTheme("github-dark", js.Dynamic.literal(
          base = "vs-dark",
          inherit = true,
          rules = js.Array(
            // Keywords (class, def, val, var, etc.) - bright blue
            js.Dynamic.literal(token = "keyword", foreground = "79c0ff"),
            // Strings - green
            js.Dynamic.literal(token = "string", foreground = "a5d6ff"),
            // Comments - gray
            js.Dynamic.literal(token = "comment", foreground = "8b949e", fontStyle = "italic"),
            // Numbers - light blue
            js.Dynamic.literal(token = "number", foreground = "79c0ff"),
            // Types and classes - yellow
            js.Dynamic.literal(token = "type", foreground = "ffa657"),
            // Function names - purple/pink
            js.Dynamic.literal(token = "identifier.function", foreground = "d2a8ff"),
            // Variables - white/light
            js.Dynamic.literal(token = "identifier", foreground = "e6edf3"),
            // Operators - light
            js.Dynamic.literal(token = "delimiter", foreground = "e6edf3"),
            // Special keywords (import, package) - pink
            js.Dynamic.literal(token = "keyword.import", foreground = "ff7b72"),
            js.Dynamic.literal(token = "keyword.package", foreground = "ff7b72"),
            // Annotations (@) - yellow
            js.Dynamic.literal(token = "annotation", foreground = "ffa657")
          ),
          colors = js.Dynamic.literal(
            "editor.background" -> "#0d1117",
            "editor.foreground" -> "#e6edf3",
            "editorLineNumber.foreground" -> "#7d8590",
            "editorLineNumber.activeForeground" -> "#e6edf3",
            "editor.selectionBackground" -> "#264f78",
            "editor.inactiveSelectionBackground" -> "#3a3d41",
            "editorCursor.foreground" -> "#e6edf3",
            "editor.lineHighlightBackground" -> "#161b22",
            "editorIndentGuide.background" -> "#21262d",
            "editorIndentGuide.activeBackground" -> "#30363d",
            "editor.findMatchBackground" -> "#ffd33d44",
            "editor.findMatchHighlightBackground" -> "#ffd33d22"
          )
        ))
        
        // Detect current theme
        val isDarkTheme = dom.document.documentElement.getAttribute("data-theme") == "dark"
        val editorTheme = if (isDarkTheme) "github-dark" else "vs"
        
        val editorInstance = monaco.editor.create(container, js.Dynamic.literal(
          value = initialValue,
          language = "scala",
          theme = editorTheme,
          fontSize = 13,
          fontFamily = "'Monaco', 'Menlo', 'Consolas', monospace",
          lineNumbers = "on",
          glyphMargin = false,
          minimap = js.Dynamic.literal(enabled = false),
          scrollBeyondLastLine = false,
          automaticLayout = true,
          wordWrap = "on",
          tabSize = 2,
          insertSpaces = true
        ))
        
        // Set up change listener
        editorInstance.onDidChangeModelContent { (_: js.Any) =>
          val currentValue = editorInstance.getValue().asInstanceOf[String]
          onChange(currentValue)
        }
        
        editor = Some(editorInstance)
        isInitialized = true
        Some(editorInstance)
        
      } catch {
        case ex: Exception =>
          dom.console.error(s"Failed to initialize Monaco Editor: ${ex.getMessage}")
          None
      }
    }
  }
  
  /**
   * Update editor value without triggering change event
   */
  def setValue(value: String): Unit = {
    editor.foreach { ed =>
      val currentValue = ed.getValue().asInstanceOf[String]
      if (currentValue != value) {
        ed.setValue(value)
      }
    }
  }
  
  /**
   * Get current editor value
   */
  def getValue: String = {
    editor.map(_.getValue().asInstanceOf[String]).getOrElse("")
  }
  
  /**
   * Switch editor theme
   */
  def setTheme(isDark: Boolean): Unit = {
    editor.foreach { ed =>
      val monaco = dom.window.asInstanceOf[js.Dynamic].monaco
      val themeToUse = if (isDark) "github-dark" else "vs"
      monaco.editor.setTheme(themeToUse)
    }
  }
  
  /**
   * Focus the editor
   */
  def focus(): Unit = {
    editor.foreach(_.focus())
  }
  
  /**
   * Check if editor is initialized
   */
  def isReady: Boolean = isInitialized && editor.isDefined
  
  /**
   * Dispose the editor instance
   */
  def dispose(): Unit = {
    editor.foreach(_.dispose())
    editor = None
    isInitialized = false
  }
  
  /**
   * Get underlying Monaco editor instance
   */
  def getEditor: Option[js.Dynamic] = editor
}