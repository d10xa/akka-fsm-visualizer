package ui.components

import org.scalajs.dom
import utils.{DomUtils, ToastNotifications}
import config.AppConfig

/**
 * File drag & drop uploader component
 * Single Responsibility: File upload handling and validation
 */
class FileUploader(
  dropZoneId: String,
  fileInputId: String,
  onFileLoaded: String => Unit,
  onError: String => Unit = _ => {}
) {
  
  private var isInitialized = false
  
  /**
   * Initialize file upload handlers
   */
  def initialize(): Unit = {
    if (!isInitialized) {
      setupDropZone()
      setupFileInput()
      isInitialized = true
    }
  }
  
  private def setupDropZone(): Unit = {
    DomUtils.getElementById[dom.HTMLElement](dropZoneId).foreach { dropZone =>
      
      // Prevent default drag behaviors
      List("dragenter", "dragover", "dragleave", "drop").foreach { event =>
        dropZone.addEventListener(event, { e: dom.Event =>
          e.preventDefault()
          e.stopPropagation()
        })
      }
      
      // Visual feedback on drag enter/over
      List("dragenter", "dragover").foreach { event =>
        dropZone.addEventListener(event, { _: dom.Event =>
          dropZone.classList.add("drag-over")
        })
      }
      
      List("dragleave", "drop").foreach { event =>
        dropZone.addEventListener(event, { _: dom.Event =>
          dropZone.classList.remove("drag-over")
        })
      }
      
      // Handle drop
      dropZone.addEventListener("drop", { e: dom.DragEvent =>
        e.preventDefault()
        e.stopPropagation()
        
        val files = e.dataTransfer.files
        if (files.length > 0) {
          val file = files(0)
          handleFile(file)
        }
      })
    }
  }
  
  private def setupFileInput(): Unit = {
    DomUtils.getInputElement(fileInputId).foreach { fileInput =>
      fileInput.addEventListener("change", { _: dom.Event =>
        if (fileInput.files.length > 0) {
          val file = fileInput.files(0)
          handleFile(file)
        }
      })
    }
  }
  
  private def handleFile(file: dom.File): Unit = {
    // Validate file
    if (!validateFile(file)) {
      return
    }
    
    // Read file
    DomUtils.readFile(
      file = file,
      onSuccess = { content =>
        onFileLoaded(content)
        showSuccessToast(file.name)
      },
      onError = { error =>
        onError(error)
        showErrorToast(error)
      }
    )
  }
  
  private def validateFile(file: dom.File): Boolean = {
    // Check file extension
    if (!DomUtils.hasSupportedExtension(file.name, AppConfig.SUPPORTED_FILE_EXTENSIONS)) {
      val error = s"Unsupported file type. Supported extensions: ${AppConfig.SUPPORTED_FILE_EXTENSIONS.mkString(", ")}"
      onError(error)
      showErrorToast(error)
      return false
    }
    
    // Check file size
    val maxSizeBytes = AppConfig.MAX_FILE_SIZE_MB * 1024 * 1024
    if (file.size > maxSizeBytes) {
      val error = s"File too large. Maximum size: ${AppConfig.MAX_FILE_SIZE_MB}MB"
      onError(error)
      showErrorToast(error)
      return false
    }
    
    true
  }
  
  private def showSuccessToast(filename: String): Unit = {
    getToastContainer().foreach { container =>
      ToastNotifications.showSuccess(
        container = container,
        title = "File Loaded",
        message = s"Successfully loaded $filename"
      )
    }
  }
  
  private def showErrorToast(message: String): Unit = {
    getToastContainer().foreach { container =>
      ToastNotifications.showError(
        container = container,
        title = "Upload Error",
        message = message
      )
    }
  }
  
  private def getToastContainer(): Option[dom.Element] = {
    // Try to find toast container, create if doesn't exist
    Option(dom.document.querySelector(".toast-container")).orElse {
      val container = dom.document.createElement("div")
      container.setAttribute("class", "toast-container")
      dom.document.body.appendChild(container)
      Some(container)
    }
  }
  
  /**
   * Programmatically trigger file selection dialog
   */
  def triggerFileDialog(): Unit = {
    DomUtils.getInputElement(fileInputId).foreach(_.click())
  }
  
  /**
   * Reset file input
   */
  def reset(): Unit = {
    DomUtils.getInputElement(fileInputId).foreach(_.value = "")
  }
}