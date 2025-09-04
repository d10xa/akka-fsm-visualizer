import akka.actor.{Actor, FSM, Props}
import scala.concurrent.duration._

// Большая FSM для тестирования stack overflow
sealed trait ProcessingEvent
case object Start extends ProcessingEvent
case object Next extends ProcessingEvent
case object Complete extends ProcessingEvent
case object Fail extends ProcessingEvent
case object Reset extends ProcessingEvent

sealed trait ProcessingState
object State {
  case object State0 extends ProcessingState
  case object State1 extends ProcessingState
  case object State2 extends ProcessingState
  case object State3 extends ProcessingState
  case object State4 extends ProcessingState
  case object State5 extends ProcessingState
  case object State6 extends ProcessingState
  case object State7 extends ProcessingState
  case object State8 extends ProcessingState
  case object State9 extends ProcessingState
  case object State10 extends ProcessingState
  case object State11 extends ProcessingState
  case object State12 extends ProcessingState
  case object State13 extends ProcessingState
  case object State14 extends ProcessingState
  case object State15 extends ProcessingState
  case object State16 extends ProcessingState
  case object State17 extends ProcessingState
  case object State18 extends ProcessingState
  case object State19 extends ProcessingState
  case object State20 extends ProcessingState
  case object State21 extends ProcessingState
  case object State22 extends ProcessingState
  case object State23 extends ProcessingState
  case object State24 extends ProcessingState
  case object State25 extends ProcessingState
  case object State26 extends ProcessingState
  case object State27 extends ProcessingState
  case object State28 extends ProcessingState
  case object State29 extends ProcessingState
  case object State30 extends ProcessingState
  case object State31 extends ProcessingState
  case object State32 extends ProcessingState
  case object State33 extends ProcessingState
  case object State34 extends ProcessingState
  case object State35 extends ProcessingState
  case object State36 extends ProcessingState
  case object State37 extends ProcessingState
  case object State38 extends ProcessingState
  case object State39 extends ProcessingState
  case object State40 extends ProcessingState
  case object State41 extends ProcessingState
  case object State42 extends ProcessingState
  case object State43 extends ProcessingState
  case object State44 extends ProcessingState
  case object State45 extends ProcessingState
  case object State46 extends ProcessingState
  case object State47 extends ProcessingState
  case object State48 extends ProcessingState
  case object State49 extends ProcessingState
  case object Complete extends ProcessingState
  case object Failed extends ProcessingState
}

sealed trait ProcessingData
case object EmptyData extends ProcessingData

class LargeProcessingFSM extends Actor with FSM[ProcessingState, ProcessingData] {

  when(State.State0) {
    case Event(Start, _) => goto(State.State1)
    case Event(Fail, _) => goto(State.Failed)
  }

  when(State.State1) {
    case Event(Next, _) => processNext(2)
    case Event(Fail, _) => goto(State.Failed)
  }

  when(State.State2) {
    case Event(Next, _) => processNext(3)
    case Event(Fail, _) => goto(State.Failed)
  }

  when(State.State3) {
    case Event(Next, _) => processNext(4)
    case Event(Fail, _) => goto(State.Failed)
  }

  when(State.State4) {
    case Event(Next, _) => processNext(5)
    case Event(Fail, _) => goto(State.Failed)
  }

  when(State.State5) {
    case Event(Next, _) => processNext(6)
    case Event(Fail, _) => goto(State.Failed)
  }

  when(State.State6) {
    case Event(Next, _) => processNext(7)
    case Event(Fail, _) => goto(State.Failed)
  }

  when(State.State7) {
    case Event(Next, _) => processNext(8)
    case Event(Fail, _) => goto(State.Failed)
  }

  when(State.State8) {
    case Event(Next, _) => processNext(9)
    case Event(Fail, _) => goto(State.Failed)
  }

  when(State.State9) {
    case Event(Next, _) => processNext(10)
    case Event(Fail, _) => goto(State.Failed)
  }

  when(State.State10) {
    case Event(Next, _) => processNext(11)
    case Event(Fail, _) => goto(State.Failed)
  }

