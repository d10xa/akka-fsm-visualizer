import akka.actor.{FSM}
import scala.concurrent.duration._

// Complex FSM for testing high-quality PNG export on large diagrams
sealed trait ProcessingState
object ProcessingStates {
  case object SystemInitialization extends ProcessingState
  case object ConfigurationValidation extends ProcessingState
  case object DatabaseConnectionSetup extends ProcessingState
  case object AuthenticationServiceInitialization extends ProcessingState
  case object ExternalApiConnectionVerification extends ProcessingState
  case object CacheWarmup extends ProcessingState
  case object LoadBalancerRegistration extends ProcessingState
  case object HealthCheckInitialization extends ProcessingState
  case object ServiceDiscoveryRegistration extends ProcessingState
  case object MetricsCollectionSetup extends ProcessingState
  case object LoggingServiceInitialization extends ProcessingState
  case object RequestProcessingReady extends ProcessingState
  
  case object IncomingRequestValidation extends ProcessingState
  case object RequestAuthentication extends ProcessingState
  case object RequestAuthorization extends ProcessingState
  case object RequestRateLimit extends ProcessingState
  case object RequestDataExtraction extends ProcessingState
  case object BusinessLogicExecution extends ProcessingState
  case object DatabaseQueryExecution extends ProcessingState
  case object ExternalServiceInvocation extends ProcessingState
  case object ResponseDataAggregation extends ProcessingState
  case object ResponseFormatting extends ProcessingState
  case object ResponseCaching extends ProcessingState
  case object ResponseDelivery extends ProcessingState
  
  case object ErrorHandlingInitialization extends ProcessingState
  case object ValidationErrorProcessing extends ProcessingState
  case object AuthenticationErrorProcessing extends ProcessingState
  case object AuthorizationErrorProcessing extends ProcessingState
  case object RateLimitErrorProcessing extends ProcessingState
  case object BusinessLogicErrorProcessing extends ProcessingState
  case object DatabaseErrorProcessing extends ProcessingState
  case object ExternalServiceErrorProcessing extends ProcessingState
  case object TimeoutErrorProcessing extends ProcessingState
  case object GeneralErrorProcessing extends ProcessingState
  
  case object CircuitBreakerOpen extends ProcessingState
  case object CircuitBreakerHalfOpen extends ProcessingState
  case object CircuitBreakerClosed extends ProcessingState
  
  case object SystemShutdownInitiation extends ProcessingState
  case object GracefulConnectionClosure extends ProcessingState
  case object ResourceCleanup extends ProcessingState
  case object FinalStateReporting extends ProcessingState
  case object SystemShutdownComplete extends ProcessingState
}

sealed trait ProcessingData
case object EmptyProcessingData extends ProcessingData
case class SystemInitializationData(configVersion: String, startTime: Long) extends ProcessingData
case class RequestProcessingData(requestId: String, userId: String, timestamp: Long) extends ProcessingData
case class ErrorProcessingData(errorType: String, errorMessage: String, retryCount: Int) extends ProcessingData

sealed trait ProcessingEvent
case object InitializeSystem extends ProcessingEvent
case object ValidateConfiguration extends ProcessingEvent
case object SetupDatabaseConnection extends ProcessingEvent
case object InitializeAuthenticationService extends ProcessingEvent
case object VerifyExternalApiConnections extends ProcessingEvent
case object WarmupCache extends ProcessingEvent
case object RegisterWithLoadBalancer extends ProcessingEvent
case object InitializeHealthChecks extends ProcessingEvent
case object RegisterServiceDiscovery extends ProcessingEvent
case object SetupMetricsCollection extends ProcessingEvent
case object InitializeLoggingService extends ProcessingEvent
case object SystemReady extends ProcessingEvent

case object ProcessIncomingRequest extends ProcessingEvent
case object AuthenticateRequest extends ProcessingEvent
case object AuthorizeRequest extends ProcessingEvent
case object CheckRateLimit extends ProcessingEvent
case object ExtractRequestData extends ProcessingEvent
case object ExecuteBusinessLogic extends ProcessingEvent
case object QueryDatabase extends ProcessingEvent
case object InvokeExternalService extends ProcessingEvent
case object AggregateResponseData extends ProcessingEvent
case object FormatResponse extends ProcessingEvent
case object CacheResponse extends ProcessingEvent
case object DeliverResponse extends ProcessingEvent

case object HandleValidationError extends ProcessingEvent
case object HandleAuthenticationError extends ProcessingEvent
case object HandleAuthorizationError extends ProcessingEvent
case object HandleRateLimitError extends ProcessingEvent
case object HandleBusinessLogicError extends ProcessingEvent
case object HandleDatabaseError extends ProcessingEvent
case object HandleExternalServiceError extends ProcessingEvent
case object HandleTimeoutError extends ProcessingEvent
case object HandleGeneralError extends ProcessingEvent

