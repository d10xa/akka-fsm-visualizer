package utils

import org.scalajs.dom
import scala.scalajs.js

/**
 * DOM utility functions for common operations
 */
object DomUtils {
  
  /**
   * Safely get element by ID with proper typing
   */
  def getElementById[T <: dom.Element](id: String): Option[T] = {
    Option(dom.document.getElementById(id)).map(_.asInstanceOf[T])
  }
  
  /**
   * Get HTML input element by ID
   */
  def getInputElement(id: String): Option[dom.HTMLInputElement] = {
    getElementById[dom.HTMLInputElement](id)
  }
  
  /**
   * Get HTML textarea element by ID  
   */
  def getTextareaElement(id: String): Option[dom.HTMLTextAreaElement] = {
    getElementById[dom.HTMLTextAreaElement](id)
  }
  
  /**
   * Get HTML div element by ID
   */
  def getDivElement(id: String): Option[dom.HTMLDivElement] = {
    getElementById[dom.HTMLDivElement](id)
  }
  
  /**
   * Get HTML button element by ID
   */
  def getButtonElement(id: String): Option[dom.HTMLButtonElement] = {
    getElementById[dom.HTMLButtonElement](id)
  }
  
  /**
   * Create and dispatch a custom event
   */
  def dispatchCustomEvent(element: dom.Element, eventType: String, detail: js.Any = js.undefined): Unit = {
    val event = new dom.CustomEvent(eventType, js.Dynamic.literal(
      bubbles = true,
      cancelable = true,
      detail = detail
    ).asInstanceOf[dom.CustomEventInit])
    element.dispatchEvent(event)
  }
  
  /**
   * Add event listener with proper cleanup
   */
  def addEventListenerWithCleanup[T <: dom.Event](
    element: dom.EventTarget,
    eventType: String,
    handler: T => Unit
  ): () => Unit = {
    val listener: js.Function1[T, Unit] = handler
    element.addEventListener(eventType, listener)
    // Return cleanup function
    () => element.removeEventListener(eventType, listener)
  }
  
  /**
   * Safe file reading with error handling
   */
  def readFile(file: dom.File, onSuccess: String => Unit, onError: String => Unit): Unit = {
    val reader = new dom.FileReader()
    
    reader.onload = { _: dom.Event =>
      onSuccess(reader.result.asInstanceOf[String])
    }
    
    reader.onerror = { _: dom.Event =>
      onError("Failed to read file")
    }
    
    reader.readAsText(file)
  }
  
  /**
   * Check if file has supported extension
   */
  def hasSupportedExtension(filename: String, supportedExtensions: Set[String]): Boolean = {
    supportedExtensions.exists(ext => filename.toLowerCase.endsWith(ext.toLowerCase))
  }
  
  /**
   * Format file size for display
   */
  def formatFileSize(bytes: Long): String = {
    if (bytes < 1024) s"${bytes}B"
    else if (bytes < 1024 * 1024) f"${bytes / 1024.0}%.1fKB"
    else f"${bytes / (1024.0 * 1024.0)}%.1fMB"
  }
}