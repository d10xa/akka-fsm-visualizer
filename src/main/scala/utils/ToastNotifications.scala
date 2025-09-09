package utils

import org.scalajs.dom
import config.AppConfig

/**
 * Toast notification system
 */
object ToastNotifications {
  
  sealed trait ToastType {
    def cssClass: String
  }
  
  case object Success extends ToastType {
    val cssClass = AppConfig.CSS_CLASSES("toastSuccess")
  }
  
  case object Warning extends ToastType {
    val cssClass = AppConfig.CSS_CLASSES("toastWarning") 
  }
  
  case object Error extends ToastType {
    val cssClass = AppConfig.CSS_CLASSES("toastError")
  }
  
  /**
   * Show toast notification
   */
  def show(
    container: dom.Element,
    title: String,
    message: String,
    toastType: ToastType = Success,
    duration: Int = AppConfig.TOAST_DEFAULT_DURATION
  ): Unit = {
    val toast = createToastElement(title, message, toastType)
    container.appendChild(toast)
    
    // Auto-remove after duration
    dom.window.setTimeout(() => {
      if (container.contains(toast)) {
        removeToast(toast)
      }
    }, duration)
    
    // Add click-to-dismiss
    toast.addEventListener("click", { _: dom.Event =>
      removeToast(toast)
    })
  }
  
  /**
   * Show success toast
   */
  def showSuccess(container: dom.Element, title: String, message: String): Unit = {
    show(container, title, message, Success)
  }
  
  /**
   * Show warning toast  
   */
  def showWarning(container: dom.Element, title: String, message: String): Unit = {
    show(container, title, message, Warning)
  }
  
  /**
   * Show error toast
   */
  def showError(container: dom.Element, title: String, message: String): Unit = {
    show(container, title, message, Error)
  }
  
  private def createToastElement(title: String, message: String, toastType: ToastType): dom.HTMLDivElement = {
    val toast = dom.document.createElement("div").asInstanceOf[dom.HTMLDivElement]
    toast.className = s"${AppConfig.CSS_CLASSES("toast")} ${toastType.cssClass}"
    
    toast.innerHTML = s"""
      <div class="toast-title">$title</div>
      <div class="toast-message">$message</div>
      <div class="toast-close">×</div>
    """
    
    toast
  }
  
  private def removeToast(toast: dom.Element): Unit = {
    toast.classList.add("toast-removing")
    
    // Wait for animation, then remove
    dom.window.setTimeout(() => {
      if (toast.parentNode != null) {
        toast.parentNode.removeChild(toast)
      }
    }, 300)
  }
}