// Example FSM for testing
when(State.Idle) {
  case Event(StartProcessing, _) =>
    goto(State.Processing)
  case Event(Stop, _) =>
    stopSuccess()
}

when(State.Processing) {
  case Event(ProcessComplete, _) =>
    goto(State.Complete)
  case Event(ProcessFailed, _) =>
    goto(State.Failed)
  case Event(Stop, _) =>
    goto(State.Stopping)
}

when(State.Complete) {
  case Event(Reset, _) =>
    goto(State.Idle)
}

when(State.Failed) {
  case Event(Retry, _) =>
    goto(State.Processing)
  case Event(Reset, _) =>
    goto(State.Idle)
}

when(State.Stopping) {
  case Event(ProcessStopped, _) =>
    stopSuccess()
}

def recoverStateDecision(reason: String): State = {
  reason match {
    case "network_error" => Target.enter(State.Idle, InitialData)
    case "critical_error" => Target.enter(State.Failed, ErrorData(reason))
    case _ => Target.enter(State.Processing, RecoveryData)
  }
}
