import org.scalajs.dom
import org.scalajs.dom.{document, console}
import scala.scalajs.js.annotation.JSExportTopLevel

@JSExportTopLevel("FsmVisualizerApp")
object FsmVisualizerApp {
  
  private var analysisTimer: Option[Int] = None
  
  // Zoom state variables
  private var currentZoom: Double = 1.0
  private var currentPanX: Double = 0.0
  private var currentPanY: Double = 0.0
  private var isDragging: Boolean = false
  private var dragStartX: Double = 0.0
  private var dragStartY: Double = 0.0
  
  // Mermaid editing state
  private var lastGeneratedMermaid: String = ""
  private var isCodeManuallyEdited: Boolean = false
  private var mermaidEditTimer: Option[Int] = None
  
  def main(args: Array[String]): Unit = {
    dom.document.addEventListener("DOMContentLoaded", { (_: dom.Event) =>
      setupUI()
    })
  }
  
  private def setupUI(): Unit = {
    val fileInput = Option(document.getElementById("fileInput")).map(_.asInstanceOf[dom.HTMLInputElement])
    val codeTextarea = Option(document.getElementById("codeInput")).map(_.asInstanceOf[dom.HTMLTextAreaElement])
    val copyButton = Option(document.getElementById("copyButton")).map(_.asInstanceOf[dom.HTMLButtonElement])
    val toggleButton = Option(document.getElementById("toggleSource")).map(_.asInstanceOf[dom.HTMLButtonElement])
    val exportSvgButton = Option(document.getElementById("exportSvg")).map(_.asInstanceOf[dom.HTMLButtonElement])
    val exportPngButton = Option(document.getElementById("exportPng")).map(_.asInstanceOf[dom.HTMLButtonElement])
    val fullscreenButton = Option(document.getElementById("fullscreen")).map(_.asInstanceOf[dom.HTMLButtonElement])
    val fullscreenModal = Option(document.getElementById("fullscreenModal")).map(_.asInstanceOf[dom.HTMLDivElement])
    val closeFullscreenButton = Option(document.getElementById("closeFullscreen")).map(_.asInstanceOf[dom.HTMLButtonElement])
    val fullscreenDiagram = Option(document.getElementById("fullscreenDiagram")).map(_.asInstanceOf[dom.HTMLDivElement])
    val zoomContainer = Option(document.getElementById("zoomContainer")).map(_.asInstanceOf[dom.HTMLDivElement])
    val zoomInButton = Option(document.getElementById("zoomIn")).map(_.asInstanceOf[dom.HTMLButtonElement])
    val zoomOutButton = Option(document.getElementById("zoomOut")).map(_.asInstanceOf[dom.HTMLButtonElement])
    val zoomResetButton = Option(document.getElementById("zoomReset")).map(_.asInstanceOf[dom.HTMLButtonElement])
    val fitToScreenButton = Option(document.getElementById("fitToScreen")).map(_.asInstanceOf[dom.HTMLButtonElement])
    val zoomLevelSpan = Option(document.getElementById("zoomLevel")).map(_.asInstanceOf[dom.HTMLSpanElement])
    val regenerateButton = Option(document.getElementById("regenerateCode")).map(_.asInstanceOf[dom.HTMLButtonElement])
    val modifiedIndicator = Option(document.getElementById("modifiedIndicator")).map(_.asInstanceOf[dom.HTMLSpanElement])
    val errorDiv = Option(document.getElementById("error")).map(_.asInstanceOf[dom.HTMLDivElement])
    val mermaidOutput = Option(document.getElementById("mermaidOutput")).map(_.asInstanceOf[dom.HTMLTextAreaElement])
    val diagramContainer = Option(document.getElementById("diagramContainer")).map(_.asInstanceOf[dom.HTMLDivElement])
    
    // Check if all required elements are present
    if (fileInput.isEmpty || codeTextarea.isEmpty || copyButton.isEmpty || toggleButton.isEmpty || 
        exportSvgButton.isEmpty || exportPngButton.isEmpty || fullscreenButton.isEmpty ||
        fullscreenModal.isEmpty || closeFullscreenButton.isEmpty || fullscreenDiagram.isEmpty ||
        zoomContainer.isEmpty || zoomInButton.isEmpty || zoomOutButton.isEmpty || 
        zoomResetButton.isEmpty || fitToScreenButton.isEmpty || zoomLevelSpan.isEmpty ||
        regenerateButton.isEmpty || modifiedIndicator.isEmpty ||
        errorDiv.isEmpty || mermaidOutput.isEmpty || diagramContainer.isEmpty) {
      println("Some DOM elements not found, retrying in 100ms...")
      dom.window.setTimeout(() => setupUI(), 100)
      return
    }
    
    // Extract values from Options for cleaner code
    val fileInputEl = fileInput.get
    val codeTextareaEl = codeTextarea.get
    val copyButtonEl = copyButton.get
    val toggleButtonEl = toggleButton.get
    val exportSvgButtonEl = exportSvgButton.get
    val exportPngButtonEl = exportPngButton.get  
    val fullscreenButtonEl = fullscreenButton.get
    val fullscreenModalEl = fullscreenModal.get
    val closeFullscreenButtonEl = closeFullscreenButton.get
    val fullscreenDiagramEl = fullscreenDiagram.get
    val zoomContainerEl = zoomContainer.get
    val zoomInButtonEl = zoomInButton.get
    val zoomOutButtonEl = zoomOutButton.get
    val zoomResetButtonEl = zoomResetButton.get
    val fitToScreenButtonEl = fitToScreenButton.get
    val zoomLevelSpanEl = zoomLevelSpan.get
    val regenerateButtonEl = regenerateButton.get
    val modifiedIndicatorEl = modifiedIndicator.get
    val errorDivEl = errorDiv.get
    val mermaidOutputEl = mermaidOutput.get
    val diagramContainerEl = diagramContainer.get
    
    // Load example code
    loadExampleCode(codeTextareaEl)
    
    // File upload handler
    fileInputEl.addEventListener("change", { (_: dom.Event) =>
      val files = fileInputEl.files
      if (files.length > 0) {
        val file = files(0)
        val reader = new dom.FileReader()
        reader.onload = { (_: dom.Event) =>
          codeTextareaEl.value = reader.result.asInstanceOf[String]
          clearError(errorDivEl)
          analyzeCode(codeTextareaEl.value, mermaidOutputEl, diagramContainerEl, errorDivEl)
        }
        reader.readAsText(file)
      }
    })
    
    // Auto-analyze on code change with debouncing
    codeTextareaEl.addEventListener("input", { (_: dom.Event) =>
      // Clear any existing timer
      analysisTimer.foreach(timer => dom.window.clearTimeout(timer))
      
      // Set a new timer to debounce the analysis
      val timerId = dom.window.setTimeout(() => {
        val code = codeTextareaEl.value
        if (code.trim.nonEmpty) {
          analyzeCode(code, mermaidOutputEl, diagramContainerEl, errorDivEl)
        } else {
          mermaidOutputEl.value = ""
          diagramContainerEl.innerHTML = """<div class="placeholder-text">Enter Akka FSM code to see the diagram</div>"""
          clearError(errorDivEl)
        }
      }, 300) // 300ms debounce delay
      
      analysisTimer = Some(timerId)
    })
    
    
    // Initialize display states
    mermaidOutputEl.style.display = "none"
    diagramContainerEl.style.display = "block"
    
    // Toggle source view
    toggleButtonEl.addEventListener("click", { (_: dom.Event) =>
      val isSourceVisible = mermaidOutputEl.style.display == "block"
      if (isSourceVisible) {
        // Switching from code view to diagram view
        mermaidOutputEl.style.display = "none"
        diagramContainerEl.style.display = "block"
        toggleButtonEl.textContent = "Show Code"
        
        // Re-render diagram with current Mermaid code (in case it was manually edited)
        val currentMermaidCode = mermaidOutputEl.value
        if (currentMermaidCode.trim.nonEmpty) {
          try {
            renderMermaidDiagram(currentMermaidCode, diagramContainerEl)
            clearError(errorDivEl)
          } catch {
            case ex: Exception =>
              showError(errorDivEl, s"Mermaid rendering error: ${ex.getMessage}")
          }
        } else {
          diagramContainerEl.innerHTML = """<div class="placeholder-text">Enter Mermaid code to see the diagram</div>"""
        }
      } else {
        // Switching from diagram view to code view
        mermaidOutputEl.style.display = "block"
        diagramContainerEl.style.display = "none"
        toggleButtonEl.textContent = "Show Diagram"
      }
    })
    
    // Copy button handler
    copyButtonEl.addEventListener("click", { (_: dom.Event) =>
      mermaidOutputEl.select()
      dom.document.execCommand("copy")
      
      // Show feedback
      val originalText = copyButtonEl.textContent
      copyButtonEl.textContent = "✓ Copied!"
      dom.window.setTimeout(() => {
        copyButtonEl.textContent = originalText
      }, 2000)
    })
    
    // Export SVG button handler
    exportSvgButtonEl.addEventListener("click", { (_: dom.Event) =>
      exportDiagramAsSvg(diagramContainerEl)
    })
    
    // Export PNG button handler
    exportPngButtonEl.addEventListener("click", { (_: dom.Event) =>
      exportDiagramAsPng(diagramContainerEl)
    })
    
    // Fullscreen button handler
    fullscreenButtonEl.addEventListener("click", { (_: dom.Event) =>
      showFullscreen(diagramContainerEl, fullscreenModalEl, fullscreenDiagramEl, zoomContainerEl, zoomLevelSpanEl)
    })
    
    // Close fullscreen handler
    closeFullscreenButtonEl.addEventListener("click", { (_: dom.Event) =>
      hideFullscreen(fullscreenModalEl)
    })
    
    // Close fullscreen on modal background click
    fullscreenModalEl.addEventListener("click", { (event: dom.Event) =>
      if (event.target == fullscreenModalEl) {
        hideFullscreen(fullscreenModalEl)
      }
    })
    
    // Zoom controls
    zoomInButtonEl.addEventListener("click", { (_: dom.Event) =>
      zoomIn(zoomContainerEl, zoomLevelSpanEl)
    })
    
    zoomOutButtonEl.addEventListener("click", { (_: dom.Event) =>
      zoomOut(zoomContainerEl, zoomLevelSpanEl)
    })
    
    zoomResetButtonEl.addEventListener("click", { (_: dom.Event) =>
      resetZoom(zoomContainerEl, zoomLevelSpanEl)
    })
    
    fitToScreenButtonEl.addEventListener("click", { (_: dom.Event) =>
      fitToScreen(zoomContainerEl, zoomLevelSpanEl)
    })
    
    // Regenerate code button handler
    regenerateButtonEl.addEventListener("click", { (_: dom.Event) =>
      if (isCodeManuallyEdited) {
        if (dom.window.confirm("This will replace your manually edited Mermaid code with a new version generated from the Scala code. Are you sure?")) {
          regenerateMermaidCode(codeTextareaEl.value, mermaidOutputEl, diagramContainerEl, errorDivEl, regenerateButtonEl, modifiedIndicatorEl)
        }
      }
    })
    
    // Mermaid code editing handler
    mermaidOutputEl.addEventListener("input", { (_: dom.Event) =>
      onMermaidCodeEdit(mermaidOutputEl, diagramContainerEl, errorDivEl, regenerateButtonEl, modifiedIndicatorEl)
    })
    
    // Initialize mermaid with retry mechanism
    initializeMermaid()
    
    // Initial analysis with example code
    analyzeCode(codeTextareaEl.value, mermaidOutputEl, diagramContainerEl, errorDivEl)
  }
  
