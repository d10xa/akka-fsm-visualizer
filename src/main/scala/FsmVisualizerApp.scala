import org.scalajs.dom
import org.scalajs.dom.document
import scala.scalajs.js.annotation.JSExportTopLevel

@JSExportTopLevel("FsmVisualizerApp")
object FsmVisualizerApp {
  
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
    val errorDiv = Option(document.getElementById("error")).map(_.asInstanceOf[dom.HTMLDivElement])
    val mermaidOutput = Option(document.getElementById("mermaidOutput")).map(_.asInstanceOf[dom.HTMLTextAreaElement])
    val diagramContainer = Option(document.getElementById("diagramContainer")).map(_.asInstanceOf[dom.HTMLDivElement])
    
    // Check if all required elements are present
    if (fileInput.isEmpty || codeTextarea.isEmpty || copyButton.isEmpty || toggleButton.isEmpty || 
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
    
    // Auto-analyze on code change
    codeTextareaEl.addEventListener("input", { (_: dom.Event) =>
      val code = codeTextareaEl.value
      if (code.trim.nonEmpty) {
        analyzeCode(code, mermaidOutputEl, diagramContainerEl, errorDivEl)
      } else {
        mermaidOutputEl.value = ""
        diagramContainerEl.innerHTML = """<div class="placeholder-text">Enter Akka FSM code to see the diagram</div>"""
        clearError(errorDivEl)
      }
    })
    
    // Toggle source view
    toggleButtonEl.addEventListener("click", { (_: dom.Event) =>
      val isSourceVisible = mermaidOutputEl.style.display != "none"
      if (isSourceVisible) {
        mermaidOutputEl.style.display = "none"
        diagramContainerEl.style.display = "block"
        toggleButtonEl.textContent = "Show Code"
      } else {
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
        outputTextarea.value = mermaidCode
        renderMermaidDiagram(mermaidCode, diagramContainer)
        
      case Left(error) =>
        showError(errorDiv, error)
        outputTextarea.value = ""
        diagramContainer.innerHTML = s"""<div class="error-text">Parse Error: $error</div>"""
    }
  }
  
  private def renderMermaidDiagram(mermaidCode: String, container: dom.HTMLDivElement): Unit = {
    try {
      // Clear previous content
      container.innerHTML = ""
      
      // Create a unique ID for this diagram
      val diagramId = s"mermaid-${System.currentTimeMillis()}-${Math.random().toString.replace(".", "")}"
      
      // Create div for mermaid
      val mermaidDiv = document.createElement("div")
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
          // Try modern API first, fallback to legacy
          if (!scala.scalajs.js.isUndefined(mermaid.run)) {
            // Modern async API (v10+)
            val promise = mermaid.run(scala.scalajs.js.Dynamic.literal(
              nodes = scala.scalajs.js.Array(mermaidDiv)
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
                container.innerHTML = s"""<div class="error-text">Failed to render diagram: $errorMsg</div>"""
              })
            }
          } else {
            // Legacy API (v9 and below)
            mermaid.init(scala.scalajs.js.undefined, mermaidDiv)
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
}