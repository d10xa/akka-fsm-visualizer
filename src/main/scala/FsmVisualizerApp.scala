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
    val analyzeButton = document.getElementById("analyzeButton").asInstanceOf[dom.HTMLButtonElement]
    val clearButton = document.getElementById("clearButton").asInstanceOf[dom.HTMLButtonElement]
    val copyButton = document.getElementById("copyButton").asInstanceOf[dom.HTMLButtonElement]
    val errorDiv = document.getElementById("error").asInstanceOf[dom.HTMLDivElement]
    val mermaidOutput = document.getElementById("mermaidOutput").asInstanceOf[dom.HTMLTextAreaElement]
    
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
        }
        reader.readAsText(file)
      }
    })
    
    // Analyze button handler
    analyzeButton.addEventListener("click", { (_: dom.Event) =>
      analyzeCode(codeTextarea.value, mermaidOutput, errorDiv)
    })
    
    // Clear button handler
    clearButton.addEventListener("click", { (_: dom.Event) =>
      codeTextarea.value = ""
      mermaidOutput.value = ""
      clearError(errorDiv)
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
    
    // Initial analysis with example code
    analyzeCode(codeTextarea.value, mermaidOutput, errorDiv)
  }
  
  private def loadExampleCode(textarea: dom.HTMLTextAreaElement): Unit = {
    val exampleCode = """import akka.actor.{Actor, ActorRef, FSM, Props}
      |import scala.concurrent.duration._
      |
      |// События
      |sealed trait ProcessingEvent
      |case object StartProcessing extends ProcessingEvent
      |case object ProcessComplete extends ProcessingEvent
      |case object ProcessFailed extends ProcessingEvent
      |case object Retry extends ProcessingEvent
      |case object Reset extends ProcessingEvent
      |case object Stop extends ProcessingEvent
      |case object ProcessStopped extends ProcessingEvent
      |
      |// Состояния
      |sealed trait ProcessingState
      |object State {
      |  case object Idle extends ProcessingState
      |  case object Processing extends ProcessingState
      |  case object Complete extends ProcessingState
      |  case object Failed extends ProcessingState
      |  case object Stopping extends ProcessingState
      |  case object RecoverSelf extends ProcessingState
      |}
      |
      |// Данные состояния
      |sealed trait ProcessingData
      |case object EmptyData extends ProcessingData
      |case class ProcessingInfo(startTime: Long) extends ProcessingData
      |case class ErrorData(reason: String) extends ProcessingData
      |case object RecoveryData extends ProcessingData
      |case object InitialData extends ProcessingData
      |
      |class ProcessingFSM extends Actor with FSM[ProcessingState, ProcessingData] {
      |
      |  when(State.Idle) {
      |    case Event(StartProcessing, _) =>
      |      goto(State.Processing)
      |    case Event(Stop, _) =>
      |      stopSuccess()
      |  }
      |
      |  when(State.Processing) {
      |    case Event(ProcessComplete, _) =>
      |      goto(State.Complete)
      |    case Event(ProcessFailed, _) =>
      |      goto(State.Failed)
      |    case Event(Stop, _) =>
      |      goto(State.Stopping)
      |  }
      |
      |  when(State.Complete) {
      |    case Event(Reset, _) =>
      |      goto(State.Idle)
      |  }
      |
      |  when(State.Failed) {
      |    case Event(Retry, _) =>
      |      goto(State.Processing)
      |    case Event(Reset, _) =>
      |      goto(State.Idle)
      |  }
      |
      |  when(State.Stopping) {
      |    case Event(ProcessStopped, _) =>
      |      stopSuccess()
      |  }
      |
      |  def recoverStateDecision(reason: String): State = {
      |    reason match {
      |      case "network_error" => Target.enter(State.Idle, InitialData)
      |      case "critical_error" => Target.enter(State.Failed, ErrorData(reason))
      |      case _ => Target.enter(State.Processing, RecoveryData)
      |    }
      |  }
      |
      |  startWith(State.Idle, EmptyData)
      |  initialize()
      |}
      |
      |object Target {
      |  def enter(state: ProcessingState, data: ProcessingData): State = state
      |}""".stripMargin
    
    textarea.value = exampleCode
  }
  
  private def analyzeCode(code: String, outputTextarea: dom.HTMLTextAreaElement, errorDiv: dom.HTMLDivElement): Unit = {
    if (code.trim.isEmpty) {
      showError(errorDiv, "Please provide Scala code to analyze")
      outputTextarea.value = ""
      return
    }
    
    AkkaFsmAnalyzer.parseScalaCode(code) match {
      case Right(mermaidCode) =>
        clearError(errorDiv)
        outputTextarea.value = mermaidCode
        
      case Left(error) =>
        showError(errorDiv, error)
        outputTextarea.value = ""
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