  private def loadExampleCode(textarea: dom.HTMLTextAreaElement): Unit = {
    val exampleCode = """import akka.actor.{Actor, FSM, Props}
      |import scala.concurrent.duration._
      |
      |// События
      |sealed trait OrderEvent
      |case object PlaceOrder extends OrderEvent
      |case object PaymentReceived extends OrderEvent
      |case object PaymentFailed extends OrderEvent
      |case object ItemsReserved extends OrderEvent
      |case object OutOfStock extends OrderEvent
      |case object OrderShipped extends OrderEvent
      |case object OrderDelivered extends OrderEvent
      |case object OrderCancelled extends OrderEvent
      |case object RefundIssued extends OrderEvent
      |
      |// Состояния
      |sealed trait OrderState
      |object State {
      |  case object WaitingForOrder extends OrderState
      |  case object PaymentPending extends OrderState
      |  case object PaymentFailed extends OrderState
      |  case object ReservingItems extends OrderState
      |  case object ReadyToShip extends OrderState
      |  case object Shipped extends OrderState
      |  case object Delivered extends OrderState
      |  case object Cancelled extends OrderState
      |  case object Refunded extends OrderState
      |}
      |
      |// Данные
      |sealed trait OrderData
      |case object NoData extends OrderData
      |case class OrderInfo(orderId: String, amount: Double) extends OrderData
      |
      |class OrderProcessingFSM extends Actor with FSM[OrderState, OrderData] {
      |
      |  when(State.WaitingForOrder) {
      |    case Event(PlaceOrder, _) =>
      |      goto(State.PaymentPending) using OrderInfo("ORDER-123", 99.99)
      |  }
      |
      |  when(State.PaymentPending) {
      |    case Event(PaymentReceived, orderInfo) =>
      |      goto(State.ReservingItems) using orderInfo
      |      
      |    case Event(PaymentFailed, orderInfo) =>
      |      handlePaymentFailure(orderInfo)
      |  }
      |
      |  when(State.PaymentFailed) {
      |    case Event(PaymentReceived, orderInfo) =>
      |      goto(State.ReservingItems) using orderInfo
      |      
      |    case Event(OrderCancelled, _) =>
      |      goto(State.Cancelled) using NoData
      |  }
      |
      |  when(State.ReservingItems) {
      |    case Event(ItemsReserved, orderInfo) =>
      |      goto(State.ReadyToShip) using orderInfo
      |      
      |    case Event(OutOfStock, orderInfo) =>
      |      handleOutOfStock(orderInfo)
      |      
      |    case Event(PaymentFailed, orderInfo) =>
      |      // Повторная резервация при проблемах с оплатой - остаемся в том же состоянии
      |      goto(State.ReservingItems) using orderInfo
      |  }
      |
      |  when(State.ReadyToShip) {
      |    case Event(OrderShipped, orderInfo) =>
      |      goto(State.Shipped) using orderInfo
      |      
      |    case Event(OrderCancelled, orderInfo) =>
      |      processRefund(orderInfo)
      |  }
      |
      |  when(State.Shipped) {
      |    case Event(OrderDelivered, orderInfo) =>
      |      goto(State.Delivered) using orderInfo
      |      
      |    case Event(OrderCancelled, orderInfo) =>
      |      processRefund(orderInfo)
      |  }
      |
      |  when(State.Delivered) {
      |    case Event(OrderCancelled, orderInfo) =>
      |      processRefund(orderInfo)
      |  }
      |
      |  when(State.Cancelled) {
      |    case Event(RefundIssued, _) =>
      |      goto(State.Refunded) using NoData
      |  }
      |
      |  when(State.Refunded) {
      |    case Event(PlaceOrder, _) =>
      |      goto(State.PaymentPending) using OrderInfo("ORDER-NEW", 149.99)
      |  }
      |
      |  // Вспомогательные функции с переходами состояний
      |  def handlePaymentFailure(orderInfo: OrderInfo): State = {
      |    if (orderInfo.amount > 100.0) {
      |      goto(State.PaymentFailed) using orderInfo
      |    } else {
      |      goto(State.Cancelled) using NoData
      |    }
      |  }
      |
      |  def handleOutOfStock(orderInfo: OrderInfo): State = {
      |    findAlternatives(orderInfo)
      |  }
      |
      |  def findAlternatives(orderInfo: OrderInfo): State = {
      |    orderInfo.amount match {
      |      case amount if amount > 50.0 =>
      |        goto(State.Cancelled) using orderInfo
      |      case _ =>
      |        goto(State.PaymentFailed) using orderInfo
      |    }
      |  }
      |
      |  def processRefund(orderInfo: OrderInfo): State = {
      |    goto(State.Cancelled) using orderInfo
      |  }
      |
      |  startWith(State.WaitingForOrder, NoData)
      |  initialize()
      |}""".stripMargin
    
    textarea.value = exampleCode
  }
  
