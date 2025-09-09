package utils

import org.scalajs.dom
import scala.scalajs.js

/**
 * Utility for debouncing function calls
 */
class DebounceTimer(delayMs: Int) {
  private var timerId: Option[Int] = None
  
  /**
   * Execute function after delay, canceling any pending execution
   */
  def debounce(fn: () => Unit): Unit = {
    // Cancel existing timer
    timerId.foreach(dom.window.clearTimeout)
    
    // Set new timer
    timerId = Some(dom.window.setTimeout(() => {
      timerId = None
      fn()
    }, delayMs))
  }
  
  /**
   * Cancel any pending execution
   */
  def cancel(): Unit = {
    timerId.foreach(dom.window.clearTimeout)
    timerId = None
  }
  
  /**
   * Check if there's a pending execution
   */
  def isPending: Boolean = timerId.isDefined
}

/**
 * Companion object with factory methods
 */
object DebounceTimer {
  /**
   * Create a new debounce timer with specified delay
   */
  def apply(delayMs: Int): DebounceTimer = new DebounceTimer(delayMs)
  
  /**
   * Create debounced function that can be called multiple times
   */
  def createDebouncedFunction(delayMs: Int)(fn: () => Unit): () => Unit = {
    val timer = new DebounceTimer(delayMs)
    () => timer.debounce(fn)
  }
}