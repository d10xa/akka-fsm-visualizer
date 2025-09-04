import munit.FunSuite

class AkkaFsmAnalyzerTest extends FunSuite {

  test("parse simple FSM with direct goto calls") {
    val code = """
      |object State {
      |  case object Idle extends State
      |  case object Processing extends State
      |}
      |
      |class SimpleFSM extends FSM[State, Data] {
      |  when(State.Idle) {
      |    case Event(Start, _) => goto(State.Processing)
      |  }
      |  when(State.Processing) {
      |    case Event(Complete, _) => goto(State.Idle)
      |  }
      |}
    """.stripMargin

    val result = AkkaFsmAnalyzer.parseScalaCode(code)
    assert(result.isRight)
    
    val mermaidCode = result.getOrElse("")
    assert(mermaidCode.contains("stateDiagram-v2"))
    assert(mermaidCode.contains("Idle --> Processing"))
    assert(mermaidCode.contains("Processing --> Idle"))
  }

  test("parse FSM with nested function calls") {
    val code = """
      |object State {
      |  case object WaitingForOrder extends OrderState
      |  case object PaymentPending extends OrderState
      |  case object PaymentFailed extends OrderState
      |  case object Cancelled extends OrderState
      |}
      |
      |class OrderFSM extends FSM[OrderState, OrderData] {
      |  when(State.WaitingForOrder) {
      |    case Event(PlaceOrder, _) => goto(State.PaymentPending)
      |  }
      |  
      |  when(State.PaymentPending) {
      |    case Event(PaymentFailed, orderInfo) => handlePaymentFailure(orderInfo)
      |  }
      |  
      |  def handlePaymentFailure(orderInfo: OrderInfo): State = {
      |    if (orderInfo.amount > 100.0) {
      |      goto(State.PaymentFailed)
      |    } else {
      |      goto(State.Cancelled)
      |    }
      |  }
      |}
    """.stripMargin

    val result = AkkaFsmAnalyzer.parseScalaCode(code)
    assert(result.isRight)
    
    val mermaidCode = result.getOrElse("")
    assert(mermaidCode.contains("stateDiagram-v2"))
    assert(mermaidCode.contains("WaitingForOrder --> PaymentPending"))
    assert(mermaidCode.contains("PaymentPending --> PaymentFailed"))
    assert(mermaidCode.contains("PaymentPending --> Cancelled"))
  }

  test("parse FSM with deeply nested function calls") {
    val code = """
      |object State {
      |  case object ReservingItems extends OrderState
      |  case object PaymentFailed extends OrderState
      |  case object Cancelled extends OrderState
      |}
      |
      |class OrderFSM extends FSM[OrderState, OrderData] {
      |  when(State.ReservingItems) {
      |    case Event(OutOfStock, orderInfo) => handleOutOfStock(orderInfo)
      |  }
      |  
      |  def handleOutOfStock(orderInfo: OrderInfo): State = {
      |    findAlternatives(orderInfo)
      |  }
      |  
      |  def findAlternatives(orderInfo: OrderInfo): State = {
      |    orderInfo.amount match {
      |      case amount if amount > 50.0 => goto(State.Cancelled)
      |      case _ => goto(State.PaymentFailed)
      |    }
      |  }
      |}
    """.stripMargin

    val result = AkkaFsmAnalyzer.parseScalaCode(code)
    assert(result.isRight)
    
    val mermaidCode = result.getOrElse("")
    assert(mermaidCode.contains("ReservingItems --> Cancelled"))
    assert(mermaidCode.contains("ReservingItems --> PaymentFailed"))
  }

  test("parse FSM with stopSuccess calls") {
    val code = """
      |object State {
      |  case object Idle extends State
      |  case object Complete extends State
      |}
      |
      |class SimpleFSM extends FSM[State, Data] {
      |  when(State.Idle) {
      |    case Event(Stop, _) => stopSuccess()
      |  }
      |  when(State.Complete) {
      |    case Event(Finish, _) => stopSuccess()
      |  }
      |}
    """.stripMargin

    val result = AkkaFsmAnalyzer.parseScalaCode(code)
    assert(result.isRight)
    
    val mermaidCode = result.getOrElse("")
    assert(mermaidCode.contains("Idle --> stop"))
    assert(mermaidCode.contains("Complete --> stop"))
    assert(mermaidCode.contains("stop --> [*]"))
  }