  private def analyzeCode(code: String, outputTextarea: dom.HTMLTextAreaElement, diagramContainer: dom.HTMLDivElement, errorDiv: dom.HTMLDivElement): Unit = {
    if (code.trim.isEmpty) {
      showError(errorDiv, "Please provide Scala code to analyze")
      outputTextarea.value = ""
      diagramContainer.innerHTML = """<div class="placeholder-text">Enter Akka FSM code to see the diagram</div>"""
      return
    }
    
    AkkaFsmAnalyzer.parseScalaCode(code) match {
      case Right(mermaidCode) =>
        clearError(errorDiv)
        
        // Update state tracking
        lastGeneratedMermaid = mermaidCode
        isCodeManuallyEdited = false
        
        // Update UI
        outputTextarea.value = mermaidCode
        updateMermaidEditingState(outputTextarea, None, None)
        renderMermaidDiagram(mermaidCode, diagramContainer)
        
      case Left(error) =>
        showError(errorDiv, error)
        outputTextarea.value = ""
        diagramContainer.innerHTML = s"""<div class="error-text">Parse Error: $error</div>"""
        
        // Reset state
        lastGeneratedMermaid = ""
        isCodeManuallyEdited = false
        updateMermaidEditingState(outputTextarea, None, None)
    }
  }
  
