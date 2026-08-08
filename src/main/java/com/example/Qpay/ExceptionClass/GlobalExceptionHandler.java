package com.example.Qpay.ExceptionClass;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  // ── Custom Exceptions ────────────────────────────────────────────────────

  public static class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String msg) { super(msg); }
  }

  public static class BadRequestException extends RuntimeException {
    public BadRequestException(String msg) { super(msg); }
  }

  public static class ConflictException extends RuntimeException {
    public ConflictException(String msg) { super(msg); }
  }

  public static class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String msg) { super(msg); }
  }

  public static class OtpException extends RuntimeException {
    public OtpException(String msg) { super(msg); }
  }

  public static class SessionException extends RuntimeException {
    public SessionException(String msg) { super(msg); }
  }

  public static class PaymentException extends RuntimeException {
    public PaymentException(String msg) { super(msg); }
  }

  public static class RateLimitException extends RuntimeException {
    public RateLimitException(String msg) { super(msg); }
  }

  // ── Error Response ───────────────────────────────────────────────────────

  @Data @Builder
  public static class ErrorResponse {
    private boolean success;
    private int status;
    private String error;
    private String message;
    private OffsetDateTime timestamp;
    private Map<String, String> fieldErrors;
  }

  // ── Handlers ─────────────────────────────────────────────────────────────

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
    return error(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage());
  }

  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex) {
    return error(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage());
  }

  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex) {
    return error(HttpStatus.CONFLICT, "Conflict", ex.getMessage());
  }

  @ExceptionHandler(UnauthorizedException.class)
  public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException ex) {
    return error(HttpStatus.UNAUTHORIZED, "Unauthorized", ex.getMessage());
  }

  @ExceptionHandler(OtpException.class)
  public ResponseEntity<ErrorResponse> handleOtp(OtpException ex) {
    return error(HttpStatus.BAD_REQUEST, "OTP Error", ex.getMessage());
  }

  @ExceptionHandler(SessionException.class)
  public ResponseEntity<ErrorResponse> handleSession(SessionException ex) {
    return error(HttpStatus.CONFLICT, "Session Error", ex.getMessage());
  }

  @ExceptionHandler(PaymentException.class)
  public ResponseEntity<ErrorResponse> handlePayment(PaymentException ex) {
    return error(HttpStatus.BAD_REQUEST, "Payment Error", ex.getMessage());
  }

  @ExceptionHandler(RateLimitException.class)
  public ResponseEntity<ErrorResponse> handleRateLimit(RateLimitException ex) {
    return error(HttpStatus.TOO_MANY_REQUESTS, "Rate Limit Exceeded", ex.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
    Map<String, String> fieldErrors = new HashMap<>();
    for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
      fieldErrors.put(fe.getField(), fe.getDefaultMessage());
    }
    ErrorResponse response = ErrorResponse.builder()
            .success(false)
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Validation Failed")
            .message("One or more fields are invalid")
            .timestamp(OffsetDateTime.now())
            .fieldErrors(fieldErrors)
            .build();
    return ResponseEntity.badRequest().body(response);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
    log.error("Unhandled exception: ", ex);
    return error(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
            "An unexpected error occurred. Please try again.");
  }

  private ResponseEntity<ErrorResponse> error(HttpStatus status, String error, String message) {
    return ResponseEntity.status(status).body(
            ErrorResponse.builder()
                    .success(false)
                    .status(status.value())
                    .error(error)
                    .message(message)
                    .timestamp(OffsetDateTime.now())
                    .build()
    );
  }
}
