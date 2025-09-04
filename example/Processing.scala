import akka.actor.{Actor, ActorRef, FSM, Props}
import scala.concurrent.duration._

// События
sealed trait ProcessingEvent
case object StartProcessing extends ProcessingEvent
case object ProcessComplete extends ProcessingEvent
case object ProcessFailed extends ProcessingEvent
case object Retry extends ProcessingEvent
case object Reset extends ProcessingEvent
case object Stop extends ProcessingEvent
case object ProcessStopped extends ProcessingEvent
case class ErrorOccurred(reason: String) extends ProcessingEvent

// Состояния
sealed trait ProcessingState
object State {
  case object Idle extends ProcessingState
  case object Processing extends ProcessingState
  case object Complete extends ProcessingState
  case object Failed extends ProcessingState
  case object Stopping extends ProcessingState
  case object RecoverSelf extends ProcessingState
}

// Данные состояния
sealed trait ProcessingData
case object EmptyData extends ProcessingData
case class ProcessingInfo(startTime: Long, retryCount: Int = 0) extends ProcessingData
case class ErrorData(reason: String, timestamp: Long = System.currentTimeMillis()) extends ProcessingData
case object RecoveryData extends ProcessingData
case object InitialData extends ProcessingData

class ProcessingFSM extends Actor with FSM[ProcessingState, ProcessingData] {

  when(State.Idle) {
    case Event(StartProcessing, _) =>
      println("Starting processing...")
      goto(State.Processing) using ProcessingInfo(System.currentTimeMillis())
      
    case Event(Stop, _) =>
      println("Stopping from idle state")
      stopSuccess()
  }

  when(State.Processing) {
    case Event(ProcessComplete, ProcessingInfo(startTime, _)) =>
      val duration = System.currentTimeMillis() - startTime
      println(s"Processing completed in ${duration}ms")
      goto(State.Complete) using EmptyData
      
    case Event(ProcessFailed, ProcessingInfo(_, retryCount)) =>
      println(s"Processing failed (retry count: $retryCount)")
      goto(State.Failed) using ErrorData(s"Process failed after $retryCount retries")
      
    case Event(Stop, _) =>
      println("Stopping processing...")
      goto(State.Stopping) using EmptyData
      
    case Event(ErrorOccurred(reason), data) =>
      println(s"Error occurred during processing: $reason")
      goto(State.Failed) using ErrorData(reason)
  }

  when(State.Complete) {
    case Event(Reset, _) =>
      println("Resetting to idle state")
      goto(State.Idle) using EmptyData
      
    case Event(Stop, _) =>
      println("Stopping from complete state")
      stopSuccess()
  }

  when(State.Failed) {
    case Event(Retry, ErrorData(reason, _)) =>
      println(s"Retrying after failure: $reason")
      goto(State.Processing) using ProcessingInfo(System.currentTimeMillis(), 1)
      
    case Event(Reset, _) =>
      println("Resetting from failed state")
      goto(State.Idle) using EmptyData
      
    case Event(Stop, _) =>
      println("Stopping from failed state")
      stopSuccess()
  }

  when(State.Stopping) {
    case Event(ProcessStopped, _) =>
      println("Process stopped successfully")
      stopSuccess()
      
    case Event(Stop, _) =>
      println("Already stopping...")
      stay()
  }

  // Метод восстановления состояния
  def recoverStateDecision(reason: String): State = {
    reason match {
      case "network_error" => 
        println("Recovering from network error - going to idle")
        Target.enter(State.Idle, InitialData)
      case "critical_error" => 
        println("Recovering from critical error - going to failed")
        Target.enter(State.Failed, ErrorData(reason))
      case "timeout" =>
        println("Recovering from timeout - retrying processing")
        Target.enter(State.Processing, RecoveryData)
      case _ => 
        println("Unknown error - going to processing with recovery data")
        Target.enter(State.Processing, RecoveryData)
    }
  }

  // Инициализация FSM
  startWith(State.Idle, EmptyData)

  // Обработка переходов
  onTransition {
    case State.Idle -> State.Processing => log.info("Started processing")
    case State.Processing -> State.Complete => log.info("Processing completed successfully")
    case State.Processing -> State.Failed => log.info("Processing failed")
    case State.Failed -> State.Processing => log.info("Retrying processing")
    case _ -> State.Stopping => log.info("Stopping FSM")
  }

  // Таймауты
  when(State.Processing, stateTimeout = 30.seconds) {
    case Event(StateTimeout, _) =>
      println("Processing timeout!")
      goto(State.Failed) using ErrorData("Processing timeout")
  }

  // Обработка неожиданных событий
  whenUnhandled {
    case Event(e, s) =>
      log.warning("Received unhandled request {} in state {}/{}", e, stateName, s)
      stay()
  }

  initialize()
}

// Вспомогательный объект Target для восстановления
object Target {
  def enter(state: ProcessingState, data: ProcessingData): State = {
    // Логика входа в состояние с данными
    state match {
      case State.Idle => State.Idle
      case State.Processing => State.Processing  
      case State.Complete => State.Complete
      case State.Failed => State.Failed
      case State.Stopping => State.Stopping
      case State.RecoverSelf => State.RecoverSelf
    }
  }
}

// Companion object для создания актора
object ProcessingFSM {
  def props(): Props = Props(new ProcessingFSM())
}

// Пример использования
object ProcessingFSMApp extends App {
  import akka.actor.ActorSystem

  val system = ActorSystem("ProcessingSystem")
  val processingActor = system.actorOf(ProcessingFSM.props(), "processing-fsm")

  // Пример последовательности событий
  import system.dispatcher
  import scala.concurrent.duration._

  system.scheduler.scheduleOnce(1.second) {
    processingActor ! StartProcessing
  }

  system.scheduler.scheduleOnce(3.seconds) {
    processingActor ! ProcessComplete
  }

  system.scheduler.scheduleOnce(5.seconds) {
    processingActor ! Reset
  }

  system.scheduler.scheduleOnce(7.seconds) {
    processingActor ! Stop
  }

  system.scheduler.scheduleOnce(10.seconds) {
    system.terminate()
  }
}