  when(State.State11) {
    case Event(Next, _) => processNext(12)
    case Event(Fail, _) => goto(State.Failed)
  }

  when(State.State12) {
    case Event(Next, _) => processNext(13)
    case Event(Fail, _) => goto(State.Failed)
  }

  when(State.State13) {
    case Event(Next, _) => processNext(14)
    case Event(Fail, _) => goto(State.Failed)
  }

  when(State.State14) {
    case Event(Next, _) => processNext(15)
    case Event(Fail, _) => goto(State.Failed)
  }

  when(State.State15) {
    case Event(Next, _) => processNext(16)
    case Event(Fail, _) => goto(State.Failed)
  }

  when(State.State16) {
    case Event(Next, _) => processNext(17)
    case Event(Fail, _) => goto(State.Failed)
  }

  when(State.State17) {
    case Event(Next, _) => processNext(18)
    case Event(Fail, _) => goto(State.Failed)
  }

  when(State.State18) {
    case Event(Next, _) => processNext(19)
    case Event(Fail, _) => goto(State.Failed)
  }

  when(State.State19) {
    case Event(Next, _) => processNext(20)
    case Event(Fail, _) => goto(State.Failed)
  }

  when(State.State20) {
    case Event(Next, _) => processNext(21)
    case Event(Fail, _) => goto(State.Failed)
  }

  when(State.State21) {
    case Event(Next, _) => processNext(22)
    case Event(Fail, _) => goto(State.Failed)
  }

  when(State.State22) {
    case Event(Next, _) => processNext(23)
    case Event(Fail, _) => goto(State.Failed)
  }

  when(State.State23) {
    case Event(Next, _) => processNext(24)
    case Event(Fail, _) => goto(State.Failed)
  }

  when(State.State24) {
    case Event(Next, _) => processNext(25)
    case Event(Fail, _) => goto(State.Failed)
  }

  when(State.State25) {
    case Event(Next, _) => processNext(26)
    case Event(Fail, _) => goto(State.Failed)
  }

  when(State.State26) {
    case Event(Next, _) => processNext(27)
    case Event(Fail, _) => goto(State.Failed)
  }

  when(State.State27) {
    case Event(Next, _) => processNext(28)
    case Event(Fail, _) => goto(State.Failed)
  }

  when(State.State28) {
    case Event(Next, _) => processNext(29)
    case Event(Fail, _) => goto(State.Failed)
  }

  when(State.State29) {
    case Event(Next, _) => processNext(30)
    case Event(Fail, _) => goto(State.Failed)
  }

  when(State.State30) {
    case Event(Next, _) => processNext(31)
    case Event(Fail, _) => goto(State.Failed)
  }

  when(State.State31) {
    case Event(Next, _) => processNext(32)
    case Event(Fail, _) => goto(State.Failed)
  }

  when(State.State32) {
    case Event(Next, _) => processNext(33)
    case Event(Fail, _) => goto(State.Failed)
  }

  when(State.State33) {
    case Event(Next, _) => processNext(34)
    case Event(Fail, _) => goto(State.Failed)
  }

  when(State.State34) {
    case Event(Next, _) => processNext(35)
    case Event(Fail, _) => goto(State.Failed)
  }

  when(State.State35) {
    case Event(Next, _) => processNext(36)
    case Event(Fail, _) => goto(State.Failed)
  }

  when(State.State36) {
    case Event(Next, _) => processNext(37)
    case Event(Fail, _) => goto(State.Failed)
  }

  when(State.State37) {
    case Event(Next, _) => processNext(38)
    case Event(Fail, _) => goto(State.Failed)
  }

  when(State.State38) {
    case Event(Next, _) => processNext(39)
    case Event(Fail, _) => goto(State.Failed)
  }

  when(State.State39) {
    case Event(Next, _) => processNext(40)
    case Event(Fail, _) => goto(State.Failed)
  }

  when(State.State40) {
    case Event(Next, _) => processNext(41)
    case Event(Fail, _) => goto(State.Failed)
  }