  private def renderMermaidDiagram(mermaidCode: String, container: dom.HTMLDivElement): Unit = {
    try {
      // Validate container exists
      if (container == null) {
        println("Error: diagram container is null")
        return
      }
      
      // Clear previous content
      container.innerHTML = ""
      
      // Create a unique ID for this diagram
      val diagramId = s"mermaid-${System.currentTimeMillis()}-${Math.random().toString.replace(".", "")}"
      
      // Create div for mermaid
      val mermaidDiv = document.createElement("div")
      if (mermaidDiv == null) {
        println("Error: could not create mermaid div")
        container.innerHTML = """<div class="error-text">Failed to create diagram element</div>"""
        return
      }
      
      mermaidDiv.setAttribute("class", "mermaid")
      mermaidDiv.setAttribute("id", diagramId)
      mermaidDiv.textContent = mermaidCode
      container.appendChild(mermaidDiv)
      
      // Render with mermaid.js using async API
      val mermaid = dom.window.asInstanceOf[scala.scalajs.js.Dynamic].mermaid
      if (scala.scalajs.js.isUndefined(mermaid)) {
        container.innerHTML = """<div class="error-text">Mermaid.js not loaded</div>"""
        initializeMermaid() // Try to reinitialize
        return
      }
      
      // Add small delay to ensure DOM is ready
      dom.window.setTimeout(() => {
        try {
          // Re-find the mermaid div to ensure it's still valid
          val currentMermaidDiv = document.getElementById(diagramId)
          if (currentMermaidDiv == null) {
            println(s"Error: mermaid div with id $diagramId not found")
            container.innerHTML = """<div class="error-text">Failed to find diagram element for rendering</div>"""
            return
          }
          
          // Try modern API first, fallback to legacy
          if (!scala.scalajs.js.isUndefined(mermaid.run)) {
            // Modern async API (v10+)
            val promise = mermaid.run(scala.scalajs.js.Dynamic.literal(
              nodes = scala.scalajs.js.Array(currentMermaidDiv)
            ))
            
            if (!scala.scalajs.js.isUndefined(promise) && !scala.scalajs.js.isUndefined(promise.`catch`)) {
              promise.`catch`((error: scala.scalajs.js.Any) => {
                val errorMsg = if (error != null) {
                  try {
                    val errorDynamic = error.asInstanceOf[scala.scalajs.js.Dynamic]
                    if (!scala.scalajs.js.isUndefined(errorDynamic.message)) {
                      errorDynamic.message.toString
                    } else {
                      error.toString
                    }
                  } catch {
                    case _: Exception => error.toString
                  }
                } else {
                  "Unknown error"
                }
                println(s"Mermaid render error: $errorMsg")
                if (container != null) {
                  container.innerHTML = s"""<div class="error-text">Failed to render diagram: $errorMsg</div>"""
                }
              })
            }
          } else {
            // Legacy API (v9 and below)
            mermaid.init(scala.scalajs.js.undefined, currentMermaidDiv)
          }
        } catch {
          case ex: Exception =>
            println(s"Exception in delayed render: ${ex.getMessage}")
            container.innerHTML = s"""<div class="error-text">Render error: ${ex.getMessage}</div>"""
        }
      }, 50)
      
    } catch {
      case ex: Exception =>
        println(s"Exception in renderMermaidDiagram: ${ex.getMessage}")
        container.innerHTML = s"""<div class="error-text">Diagram render error: ${ex.getMessage}</div>"""
    }
  }
  