  test("parse FSM with Target.enter calls") {
    val code = """
      |object State {
      |  case object RecoverSelf extends State
      |  case object Idle extends State
      |  case object Failed extends State
      |}
      |
      |class RecoveryFSM extends FSM[State, Data] {
      |  def recoverStateDecision(reason: String): State = {
      |    reason match {
      |      case "network_error" => Target.enter(State.Idle, InitialData)
      |      case "critical_error" => Target.enter(State.Failed, ErrorData(reason))
      |    }
      |  }
      |}
    """.stripMargin

    val result = AkkaFsmAnalyzer.parseScalaCode(code)
    assert(result.isRight)
    
    val mermaidCode = result.getOrElse("")
    assert(mermaidCode.contains("RecoverSelf --> Idle"))
    assert(mermaidCode.contains("RecoverSelf --> Failed"))
  }

  test("handle empty FSM gracefully") {
    val code = """
      |class EmptyFSM extends FSM[State, Data] {
      |  // No when blocks
      |}
    """.stripMargin

    val result = AkkaFsmAnalyzer.parseScalaCode(code)
    assert(result.isRight)
    
    val mermaidCode = result.getOrElse("")
    assert(mermaidCode.contains("NoTransitions"))
    assert(mermaidCode.contains("No FSM transitions found"))
  }

  test("handle parse errors gracefully") {
    val invalidCode = """
      |this is not valid scala code {{{
    """.stripMargin

    val result = AkkaFsmAnalyzer.parseScalaCode(invalidCode)
    assert(result.isLeft)
    
    val errorMessage = result.left.getOrElse("")
    assert(errorMessage.contains("Parse error"))
  }

  test("parse FSM with custom state object name") {
    val code = """
      |object OrderStates {
      |  case object WaitingForOrder extends OrderState
      |  case object PaymentPending extends OrderState
      |  case object Complete extends OrderState
      |}
      |
      |class CustomOrderFSM extends FSM[OrderState, Data] {
      |  when(OrderStates.WaitingForOrder) {
      |    case Event(PlaceOrder, _) => goto(OrderStates.PaymentPending)
      |  }
      |  when(OrderStates.PaymentPending) {
      |    case Event(PaymentComplete, _) => goto(OrderStates.Complete)
      |  }
      |}
    """.stripMargin

    val result = AkkaFsmAnalyzer.parseScalaCode(code)
    assert(result.isRight)
    
    val mermaidCode = result.getOrElse("")
    assert(mermaidCode.contains("stateDiagram-v2"))
    assert(mermaidCode.contains("WaitingForOrder --> PaymentPending"))
    assert(mermaidCode.contains("PaymentPending --> Complete"))
  }

  test("parse FSM with multiple state objects") {
    val code = """
      |object ProcessStates {
      |  case object Idle extends ProcessState
      |  case object Running extends ProcessState
      |}
      |
      |object ErrorStates {
      |  case object Failed extends ProcessState
      |  case object Recovering extends ProcessState
      |}
      |
      |class MultiFSM extends FSM[ProcessState, Data] {
      |  when(ProcessStates.Idle) {
      |    case Event(Start, _) => goto(ProcessStates.Running)
      |  }
      |  when(ProcessStates.Running) {
      |    case Event(Error, _) => goto(ErrorStates.Failed)
      |  }
      |  when(ErrorStates.Failed) {
      |    case Event(Recover, _) => goto(ErrorStates.Recovering)
      |  }
      |  when(ErrorStates.Recovering) {
      |    case Event(Complete, _) => goto(ProcessStates.Idle)
      |  }
      |}
    """.stripMargin

    val result = AkkaFsmAnalyzer.parseScalaCode(code)
    assert(result.isRight)
    
    val mermaidCode = result.getOrElse("")
    assert(mermaidCode.contains("Idle --> Running"))
    assert(mermaidCode.contains("Running --> Failed"))
    assert(mermaidCode.contains("Failed --> Recovering"))
    assert(mermaidCode.contains("Recovering --> Idle"))
  }

