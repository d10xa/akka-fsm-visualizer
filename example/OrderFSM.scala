import akka.actor.{Actor, FSM, Props}
import scala.concurrent.duration._

// События
sealed trait OrderEvent
case object PlaceOrder extends OrderEvent
case object PaymentReceived extends OrderEvent
case object PaymentFailed extends OrderEvent
case object ItemsReserved extends OrderEvent
case object OutOfStock extends OrderEvent
case object OrderShipped extends OrderEvent
case object OrderDelivered extends OrderEvent
case object OrderCancelled extends OrderEvent
case object RefundIssued extends OrderEvent

// Состояния
sealed trait OrderState
object State {
  case object WaitingForOrder extends OrderState
  case object PaymentPending extends OrderState
  case object PaymentFailed extends OrderState
  case object ReservingItems extends OrderState
  case object ReadyToShip extends OrderState
  case object Shipped extends OrderState
  case object Delivered extends OrderState
  case object Cancelled extends OrderState
  case object Refunded extends OrderState
}

// Данные
sealed trait OrderData
case object NoData extends OrderData
case class OrderInfo(orderId: String, amount: Double) extends OrderData

class OrderProcessingFSM extends Actor with FSM[OrderState, OrderData] {

  when(State.WaitingForOrder) {
    case Event(PlaceOrder, _) =>
      println("Order placed, processing payment...")
      goto(State.PaymentPending) using OrderInfo("ORDER-123", 99.99)
  }

  when(State.PaymentPending) {
    case Event(PaymentReceived, orderInfo) =>
      println("Payment successful, reserving items...")
      goto(State.ReservingItems) using orderInfo
      
    case Event(PaymentFailed, orderInfo) =>
      println("Payment failed")
      handlePaymentFailure(orderInfo)
  }

  when(State.PaymentFailed) {
    case Event(PaymentReceived, orderInfo) =>
      println("Retry payment successful")
      goto(State.ReservingItems) using orderInfo
      
    case Event(OrderCancelled, _) =>
      println("Order cancelled after payment failure")
      goto(State.Cancelled) using NoData
  }

  when(State.ReservingItems) {
    case Event(ItemsReserved, orderInfo) =>
      println("Items reserved successfully")
      goto(State.ReadyToShip) using orderInfo
      
    case Event(OutOfStock, orderInfo) =>
      println("Items out of stock")
      handleOutOfStock(orderInfo)
  }

  when(State.ReadyToShip) {
    case Event(OrderShipped, orderInfo) =>
      println("Order shipped")
      goto(State.Shipped) using orderInfo
      
    case Event(OrderCancelled, orderInfo) =>
      println("Order cancelled before shipping")
      processRefund(orderInfo)
  }

  when(State.Shipped) {
    case Event(OrderDelivered, orderInfo) =>
      println("Order delivered successfully")
      goto(State.Delivered) using orderInfo
      
    case Event(OrderCancelled, orderInfo) =>
      println("Order cancelled after shipping")
      processRefund(orderInfo)
  }

  when(State.Delivered) {
    case Event(OrderCancelled, orderInfo) =>
      println("Return requested")
      processRefund(orderInfo)
  }

  when(State.Cancelled) {
    case Event(RefundIssued, _) =>
      println("Refund processed")
      goto(State.Refunded) using NoData
  }

  when(State.Refunded) {
    case Event(PlaceOrder, _) =>
      println("Customer placing new order")
      goto(State.PaymentPending) using OrderInfo("ORDER-NEW", 149.99)
  }

  // Вспомогательные функции с переходами состояний
  def handlePaymentFailure(orderInfo: OrderInfo): State = {
    if (orderInfo.amount > 100.0) {
      println("High value order - giving second chance")
      goto(State.PaymentFailed) using orderInfo
    } else {
      println("Low value order - cancelling immediately")
      goto(State.Cancelled) using NoData
    }
  }

  def handleOutOfStock(orderInfo: OrderInfo): State = {
    println("Checking alternative options...")
    findAlternatives(orderInfo)
  }

  def findAlternatives(orderInfo: OrderInfo): State = {
    orderInfo.amount match {
      case amount if amount > 50.0 =>
        println("High value order - offering refund")
        goto(State.Cancelled) using orderInfo
      case _ =>
        println("Low value order - suggesting alternatives")
        goto(State.PaymentFailed) using orderInfo
    }
  }

  def processRefund(orderInfo: OrderInfo): State = {
    println("Processing refund for order")
    goto(State.Cancelled) using orderInfo
  }

  startWith(State.WaitingForOrder, NoData)

  // Таймауты
  when(State.PaymentPending, stateTimeout = 5.minutes) {
    case Event(StateTimeout, orderInfo) =>
      println("Payment timeout")
      goto(State.Cancelled) using NoData
  }

  initialize()
}

object OrderProcessingFSM {
  def props(): Props = Props(new OrderProcessingFSM())
}