case object OpenCircuitBreaker extends ProcessingEvent
case object CloseCircuitBreaker extends ProcessingEvent
case object HalfOpenCircuitBreaker extends ProcessingEvent

case object InitiateSystemShutdown extends ProcessingEvent
case object CloseConnections extends ProcessingEvent
case object CleanupResources extends ProcessingEvent
case object ReportFinalState extends ProcessingEvent
case object CompleteShutdown extends ProcessingEvent

case object RetryOperation extends ProcessingEvent
case object ResetSystem extends ProcessingEvent

class LargeDiagramTestFSM extends FSM[ProcessingState, ProcessingData] {
  
  // System Initialization Phase
  when(ProcessingStates.SystemInitialization) {
    case Event(InitializeSystem, _) =>
      goto(ProcessingStates.ConfigurationValidation) using SystemInitializationData("v1.0", System.currentTimeMillis())
    case Event(InitiateSystemShutdown, _) =>
      goto(ProcessingStates.SystemShutdownInitiation)
  }

  when(ProcessingStates.ConfigurationValidation) {
    case Event(ValidateConfiguration, data) =>
      goto(ProcessingStates.DatabaseConnectionSetup) using data
    case Event(HandleValidationError, _) =>
      goto(ProcessingStates.ValidationErrorProcessing)
  }

  when(ProcessingStates.DatabaseConnectionSetup) {
    case Event(SetupDatabaseConnection, data) =>
      goto(ProcessingStates.AuthenticationServiceInitialization) using data
    case Event(HandleDatabaseError, _) =>
      goto(ProcessingStates.DatabaseErrorProcessing)
  }

  when(ProcessingStates.AuthenticationServiceInitialization) {
    case Event(InitializeAuthenticationService, data) =>
      goto(ProcessingStates.ExternalApiConnectionVerification) using data
    case Event(HandleAuthenticationError, _) =>
      goto(ProcessingStates.AuthenticationErrorProcessing)
  }

  when(ProcessingStates.ExternalApiConnectionVerification) {
    case Event(VerifyExternalApiConnections, data) =>
      goto(ProcessingStates.CacheWarmup) using data
    case Event(HandleExternalServiceError, _) =>
      goto(ProcessingStates.ExternalServiceErrorProcessing)
  }

  when(ProcessingStates.CacheWarmup) {
    case Event(WarmupCache, data) =>
      goto(ProcessingStates.LoadBalancerRegistration) using data
    case Event(HandleGeneralError, _) =>
      goto(ProcessingStates.GeneralErrorProcessing)
  }

  when(ProcessingStates.LoadBalancerRegistration) {
    case Event(RegisterWithLoadBalancer, data) =>
      goto(ProcessingStates.HealthCheckInitialization) using data
    case Event(HandleGeneralError, _) =>
      goto(ProcessingStates.GeneralErrorProcessing)
  }

  when(ProcessingStates.HealthCheckInitialization) {
    case Event(InitializeHealthChecks, data) =>
      goto(ProcessingStates.ServiceDiscoveryRegistration) using data
    case Event(HandleGeneralError, _) =>
      goto(ProcessingStates.GeneralErrorProcessing)
  }

  when(ProcessingStates.ServiceDiscoveryRegistration) {
    case Event(RegisterServiceDiscovery, data) =>
      goto(ProcessingStates.MetricsCollectionSetup) using data
    case Event(HandleGeneralError, _) =>
      goto(ProcessingStates.GeneralErrorProcessing)
  }

  when(ProcessingStates.MetricsCollectionSetup) {
    case Event(SetupMetricsCollection, data) =>
      goto(ProcessingStates.LoggingServiceInitialization) using data
    case Event(HandleGeneralError, _) =>
      goto(ProcessingStates.GeneralErrorProcessing)
  }

  when(ProcessingStates.LoggingServiceInitialization) {
    case Event(InitializeLoggingService, data) =>
      goto(ProcessingStates.RequestProcessingReady) using data
    case Event(HandleGeneralError, _) =>
      goto(ProcessingStates.GeneralErrorProcessing)
  }

  // Request Processing Phase
  when(ProcessingStates.RequestProcessingReady) {
    case Event(ProcessIncomingRequest, _) =>
      goto(ProcessingStates.IncomingRequestValidation) using RequestProcessingData("req-123", "user-456", System.currentTimeMillis())
    case Event(InitiateSystemShutdown, _) =>
      goto(ProcessingStates.SystemShutdownInitiation)
  }

  when(ProcessingStates.IncomingRequestValidation) {
    case Event(AuthenticateRequest, data) =>
      goto(ProcessingStates.RequestAuthentication) using data
    case Event(HandleValidationError, _) =>
      goto(ProcessingStates.ValidationErrorProcessing)
  }