  private def showError(errorDiv: dom.HTMLDivElement, message: String): Unit = {
    errorDiv.textContent = message
    errorDiv.style.display = "block"
  }
  
  private def clearError(errorDiv: dom.HTMLDivElement): Unit = {
    errorDiv.textContent = ""
    errorDiv.style.display = "none"
  }
  
  private def initializeMermaid(retryCount: Int = 0): Unit = {
    try {
      val mermaid = dom.window.asInstanceOf[scala.scalajs.js.Dynamic].mermaid
      if (scala.scalajs.js.isUndefined(mermaid)) {
        if (retryCount < 10) {
          println(s"Mermaid not loaded yet, retrying... (attempt ${retryCount + 1})")
          dom.window.setTimeout(() => initializeMermaid(retryCount + 1), 200)
        } else {
          println("Mermaid failed to load after 10 attempts")
        }
        return
      }
      
      // Reset mermaid state
      if (!scala.scalajs.js.isUndefined(mermaid.mermaidAPI) && !scala.scalajs.js.isUndefined(mermaid.mermaidAPI.reset)) {
        mermaid.mermaidAPI.reset()
      }
      
      mermaid.initialize(scala.scalajs.js.Dynamic.literal(
        startOnLoad = false,
        theme = "default",
        securityLevel = "loose",
        fontFamily = "arial",
        deterministicIds = true,
        maxTextSize = 50000
      ))
      
      println("Mermaid initialized successfully")
    } catch {
      case ex: Exception =>
        println(s"Failed to initialize Mermaid: ${ex.getMessage}")
        if (retryCount < 5) {
          dom.window.setTimeout(() => initializeMermaid(retryCount + 1), 500)
        }
    }
  }
  
  private def exportDiagramAsSvg(container: dom.HTMLDivElement): Unit = {
    try {
      val svgElement = container.querySelector("svg")
      if (svgElement != null) {
        val svgData = new dom.XMLSerializer().serializeToString(svgElement)
        val svgBlob = new dom.Blob(
          scala.scalajs.js.Array(svgData), 
          new dom.BlobPropertyBag {
            `type` = "image/svg+xml;charset=utf-8"
          }
        )
        val url = dom.URL.createObjectURL(svgBlob)
        val link = document.createElement("a").asInstanceOf[dom.HTMLAnchorElement]
        link.href = url
        link.download = "fsm-diagram.svg"
        link.click()
        dom.URL.revokeObjectURL(url)
      } else {
        println("No SVG diagram found to export")
      }
    } catch {
      case ex: Exception =>
        println(s"Failed to export SVG: ${ex.getMessage}")
    }
  }
  
