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
    val fileInput = document.getElementById("fileInput").asInstanceOf[dom.HTMLInputElement]
    val codeTextarea = document.getElementById("codeInput").asInstanceOf[dom.HTMLTextAreaElement]
    val copyButton = document.getElementById("copyButton").asInstanceOf[dom.HTMLButtonElement]
    val toggleButton = document.getElementById("toggleSource").asInstanceOf[dom.HTMLButtonElement]
    val errorDiv = document.getElementById("error").asInstanceOf[dom.HTMLDivElement]
    val mermaidOutput = document.getElementById("mermaidOutput").asInstanceOf[dom.HTMLTextAreaElement]
    val diagramContainer = document.getElementById("diagramContainer").asInstanceOf[dom.HTMLDivElement]
    
    // Load example code
    loadExampleCode(codeTextarea)
    
    // File upload handler
    fileInput.addEventListener("change", { (_: dom.Event) =>
      val files = fileInput.files
      if (files.length > 0) {
        val file = files(0)
        val reader = new dom.FileReader()
        reader.onload = { (_: dom.Event) =>
          codeTextarea.value = reader.result.asInstanceOf[String]
          clearError(errorDiv)
          analyzeCode(codeTextarea.value, mermaidOutput, diagramContainer, errorDiv)
        }
        reader.readAsText(file)
      }
    })
    
    // Auto-analyze on code change
    codeTextarea.addEventListener("input", { (_: dom.Event) =>
      val code = codeTextarea.value
      if (code.trim.nonEmpty) {
        analyzeCode(code, mermaidOutput, diagramContainer, errorDiv)
      } else {
        mermaidOutput.value = ""
        diagramContainer.innerHTML = """<div class="placeholder-text">Enter Akka FSM code to see the diagram</div>"""
        clearError(errorDiv)
      }
    })
    
    // Toggle source view
    toggleButton.addEventListener("click", { (_: dom.Event) =>
      val isSourceVisible = mermaidOutput.style.display != "none"
      if (isSourceVisible) {
        mermaidOutput.style.display = "none"
        diagramContainer.style.display = "block"
        toggleButton.textContent = "Show Code"
      } else {
        mermaidOutput.style.display = "block"
        diagramContainer.style.display = "none"
        toggleButton.textContent = "Show Diagram"
      }
    })
    
    // Copy button handler
    copyButton.addEventListener("click", { (_: dom.Event) =>
      mermaidOutput.select()
      dom.document.execCommand("copy")
      
      // Show feedback
      val originalText = copyButton.textContent
      copyButton.textContent = "✓ Copied!"
      dom.window.setTimeout(() => {
        copyButton.textContent = originalText
      }, 2000)
    })
    
    // Initialize mermaid
    dom.window.asInstanceOf[scala.scalajs.js.Dynamic].mermaid.initialize(scala.scalajs.js.Dynamic.literal(
      startOnLoad = true,
      theme = "default",
      securityLevel = "loose"
    ))
    
    // Initial analysis with example code
    analyzeCode(codeTextarea.value, mermaidOutput, diagramContainer, errorDiv)
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
      val diagramId = s"mermaid-${System.currentTimeMillis()}"
      
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
        return
      }
      
      // Try modern API first, fallback to legacy
      if (!scala.scalajs.js.isUndefined(mermaid.run)) {
        // Modern async API (v10+)
        mermaid.run(scala.scalajs.js.Dynamic.literal(
          nodes = scala.scalajs.js.Array(mermaidDiv)
        )).`catch`((error: scala.scalajs.js.Any) => {
          println(s"Mermaid render error: $error")
          container.innerHTML = s"""<div class="error-text">Failed to render diagram: $error</div>"""
        })
      } else {
        // Legacy API (v9 and below)
        mermaid.init(scala.scalajs.js.undefined, mermaidDiv)
      }
      
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
}