  when(ProcessingStates.RequestAuthentication) {
    case Event(AuthorizeRequest, data) =>
      goto(ProcessingStates.RequestAuthorization) using data
    case Event(HandleAuthenticationError, _) =>
      goto(ProcessingStates.AuthenticationErrorProcessing)
  }

  when(ProcessingStates.RequestAuthorization) {
    case Event(CheckRateLimit, data) =>
      goto(ProcessingStates.RequestRateLimit) using data
    case Event(HandleAuthorizationError, _) =>
      goto(ProcessingStates.AuthorizationErrorProcessing)
  }

  when(ProcessingStates.RequestRateLimit) {
    case Event(ExtractRequestData, data) =>
      goto(ProcessingStates.RequestDataExtraction) using data
    case Event(HandleRateLimitError, _) =>
      goto(ProcessingStates.RateLimitErrorProcessing)
  }

  when(ProcessingStates.RequestDataExtraction) {
    case Event(ExecuteBusinessLogic, data) =>
      goto(ProcessingStates.BusinessLogicExecution) using data
    case Event(HandleValidationError, _) =>
      goto(ProcessingStates.ValidationErrorProcessing)
  }

  when(ProcessingStates.BusinessLogicExecution) {
    case Event(QueryDatabase, data) =>
      goto(ProcessingStates.DatabaseQueryExecution) using data
    case Event(InvokeExternalService, data) =>
      goto(ProcessingStates.ExternalServiceInvocation) using data
    case Event(HandleBusinessLogicError, _) =>
      goto(ProcessingStates.BusinessLogicErrorProcessing)
  }

  when(ProcessingStates.DatabaseQueryExecution) {
    case Event(AggregateResponseData, data) =>
      goto(ProcessingStates.ResponseDataAggregation) using data
    case Event(HandleDatabaseError, _) =>
      goto(ProcessingStates.DatabaseErrorProcessing)
  }

  when(ProcessingStates.ExternalServiceInvocation) {
    case Event(AggregateResponseData, data) =>
      goto(ProcessingStates.ResponseDataAggregation) using data
    case Event(HandleExternalServiceError, _) =>
      goto(ProcessingStates.ExternalServiceErrorProcessing)
    case Event(HandleTimeoutError, _) =>
      goto(ProcessingStates.TimeoutErrorProcessing)
  }

  when(ProcessingStates.ResponseDataAggregation) {
    case Event(FormatResponse, data) =>
      goto(ProcessingStates.ResponseFormatting) using data
    case Event(HandleGeneralError, _) =>
      goto(ProcessingStates.GeneralErrorProcessing)
  }

  when(ProcessingStates.ResponseFormatting) {
    case Event(CacheResponse, data) =>
      goto(ProcessingStates.ResponseCaching) using data
    case Event(HandleGeneralError, _) =>
      goto(ProcessingStates.GeneralErrorProcessing)
  }

  when(ProcessingStates.ResponseCaching) {
    case Event(DeliverResponse, data) =>
      goto(ProcessingStates.ResponseDelivery) using data
    case Event(HandleGeneralError, _) =>
      goto(ProcessingStates.GeneralErrorProcessing)
  }

  when(ProcessingStates.ResponseDelivery) {
    case Event(ProcessIncomingRequest, _) =>
      goto(ProcessingStates.IncomingRequestValidation) using RequestProcessingData("req-new", "user-new", System.currentTimeMillis())
    case Event(InitiateSystemShutdown, _) =>
      goto(ProcessingStates.SystemShutdownInitiation)
  }

  // Error Handling States with timeouts
  when(ProcessingStates.ValidationErrorProcessing, stateTimeout = 10.seconds) {
    case Event(RetryOperation, _) =>
      goto(ProcessingStates.IncomingRequestValidation)
    case Event(StateTimeout, _) =>
      goto(ProcessingStates.GeneralErrorProcessing)
  }

  when(ProcessingStates.AuthenticationErrorProcessing, stateTimeout = 15.seconds) {
    case Event(RetryOperation, _) =>
      goto(ProcessingStates.RequestAuthentication)
    case Event(StateTimeout, _) =>
      goto(ProcessingStates.GeneralErrorProcessing)
  }

  when(ProcessingStates.AuthorizationErrorProcessing, stateTimeout = 15.seconds) {
    case Event(RetryOperation, _) =>
      goto(ProcessingStates.RequestAuthorization)
    case Event(StateTimeout, _) =>
      goto(ProcessingStates.GeneralErrorProcessing)
  }

  when(ProcessingStates.RateLimitErrorProcessing, stateTimeout = 60.seconds) {
    case Event(RetryOperation, _) =>
      goto(ProcessingStates.RequestRateLimit)
    case Event(StateTimeout, _) =>
      goto(ProcessingStates.GeneralErrorProcessing)
  }

