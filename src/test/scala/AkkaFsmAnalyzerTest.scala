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
}