  private def exportDiagramAsPng(container: dom.HTMLDivElement): Unit = {
    try {
      val svgElement = container.querySelector("svg")
      if (svgElement != null) {
        // Show loading message for large diagrams
        val containerRect = container.getBoundingClientRect()
        val diagramArea = containerRect.width * containerRect.height
        if (diagramArea > 200000) {
          println("Exporting large diagram - this may take a few moments...")
        }
        
        // Try to use html2canvas library if available, otherwise fall back to simpler method
        val html2canvas = dom.window.asInstanceOf[scala.scalajs.js.Dynamic].html2canvas
        
        if (!scala.scalajs.js.isUndefined(html2canvas)) {
          // Calculate adaptive scale - targeting ~3.5MB file size
          val adaptiveScale = if (diagramArea > 500000) 6.0 else if (diagramArea > 200000) 5.0 else 4.0
          
          println(s"PNG Export: Diagram size ${containerRect.width}x${containerRect.height}, using scale $adaptiveScale")
          
          // Use html2canvas if available - extreme high quality settings
          html2canvas(container, scala.scalajs.js.Dynamic.literal(
            backgroundColor = "white",
            scale = adaptiveScale, // Doubled adaptive scaling for extreme quality
            useCORS = true,
            allowTaint = false,
            logging = false,
            width = container.scrollWidth,
            height = container.scrollHeight,
            windowWidth = container.scrollWidth,
            windowHeight = container.scrollHeight,
            removeContainer = false,
            imageTimeout = 60000, // Reasonable timeout for balanced quality
            foreignObjectRendering = false, // Better text rendering
            letterRendering = true, // Enhanced letter rendering
            onclone = scala.scalajs.js.defined { (clonedDoc: org.scalajs.dom.Document) =>
              // Enhance text rendering in cloned document for extreme quality
              val style = clonedDoc.createElement("style")
              style.textContent = """
                text { 
                  text-rendering: optimizeLegibility !important; 
                  font-smooth: always !important;
                  -webkit-font-smoothing: antialiased !important;
                }
              """
              val head = clonedDoc.querySelector("head")
              if (head != null) {
                head.appendChild(style)
              }
            }
          )).`then`((canvas: dom.HTMLCanvasElement) => {
            // Use high quality with minimal compression for ~3.5MB target
            val dataUrl = canvas.toDataURL("image/png", 0.98) // 98% quality for good balance
            val link = document.createElement("a").asInstanceOf[dom.HTMLAnchorElement]
            link.href = dataUrl
            val scaleText = if (adaptiveScale >= 6.0) "premium-hq" else if (adaptiveScale >= 5.0) "balanced-hq" else "hq"
            link.download = s"fsm-diagram-${scaleText}.png"
            link.click()
            println(s"PNG Export completed: ${canvas.width}x${canvas.height} pixels")
          })
        } else {
          // Fallback: create ultra high-resolution PNG from SVG
          val canvas = document.createElement("canvas").asInstanceOf[dom.HTMLCanvasElement]
          val ctx = canvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
          
          // Get SVG dimensions and calculate adaptive scale
          val svgRect = svgElement.getBoundingClientRect()
          val diagramArea = svgRect.width * svgRect.height
          val adaptiveScale = if (diagramArea > 500000) 7.0 else if (diagramArea > 200000) 6.0 else 5.0 // Targeting ~3.5MB for fallback
          
          println(s"PNG Fallback Export: Diagram size ${svgRect.width}x${svgRect.height}, using scale $adaptiveScale")
          
          val scaledWidth = (svgRect.width * adaptiveScale).toInt
          val scaledHeight = (svgRect.height * adaptiveScale).toInt
          
          // Set canvas to ultra-high resolution
          canvas.width = scaledWidth
          canvas.height = scaledHeight
          
          // Scale the context to match the increased canvas size
          ctx.scale(adaptiveScale, adaptiveScale)
          
          // Clean SVG data and set high resolution dimensions
          var svgData = new dom.XMLSerializer().serializeToString(svgElement)
          svgData = svgData.replaceAll("xmlns=\"http://www.w3.org/2000/svg\"", "")
          // Use original dimensions in SVG, scaling is handled by canvas context
          svgData = s"""<svg xmlns="http://www.w3.org/2000/svg" width="${svgRect.width}" height="${svgRect.height}">$svgData</svg>"""
          
          val svgBlob = new dom.Blob(
            scala.scalajs.js.Array(svgData), 
            new dom.BlobPropertyBag {
              `type` = "image/svg+xml;charset=utf-8"
            }
          )
          // Create data URL from SVG directly
          val dataUrl = "data:image/svg+xml;charset=utf-8," + 
            scala.scalajs.js.URIUtils.encodeURIComponent(svgData)
          
          val img = document.createElement("img").asInstanceOf[dom.HTMLImageElement]
          
          img.onload = { (_: dom.Event) =>
            try {
              // Enable high-quality rendering
              ctx.imageSmoothingEnabled = true
              val ctxDynamic = ctx.asInstanceOf[scala.scalajs.js.Dynamic]
              if (!scala.scalajs.js.isUndefined(ctxDynamic.imageSmoothingQuality)) {
                ctxDynamic.imageSmoothingQuality = "high"
              }
              
              // Fill white background at scaled dimensions
              ctx.fillStyle = "white"
              ctx.fillRect(0, 0, svgRect.width, svgRect.height)
              
              // Draw the SVG image (scaling is already applied by context)
              ctx.drawImage(img, 0, 0)
              
              val pngDataUrl = canvas.toDataURL("image/png", 0.98) // High quality targeting ~3.5MB
              val link = document.createElement("a").asInstanceOf[dom.HTMLAnchorElement]
              link.href = pngDataUrl
              val scaleText = if (adaptiveScale >= 7.0) "premium-hq" else if (adaptiveScale >= 6.0) "balanced-hq" else "hq"
              link.download = s"fsm-diagram-fallback-${scaleText}.png"
              link.click()
              println(s"PNG Fallback Export completed: ${canvas.width}x${canvas.height} pixels")
            } catch {
              case ex: Exception =>
                println(s"Canvas export failed: ${ex.getMessage}")
                // Ultimate fallback: just download the SVG
                exportDiagramAsSvg(container)
            }
          }
          
          // Set error handler using JavaScript dynamic access
          val imgDynamic = img.asInstanceOf[scala.scalajs.js.Dynamic]
          imgDynamic.onerror = { (_: dom.Event) =>
            println("Failed to load SVG image, falling back to SVG export")
            exportDiagramAsSvg(container)
          }
          
          img.src = dataUrl
        }
      } else {
        println("No SVG diagram found to export")
      }
    } catch {
      case ex: Exception =>
        println(s"Failed to export PNG: ${ex.getMessage}")
        // Fallback to SVG export
        exportDiagramAsSvg(container)
    }
  }
  
