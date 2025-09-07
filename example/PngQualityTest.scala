import akka.actor.{FSM}

sealed trait QualityTestState
object TestStates {
  case object InitialState extends QualityTestState
  case object FirstProcessing extends QualityTestState  
  case object SecondProcessing extends QualityTestState
  case object ValidationState extends QualityTestState
  case object CompletionState extends QualityTestState
  case object ErrorHandlingState extends QualityTestState
  case object RecoveryState extends QualityTestState
  case object FinalState extends QualityTestState
}

sealed trait QualityTestData
case object EmptyTestData extends QualityTestData
case class ProcessingTestData(id: String, counter: Int) extends QualityTestData

sealed trait QualityTestEvent
case object StartTest extends QualityTestEvent
case object ProcessFirst extends QualityTestEvent
case object ProcessSecond extends QualityTestEvent
case object ValidateResults extends QualityTestEvent
case object CompleteTest extends QualityTestEvent
case object HandleError extends QualityTestEvent
case object RecoverFromError extends QualityTestEvent
case object FinalizeTest extends QualityTestEvent
case object ResetTest extends QualityTestEvent

class PngQualityTestFSM extends FSM[QualityTestState, QualityTestData] {
  
  when(TestStates.InitialState) {
    case Event(StartTest, _) =>
      goto(TestStates.FirstProcessing) using ProcessingTestData("test-1", 1)
      
    case Event(ResetTest, _) =>
      stay()
  }

  when(TestStates.FirstProcessing) {
    case Event(ProcessFirst, data: ProcessingTestData) =>
      goto(TestStates.SecondProcessing) using data.copy(counter = data.counter + 1)
      
    case Event(HandleError, _) =>
      goto(TestStates.ErrorHandlingState)
      
    case Event(ResetTest, _) =>
      goto(TestStates.InitialState)
  }

  when(TestStates.SecondProcessing) {
    case Event(ProcessSecond, data: ProcessingTestData) =>
      goto(TestStates.ValidationState) using data.copy(counter = data.counter + 1)
      
    case Event(HandleError, _) =>
      goto(TestStates.ErrorHandlingState)
      
    case Event(ResetTest, _) =>
      goto(TestStates.InitialState)
  }

  when(TestStates.ValidationState) {
    case Event(ValidateResults, data: ProcessingTestData) =>
      if (data.counter > 2) {
        goto(TestStates.CompletionState)
      } else {
        goto(TestStates.ErrorHandlingState)
      }
      
    case Event(HandleError, _) =>
      goto(TestStates.ErrorHandlingState)
      
    case Event(ResetTest, _) =>
      goto(TestStates.InitialState)
  }

  when(TestStates.CompletionState) {
    case Event(CompleteTest, _) =>
      goto(TestStates.FinalState)
      
    case Event(ResetTest, _) =>
      goto(TestStates.InitialState)
  }

  when(TestStates.ErrorHandlingState) {
    case Event(RecoverFromError, _) =>
      goto(TestStates.RecoveryState)
      
    case Event(ResetTest, _) =>
      goto(TestStates.InitialState)
  }

  when(TestStates.RecoveryState) {
    case Event(ProcessFirst, _) =>
      goto(TestStates.FirstProcessing) using ProcessingTestData("recovery-1", 0)
      
    case Event(ResetTest, _) =>
      goto(TestStates.InitialState)
  }

  when(TestStates.FinalState) {
    case Event(FinalizeTest, _) =>
      stopSuccess()
      
    case Event(ResetTest, _) =>
      goto(TestStates.InitialState)
  }
  
  // Add timeout for processing states
  when(TestStates.FirstProcessing, stateTimeout = 30.seconds) {
    case Event(StateTimeout, _) =>
      goto(TestStates.ErrorHandlingState) using ProcessingTestData("timeout", -1)
  }
  
  when(TestStates.SecondProcessing, stateTimeout = 45.seconds) {
    case Event(StateTimeout, _) =>
      goto(TestStates.ErrorHandlingState) using ProcessingTestData("timeout", -1)
  }
  
  onTransition {
    case TestStates.InitialState -> TestStates.FirstProcessing => 
      log.info("Starting test processing")
    case TestStates.ValidationState -> TestStates.CompletionState => 
      log.info("Test validation successful")
    case _ -> TestStates.ErrorHandlingState => 
      log.warning("Entering error handling")
    case TestStates.RecoveryState -> TestStates.FirstProcessing => 
      log.info("Recovery completed, restarting process")
  }
  
  startWith(TestStates.InitialState, EmptyTestData)
}