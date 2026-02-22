package moe.koseirin.nyanruaineo.utils.System.Exception;


/*
 * @author KoseiRin_
 * awa
 */


/*
 * @author KoseiRin_
 * awa
 */


/*
 * @author KoseiRin_
 * awa
 */

import moe.koseirin.nyanruaineo.utils.Respond;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;


import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.naming.ServiceUnavailableException;
import java.time.LocalDateTime;

@ControllerAdvice
public class GlobalExceptionHandler {
    private final Respond respond;

    public GlobalExceptionHandler(Respond respond) {
        this.respond = respond;
    }


    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<?> handleHttpRequestMethodNotSupported() {
        return respond.respond(MediaType.APPLICATION_JSON, 405,
                Res("HTTP request method is not supported", LocalDateTime.now(), "HttpRequestMethodNotSupportedException"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleHttpMessageNotReadable() {
        return respond.respond(MediaType.APPLICATION_JSON, 400,
                Res("Request body is missing or malformed JSON", LocalDateTime.now(), "HttpMessageNotReadableException"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleMethodArgumentNotValid() {
        return respond.respond(MediaType.APPLICATION_JSON, 400,
                Res("Request parameter validation failed", LocalDateTime.now(), "MethodArgumentNotValidException"));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<?> handleMissingServletRequestParameter() {
        return respond.respond(MediaType.APPLICATION_JSON, 400,
                Res("Required request parameter is missing", LocalDateTime.now(), "MissingServletRequestParameterException"));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<?> handleMissingServletRequestPart() {
        return respond.respond(MediaType.APPLICATION_JSON, 400,
                Res("Required request part is missing", LocalDateTime.now(), "MissingServletRequestPartException"));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<?> handleMethodArgumentTypeMismatch() {
        return respond.respond(MediaType.APPLICATION_JSON, 400,
                Res("Request parameter type mismatch", LocalDateTime.now(), "MethodArgumentTypeMismatchException"));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<?> handleConstraintViolation() {
        return respond.respond(MediaType.APPLICATION_JSON, 400,
                Res("Constraint validation failed", LocalDateTime.now(), "ConstraintViolationException"));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<?> handleNoHandlerFound() {
        return respond.respond(MediaType.APPLICATION_JSON, 404,
                Res("No handler found for this request", LocalDateTime.now(), "NoHandlerFoundException"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgumentException() {
        return respond.respond(MediaType.APPLICATION_JSON, 400,
                Res("Invalid argument in request", LocalDateTime.now(), "IllegalArgumentException"));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<?> handleSecurityException() {
        return respond.respond(MediaType.APPLICATION_JSON, 401,
                Res("Security exception occurred", LocalDateTime.now(), "SecurityException"));
    }

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<?> handleNullPointerException() {
        return respond.respond(MediaType.APPLICATION_JSON, 500,
                Res("Internal server error - null reference", LocalDateTime.now(), "NullPointerException"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGenericException() {
        return respond.respond(MediaType.APPLICATION_JSON, 500,
                Res("Internal server error occurred", LocalDateTime.now(), "Exception"));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<?> handleIllegalStateException() {
        return respond.respond(MediaType.APPLICATION_JSON, 400,
                Res("Invalid request state", LocalDateTime.now(), "IllegalStateException"));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<?> handleMaxUploadSizeExceededException() {
        return respond.respond(MediaType.APPLICATION_JSON, 413,
                Res("Request payload is too large", LocalDateTime.now(), "MaxUploadSizeExceededException"));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<?> handleHttpMediaTypeNotSupportedException() {
        return respond.respond(MediaType.APPLICATION_JSON, 415,
                Res("Media type is not supported", LocalDateTime.now(), "HttpMediaTypeNotSupportedException"));
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<?> handleHttpMediaTypeNotAcceptableException() {
        return respond.respond(MediaType.APPLICATION_JSON, 406,
                Res("Media type is not acceptable", LocalDateTime.now(), "HttpMediaTypeNotAcceptableException"));
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<?> handleServiceUnavailableException() {
        return respond.respond(MediaType.APPLICATION_JSON, 503,
                Res("Service is temporarily unavailable", LocalDateTime.now(), "ServiceUnavailableException"));
    }










    private Object[] Res(String message,LocalDateTime timestamp,String exception){
      return new Object[]{"message",message, "timestamp",timestamp, "Exception", exception};
    }
}
