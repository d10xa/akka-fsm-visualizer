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
    assert(mermaidCode.contains("EmptyFSM"))
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
}