  private def showFullscreen(container: dom.HTMLDivElement, modal: dom.HTMLDivElement, fullscreenContainer: dom.HTMLDivElement, zoomContainer: dom.HTMLDivElement, zoomLevelSpan: dom.HTMLSpanElement): Unit = {
    try {
      // Clone the diagram content
      val diagramContent = container.cloneNode(true).asInstanceOf[dom.HTMLDivElement]
      zoomContainer.innerHTML = ""
      zoomContainer.appendChild(diagramContent)
      
      // Reset zoom state
      resetZoom(zoomContainer, zoomLevelSpan)
      
      // Setup pan/drag functionality
      setupPanAndDrag(zoomContainer)
      
      // Show the modal
      modal.style.display = "block"
      
      // Prevent body scrolling
      document.body.style.overflow = "hidden"
    } catch {
      case ex: Exception =>
        println(s"Failed to show fullscreen: ${ex.getMessage}")
    }
  }
  
  private def hideFullscreen(modal: dom.HTMLDivElement): Unit = {
    try {
      modal.style.display = "none"
      document.body.style.overflow = ""
    } catch {
      case ex: Exception =>
        println(s"Failed to hide fullscreen: ${ex.getMessage}")
    }
  }
  
  // Zoom functionality
  private def updateTransform(container: dom.HTMLDivElement): Unit = {
    val transform = s"scale($currentZoom) translate(${currentPanX}px, ${currentPanY}px)"
    container.style.transform = transform
  }
  
  private def updateZoomLevel(zoomLevelSpan: dom.HTMLSpanElement): Unit = {
    val percentage = Math.round(currentZoom * 100).toInt
    zoomLevelSpan.textContent = s"${percentage}%"
  }
  
  private def zoomIn(container: dom.HTMLDivElement, zoomLevelSpan: dom.HTMLSpanElement): Unit = {
    currentZoom = Math.min(currentZoom * 1.25, 5.0) // Max 500% zoom
    updateTransform(container)
    updateZoomLevel(zoomLevelSpan)
  }
  
  private def zoomOut(container: dom.HTMLDivElement, zoomLevelSpan: dom.HTMLSpanElement): Unit = {
    currentZoom = Math.max(currentZoom / 1.25, 0.1) // Min 10% zoom
    updateTransform(container)
    updateZoomLevel(zoomLevelSpan)
  }
  
  private def resetZoom(container: dom.HTMLDivElement, zoomLevelSpan: dom.HTMLSpanElement): Unit = {
    currentZoom = 1.0
    currentPanX = 0.0
    currentPanY = 0.0
    updateTransform(container)
    updateZoomLevel(zoomLevelSpan)
  }
  
  private def fitToScreen(container: dom.HTMLDivElement, zoomLevelSpan: dom.HTMLSpanElement): Unit = {
    try {
      // Get the SVG element
      val svg = container.querySelector("svg")
      if (svg != null) {
        val svgEl = svg.asInstanceOf[dom.SVGSVGElement]
        val containerEl = container.parentElement
        
        if (containerEl != null) {
          val containerRect = containerEl.getBoundingClientRect()
          val svgRect = svgEl.getBoundingClientRect()
          
          if (svgRect.width > 0 && svgRect.height > 0) {
            val scaleX = (containerRect.width * 0.9) / svgRect.width
            val scaleY = (containerRect.height * 0.9) / svgRect.height
            currentZoom = Math.min(scaleX, scaleY)
            currentPanX = 0.0
            currentPanY = 0.0
            
            updateTransform(container)
            updateZoomLevel(zoomLevelSpan)
          }
        }
      }
    } catch {
      case ex: Exception =>
        println(s"Failed to fit to screen: ${ex.getMessage}")
        // Fallback to reset zoom
        resetZoom(container, zoomLevelSpan)
    }
  }
  