  when(State.State41) {
    case Event(Next, _) => processNext(42)
    case Event(Fail, _) => goto(State.Failed)
  }

  when(State.State42) {
    case Event(Next, _) => processNext(43)
    case Event(Fail, _) => goto(State.Failed)
  }

  when(State.State43) {
    case Event(Next, _) => processNext(44)
    case Event(Fail, _) => goto(State.Failed)
  }

  when(State.State44) {
    case Event(Next, _) => processNext(45)
    case Event(Fail, _) => goto(State.Failed)
  }

  when(State.State45) {
    case Event(Next, _) => processNext(46)
    case Event(Fail, _) => goto(State.Failed)
  }

  when(State.State46) {
    case Event(Next, _) => processNext(47)
    case Event(Fail, _) => goto(State.Failed)
  }

  when(State.State47) {
    case Event(Next, _) => processNext(48)
    case Event(Fail, _) => goto(State.Failed)
  }

  when(State.State48) {
    case Event(Next, _) => processNext(49)
    case Event(Fail, _) => goto(State.Failed)
  }

  when(State.State49) {
    case Event(Complete, _) => goto(State.Complete)
    case Event(Fail, _) => goto(State.Failed)
  }

  when(State.Complete) {
    case Event(Reset, _) => goto(State.State0)
  }

  when(State.Failed) {
    case Event(Reset, _) => goto(State.State0)
  }

  // Вложенные функции для усиления проблемы
  def processNext(stateNum: Int): State = {
    stateNum match {
      case 2 => handleState(State.State2)
      case 3 => handleState(State.State3)
      case 4 => handleState(State.State4)
      case 5 => handleState(State.State5)
      case 6 => handleState(State.State6)
      case 7 => handleState(State.State7)
      case 8 => handleState(State.State8)
      case 9 => handleState(State.State9)
      case 10 => handleState(State.State10)
      case 11 => handleState(State.State11)
      case 12 => handleState(State.State12)
      case 13 => handleState(State.State13)
      case 14 => handleState(State.State14)
      case 15 => handleState(State.State15)
      case 16 => handleState(State.State16)
      case 17 => handleState(State.State17)
      case 18 => handleState(State.State18)
      case 19 => handleState(State.State19)
      case 20 => handleState(State.State20)
      case 21 => handleState(State.State21)
      case 22 => handleState(State.State22)
      case 23 => handleState(State.State23)
      case 24 => handleState(State.State24)
      case 25 => handleState(State.State25)
      case 26 => handleState(State.State26)
      case 27 => handleState(State.State27)
      case 28 => handleState(State.State28)
      case 29 => handleState(State.State29)
      case 30 => handleState(State.State30)
      case 31 => handleState(State.State31)
      case 32 => handleState(State.State32)
      case 33 => handleState(State.State33)
      case 34 => handleState(State.State34)
      case 35 => handleState(State.State35)
      case 36 => handleState(State.State36)
      case 37 => handleState(State.State37)
      case 38 => handleState(State.State38)
      case 39 => handleState(State.State39)
      case 40 => handleState(State.State40)
      case 41 => handleState(State.State41)
      case 42 => handleState(State.State42)
      case 43 => handleState(State.State43)
      case 44 => handleState(State.State44)
      case 45 => handleState(State.State45)
      case 46 => handleState(State.State46)
      case 47 => handleState(State.State47)
      case 48 => handleState(State.State48)
      case 49 => handleState(State.State49)
      case _ => goto(State.Failed)
    }
  }

  def handleState(state: ProcessingState): State = {
    validateState(state) match {
      case true => goto(state)
      case false => goto(State.Failed)
    }
  }

  def validateState(state: ProcessingState): Boolean = {
    checkStateValidity(state)
  }

  def checkStateValidity(state: ProcessingState): Boolean = {
    performValidation(state)
  }

  def performValidation(state: ProcessingState): Boolean = {
    finalValidation(state)
  }

  def finalValidation(state: ProcessingState): Boolean = true

  startWith(State.State0, EmptyData)
  initialize()
}