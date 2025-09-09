package ui.components

import org.scalajs.dom
import utils.DomUtils

/**
 * Theme management component
 * Single Responsibility: Dark/light theme switching and persistence
 */
class ThemeManager {
  
  private val THEME_KEY = "fsm-visualizer-theme"
  private var currentTheme: String = "light"
  
  /**
   * Initialize theme system
   */
  def initialize(): Unit = {
    loadSavedTheme()
    applyTheme(currentTheme)
  }
  
  /**
   * Toggle between dark and light themes
   */
  def toggleTheme(): String = {
    val newTheme = if (currentTheme == "dark") "light" else "dark"
    setTheme(newTheme)
    newTheme
  }
  
  /**
   * Set specific theme
   */
  def setTheme(theme: String): Unit = {
    currentTheme = theme
    applyTheme(theme)
    saveTheme(theme)
  }
  
  /**
   * Get current theme
   */
  def getCurrentTheme: String = currentTheme
  
  /**
   * Check if current theme is dark
   */
  def isDarkTheme: Boolean = currentTheme == "dark"
  
  private def applyTheme(theme: String): Unit = {
    // Set data-theme attribute on document element
    dom.document.documentElement.setAttribute("data-theme", theme)
    
    // Dispatch theme change event for components to listen to
    val event = new dom.CustomEvent("themeChanged", scala.scalajs.js.Dynamic.literal(
      detail = theme,
      bubbles = true
    ).asInstanceOf[dom.CustomEventInit])
    dom.document.dispatchEvent(event)
  }
  
  private def saveTheme(theme: String): Unit = {
    try {
      dom.window.localStorage.setItem(THEME_KEY, theme)
    } catch {
      case _: Exception =>
        // Silently fail if localStorage is not available
        dom.console.warn("Could not save theme preference")
    }
  }
  
  private def loadSavedTheme(): Unit = {
    try {
      Option(dom.window.localStorage.getItem(THEME_KEY)) match {
        case Some(savedTheme) if savedTheme == "dark" || savedTheme == "light" =>
          currentTheme = savedTheme
        case _ =>
          // Default to system preference if available
          currentTheme = detectSystemTheme()
      }
    } catch {
      case _: Exception =>
        // Default to light theme if localStorage fails
        currentTheme = "light"
    }
  }
  
  private def detectSystemTheme(): String = {
    try {
      if (dom.window.matchMedia("(prefers-color-scheme: dark)").matches) {
        "dark"
      } else {
        "light"
      }
    } catch {
      case _: Exception =>
        "light" // Fallback
    }
  }
  
  /**
   * Add theme change listener
   */
  def onThemeChange(callback: String => Unit): () => Unit = {
    val listener: scala.scalajs.js.Function1[dom.Event, Unit] = { event: dom.Event =>
      val customEvent = event.asInstanceOf[dom.CustomEvent]
      val theme = customEvent.detail.asInstanceOf[String]
      callback(theme)
    }
    
    dom.document.addEventListener("themeChanged", listener)
    
    // Return cleanup function
    () => dom.document.removeEventListener("themeChanged", listener)
  }
  
  /**
   * Update theme toggle button state
   */
  def updateToggleButton(buttonId: String): Unit = {
    DomUtils.getButtonElement(buttonId).foreach { button =>
      val icon = if (isDarkTheme) "☀️" else "🌙"
      val title = if (isDarkTheme) "Switch to light theme" else "Switch to dark theme"
      
      button.innerHTML = icon
      button.title = title
      button.setAttribute("aria-label", title)
    }
  }
}