  when(ProcessingStates.BusinessLogicErrorProcessing, stateTimeout = 30.seconds) {
    case Event(RetryOperation, _) =>
      goto(ProcessingStates.BusinessLogicExecution)
    case Event(StateTimeout, _) =>
      goto(ProcessingStates.GeneralErrorProcessing)
  }

  when(ProcessingStates.DatabaseErrorProcessing, stateTimeout = 45.seconds) {
    case Event(RetryOperation, _) =>
      goto(ProcessingStates.DatabaseQueryExecution)
    case Event(StateTimeout, _) =>
      goto(ProcessingStates.GeneralErrorProcessing)
  }

  when(ProcessingStates.ExternalServiceErrorProcessing, stateTimeout = 120.seconds) {
    case Event(RetryOperation, _) =>
      goto(ProcessingStates.ExternalServiceInvocation)
    case Event(OpenCircuitBreaker, _) =>
      goto(ProcessingStates.CircuitBreakerOpen)
    case Event(StateTimeout, _) =>
      goto(ProcessingStates.GeneralErrorProcessing)
  }

  when(ProcessingStates.TimeoutErrorProcessing, stateTimeout = 30.seconds) {
    case Event(RetryOperation, _) =>
      goto(ProcessingStates.ExternalServiceInvocation)
    case Event(OpenCircuitBreaker, _) =>
      goto(ProcessingStates.CircuitBreakerOpen)
    case Event(StateTimeout, _) =>
      goto(ProcessingStates.GeneralErrorProcessing)
  }

  when(ProcessingStates.GeneralErrorProcessing) {
    case Event(ResetSystem, _) =>
      goto(ProcessingStates.SystemInitialization)
    case Event(InitiateSystemShutdown, _) =>
      goto(ProcessingStates.SystemShutdownInitiation)
  }

  // Circuit Breaker States
  when(ProcessingStates.CircuitBreakerOpen, stateTimeout = 300.seconds) {
    case Event(StateTimeout, _) =>
      goto(ProcessingStates.CircuitBreakerHalfOpen)
    case Event(InitiateSystemShutdown, _) =>
      goto(ProcessingStates.SystemShutdownInitiation)
  }

  when(ProcessingStates.CircuitBreakerHalfOpen) {
    case Event(CloseCircuitBreaker, _) =>
      goto(ProcessingStates.CircuitBreakerClosed)
    case Event(OpenCircuitBreaker, _) =>
      goto(ProcessingStates.CircuitBreakerOpen)
  }

  when(ProcessingStates.CircuitBreakerClosed) {
    case Event(ProcessIncomingRequest, _) =>
      goto(ProcessingStates.IncomingRequestValidation)
    case Event(OpenCircuitBreaker, _) =>
      goto(ProcessingStates.CircuitBreakerOpen)
  }

  // Shutdown States
  when(ProcessingStates.SystemShutdownInitiation) {
    case Event(CloseConnections, _) =>
      goto(ProcessingStates.GracefulConnectionClosure)
  }

  when(ProcessingStates.GracefulConnectionClosure) {
    case Event(CleanupResources, _) =>
      goto(ProcessingStates.ResourceCleanup)
  }

  when(ProcessingStates.ResourceCleanup) {
    case Event(ReportFinalState, _) =>
      goto(ProcessingStates.FinalStateReporting)
  }

  when(ProcessingStates.FinalStateReporting) {
    case Event(CompleteShutdown, _) =>
      goto(ProcessingStates.SystemShutdownComplete)
  }

  when(ProcessingStates.SystemShutdownComplete) {
    case Event(_, _) =>
      stopSuccess()
  }

  // Complex onTransition blocks for large diagrams
  onTransition {
    case ProcessingStates.SystemInitialization -> ProcessingStates.ConfigurationValidation =>
      log.info("System initialization completed, validating configuration")
    case ProcessingStates.ConfigurationValidation -> ProcessingStates.DatabaseConnectionSetup =>
      log.info("Configuration validated, setting up database connections")
    case ProcessingStates.RequestProcessingReady -> ProcessingStates.IncomingRequestValidation =>
      log.info("Processing incoming request, starting validation")
    case _ -> ProcessingStates.ExternalServiceErrorProcessing =>
      log.warning("External service error detected, initiating error handling")
    case _ -> ProcessingStates.CircuitBreakerOpen =>
      log.warning("Circuit breaker opened due to repeated failures")
    case ProcessingStates.CircuitBreakerHalfOpen -> ProcessingStates.CircuitBreakerClosed =>
      log.info("Circuit breaker closed, service restored")
  }
  
  startWith(ProcessingStates.SystemInitialization, EmptyProcessingData)
}