  test("parse FSM with custom state object and nested functions") {
    val code = """
      |object MyStates {
      |  case object Start extends MyState
      |  case object Processing extends MyState
      |  case object Failed extends MyState
      |  case object Complete extends MyState
      |}
      |
      |class CustomFSM extends FSM[MyState, Data] {
      |  when(MyStates.Start) {
      |    case Event(Begin, _) => handleBegin()
      |  }
      |  
      |  def handleBegin(): State = {
      |    goto(MyStates.Processing)
      |  }
      |  
      |  when(MyStates.Processing) {
      |    case Event(Fail, _) => handleFailure()
      |  }
      |  
      |  def handleFailure(): State = {
      |    checkCanRecover() match {
      |      case true => goto(MyStates.Processing) 
      |      case false => goto(MyStates.Failed)
      |    }
      |  }
      |  
      |  def checkCanRecover(): Boolean = true
      |}
    """.stripMargin

    val result = AkkaFsmAnalyzer.parseScalaCode(code)
    assert(result.isRight)
    
    val mermaidCode = result.getOrElse("")
    assert(mermaidCode.contains("Start --> Processing"))
    assert(mermaidCode.contains("Processing --> Failed"))
  }

  test("handle large FSM without stack overflow") {
    // Create a large FSM with many states and deep nesting
    val largeCode = generateLargeFSMCode(100) // 100 states
    
    val result = AkkaFsmAnalyzer.parseScalaCode(largeCode)
    if (result.isLeft) {
      println(s"Parse error: ${result.left.get}")
    } else {
      val mermaidCode = result.getOrElse("")
      println(s"Generated mermaid code length: ${mermaidCode.length}")
      println(s"First 500 chars: ${mermaidCode.take(500)}")
    }
    
    assert(result.isRight, s"Should parse large FSM without stack overflow, but got: ${result.left.getOrElse("")}")
    
    val mermaidCode = result.getOrElse("")
    assert(mermaidCode.contains("stateDiagram-v2"))
    // More flexible assertions since state names might be cleaned
    assert(mermaidCode.contains("State0") || mermaidCode.contains("State_0"))
    assert(mermaidCode.contains("Complete"))
  }

  test("handle deeply nested function calls") {
    val deepCode = generateDeeplyNestedCode(20) // 20 levels deep
    
    val result = AkkaFsmAnalyzer.parseScalaCode(deepCode)
    assert(result.isRight, s"Should parse deeply nested functions without stack overflow, but got: ${result.left.getOrElse("")}")
    
    val mermaidCode = result.getOrElse("")
    assert(mermaidCode.contains("Start --> Processing"))
    assert(mermaidCode.contains("Processing --> Complete"))
  }

  private def generateLargeFSMCode(numStates: Int): String = {
    val states = (0 until numStates).map(i => s"  case object State$i extends ProcessingState").mkString("\n")
    
    val whenBlocks = (0 until numStates).map { i =>
      val nextState = if (i == numStates - 1) "Complete" else s"State${i + 1}"
      s"""  when(State.State$i) {
         |    case Event(Next, _) => processNext($i)
         |    case Event(Fail, _) => goto(State.Failed)
         |  }""".stripMargin
    }.mkString("\n\n")
    
    val processNextCases = (0 until numStates).map { i =>
      val nextState = if (i == numStates - 1) "Complete" else s"State${i + 1}"
      s"      case $i => handleState(State.$nextState)"
    }.mkString("\n")
    
    s"""object State {
       |$states
       |  case object Complete extends ProcessingState
       |  case object Failed extends ProcessingState
       |}
       |
       |class LargeFSM extends FSM[ProcessingState, Data] {
       |$whenBlocks
       |
       |  when(State.Complete) {
       |    case Event(Reset, _) => goto(State.State0)
       |  }
       |
       |  when(State.Failed) {
       |    case Event(Reset, _) => goto(State.State0)
       |  }
       |
       |  def processNext(stateNum: Int): State = {
       |    stateNum match {
       |$processNextCases
       |      case _ => goto(State.Failed)
       |    }
       |  }
       |
       |  def handleState(state: ProcessingState): State = {
       |    validateState(state) match {
       |      case true => goto(state)
       |      case false => goto(State.Failed)
       |    }
       |  }
       |
       |  def validateState(state: ProcessingState): Boolean = true
       |
       |  startWith(State.State0, EmptyData)
       |}""".stripMargin
  }

