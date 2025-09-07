import akka.actor.{FSM}

sealed trait TestState
object States {
  case object Idle extends TestState
  case object Processing extends TestState  
  case object Complete extends TestState
  case object Failed extends TestState
}

sealed trait TestData
case object EmptyData extends TestData
case class ProcessingData(flag: Boolean) extends TestData

sealed trait TestEvent
case object Start extends TestEvent
case object Finish extends TestEvent
case object Reset extends TestEvent

class VariableGotoTestFSM extends FSM[TestState, TestData] {
  
  when(States.Idle) {
    case Event(Start, data: ProcessingData) =>
      val to = if (data.flag) States.Processing else States.Failed
      goto(to) replying AckMessage
      
    case Event(Reset, _) =>
      goto(States.Idle)
  }

  when(States.Processing) {
    case Event(Finish, data: ProcessingData) =>
      val targetState = if (data.flag) States.Complete else States.Failed
      goto(targetState)
      
    case Event(Reset, _) =>
      val destination = States.Idle
      goto(destination)
  }

  when(States.Complete) {
    case Event(Reset, _) =>
      goto(States.Idle)
  }

  when(States.Failed) {
    case Event(Reset, _) =>
      goto(States.Idle)
  }
  
  startWith(States.Idle, EmptyData)
}