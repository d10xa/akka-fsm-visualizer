package config

/**
 * Application configuration constants
 */
object AppConfig {
  // Debounce delays
  val ANALYSIS_DEBOUNCE_DELAY = 500 // milliseconds
  val MERMAID_EDIT_DEBOUNCE_DELAY = 300 // milliseconds
  
  // Monaco Editor configuration
  val MONACO_EDITOR_VERSION = "0.44.0"
  val MONACO_CDN_BASE = "https://cdn.jsdelivr.net/npm/monaco-editor@0.44.0/min/vs"
  
  // Mermaid configuration  
  val MERMAID_VERSION = "11.0.0"
  val MERMAID_CDN = "https://cdn.jsdelivr.net/npm/mermaid@11.0.0/dist/mermaid.min.js"
  
  // UI Constants
  val TOAST_DEFAULT_DURATION = 3000 // milliseconds
  val ZOOM_MIN = 0.1
  val ZOOM_MAX = 5.0
  val ZOOM_STEP = 0.1
  
  // CSS Classes
  val CSS_CLASSES = Map(
    "toast" -> "toast",
    "toastSuccess" -> "toast-success", 
    "toastWarning" -> "toast-warning",
    "toastError" -> "toast-error",
    "fullscreen" -> "fullscreen-overlay",
    "zoomContainer" -> "zoom-container"
  )
  
  // File upload
  val SUPPORTED_FILE_EXTENSIONS = Set(".scala")
  val MAX_FILE_SIZE_MB = 10
}