  private def generateDeeplyNestedCode(depth: Int): String = {
    val functions = (1 to depth).map { i =>
      val nextCall = if (i == depth) "goto(State.Complete)" else s"level${i + 1}()"
      s"""  def level$i(): State = {
         |    $nextCall
         |  }""".stripMargin
    }.mkString("\n\n")
    
    s"""object State {
       |  case object Start extends ProcessingState
       |  case object Processing extends ProcessingState
       |  case object Complete extends ProcessingState
       |}
       |
       |class DeepFSM extends FSM[ProcessingState, Data] {
       |  when(State.Start) {
       |    case Event(Begin, _) => goto(State.Processing)
       |  }
       |
       |  when(State.Processing) {
       |    case Event(Process, _) => level1()
       |  }
       |
       |$functions
       |
       |  startWith(State.Start, EmptyData)
       |}""".stripMargin
  }

  test("handle infinite loop protection") {
    // This should not hang the parser
    val infiniteCode = """
      |object State {
      |  case object Start extends ProcessingState
      |  case object Processing extends ProcessingState
      |  case object Complete extends ProcessingState
      |}
      |
      |class InfiniteFSM extends FSM[ProcessingState, Data] {
      |  when(State.Start) {
      |    case Event(Begin, _) => circularFunction1()
      |  }
      |  
      |  def circularFunction1(): State = {
      |    circularFunction2()
      |  }
      |  
      |  def circularFunction2(): State = {
      |    circularFunction1() // This creates a cycle
      |  }
      |}
    """.stripMargin

    val result = AkkaFsmAnalyzer.parseScalaCode(infiniteCode)
    // Should succeed and show unparsed transitions for problematic parts
    assert(result.isRight)
    
    val mermaidCode = result.getOrElse("")
    // Should contain either valid transitions or unparsed markers
    assert(mermaidCode.contains("stateDiagram-v2"))
  }

  test("generate valid Mermaid syntax with special characters") {
    val code = """
      |object States {
      |  case object NormalState extends ProcessState
      |  case object AnotherState extends ProcessState
      |}
      |
      |class SpecialFSM extends FSM[ProcessState, Data] {
      |  when(States.NormalState) {
      |    case Event(Next, _) => goto(States.AnotherState)
      |  }
      |  
      |  // This function will be unparsed due to complexity
      |  def complexFunction(): State = {
      |    val specialName = "state-with@special#chars"
      |    goto(States.AnotherState)
      |  }
      |}
    """.stripMargin

    val result = AkkaFsmAnalyzer.parseScalaCode(code)
    assert(result.isRight)
    
    val mermaidCode = result.getOrElse("")
    assert(mermaidCode.contains("stateDiagram-v2"))
    // Should generate valid Mermaid without syntax errors
    assert(mermaidCode.contains("NormalState --> AnotherState"))
  }

  test("handle FSM with only state definitions") {
    val code = """
      |import akka.actor.{Actor, FSM, Props}
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
    """.stripMargin

    val result = AkkaFsmAnalyzer.parseScalaCode(code)
    assert(result.isRight)
    
    val mermaidCode = result.getOrElse("")
    println(s"Generated for state-only FSM: $mermaidCode")
    assert(mermaidCode.contains("stateDiagram-v2"))
    assert(mermaidCode.contains("NoTransitions") || mermaidCode.contains("EmptyFSM"))
  }

}