  private def setupPanAndDrag(container: dom.HTMLDivElement): Unit = {
    // Mouse events
    container.addEventListener("mousedown", { (e: dom.MouseEvent) =>
      isDragging = true
      dragStartX = e.clientX - currentPanX
      dragStartY = e.clientY - currentPanY
      container.classList.add("dragging")
    })
    
    container.addEventListener("mousemove", { (e: dom.MouseEvent) =>
      if (isDragging) {
        currentPanX = e.clientX - dragStartX
        currentPanY = e.clientY - dragStartY
        updateTransform(container)
      }
    })
    
    container.addEventListener("mouseup", { (_: dom.MouseEvent) =>
      isDragging = false
      container.classList.remove("dragging")
    })
    
    container.addEventListener("mouseleave", { (_: dom.MouseEvent) =>
      isDragging = false
      container.classList.remove("dragging")
    })
    
    // Mouse wheel zoom with trackpad sensitivity handling
    container.addEventListener("wheel", { (e: dom.WheelEvent) =>
      e.preventDefault()
      val zoomLevelSpan = document.getElementById("zoomLevel").asInstanceOf[dom.HTMLSpanElement]
      
      // Calculate zoom factor based on wheel delta for smooth trackpad experience
      // Normalize deltaY to reasonable range and apply exponential scaling
      val deltaY = Math.max(-100, Math.min(100, e.deltaY)) // Clamp deltaY
      val zoomFactor = Math.pow(1.002, -deltaY) // Smoother scaling factor
      
      val newZoom = Math.max(0.1, Math.min(5.0, currentZoom * zoomFactor))
      
      if (newZoom != currentZoom) {
        currentZoom = newZoom
        updateTransform(container)
        updateZoomLevel(zoomLevelSpan)
      }
    })
  }
  
  // Mermaid editing functionality
  private def updateMermaidEditingState(textarea: dom.HTMLTextAreaElement, regenerateButton: Option[dom.HTMLButtonElement], indicator: Option[dom.HTMLSpanElement]): Unit = {
    if (isCodeManuallyEdited) {
      textarea.classList.add("modified")
      regenerateButton.foreach(_.classList.add("show"))
      indicator.foreach(_.classList.add("show"))
    } else {
      textarea.classList.remove("modified")
      regenerateButton.foreach(_.classList.remove("show"))
      indicator.foreach(_.classList.remove("show"))
    }
  }
  
  private def onMermaidCodeEdit(textarea: dom.HTMLTextAreaElement, diagramContainer: dom.HTMLDivElement, errorDiv: dom.HTMLDivElement, regenerateButton: dom.HTMLButtonElement, indicator: dom.HTMLSpanElement): Unit = {
    val currentCode = textarea.value
    
    // Check if code has been manually modified
    if (currentCode != lastGeneratedMermaid && lastGeneratedMermaid.nonEmpty) {
      isCodeManuallyEdited = true
      updateMermaidEditingState(textarea, Some(regenerateButton), Some(indicator))
    }
    
    // Clear any existing timer
    mermaidEditTimer.foreach(timer => dom.window.clearTimeout(timer))
    
    // Set a new timer to debounce the rendering
    val timerId = dom.window.setTimeout(() => {
      if (currentCode.trim.nonEmpty) {
        try {
          renderMermaidDiagram(currentCode, diagramContainer)
          clearError(errorDiv)
        } catch {
          case ex: Exception =>
            showError(errorDiv, s"Mermaid rendering error: ${ex.getMessage}")
        }
      } else {
        diagramContainer.innerHTML = """<div class="placeholder-text">Enter Mermaid code to see the diagram</div>"""
        clearError(errorDiv)
      }
    }, 500) // 500ms debounce for manual edits
    
    mermaidEditTimer = Some(timerId)
  }
  
  private def regenerateMermaidCode(scalaCode: String, outputTextarea: dom.HTMLTextAreaElement, diagramContainer: dom.HTMLDivElement, errorDiv: dom.HTMLDivElement, regenerateButton: dom.HTMLButtonElement, indicator: dom.HTMLSpanElement): Unit = {
    // Clear modified state first
    isCodeManuallyEdited = false
    updateMermaidEditingState(outputTextarea, Some(regenerateButton), Some(indicator))
    
    // Re-analyze the Scala code
    analyzeCode(scalaCode, outputTextarea, diagramContainer, errorDiv)
  }
}