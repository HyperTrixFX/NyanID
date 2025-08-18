package moe.koseirin.nyanruaineo.utils;


/*
 * @author KoseiRin_
 * awa
 */

import moe.koseirin.nyanruaineo.utils.ErrUtils.Error;
import moe.koseirin.nyanruaineo.utils.ErrUtils.Ygg.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse>  handleException(Exception e) {
        Error errorResponse = new Error();
        errorResponse.setStatus(400);
        errorResponse.setMessage(e.getMessage());
        errorResponse.setError("Bad Request");
        errorResponse.setTimestamp(LocalDateTime.now());
        ErrorResponse error = new ErrorResponse();
        error.setError(errorResponse.getError());
        error.setErrorMessage(errorResponse.getMessage());
        error.setCause(errorResponse.getTimestamp().toString());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse>  Exception(Exception e) {
        Error errorResponse = new Error();
        errorResponse.setStatus(500);
        errorResponse.setMessage(e.getMessage());
        errorResponse.setError("Internal Server Error");
        errorResponse.setTimestamp(LocalDateTime.now());
        ErrorResponse error = new ErrorResponse();
        error.setError(errorResponse.getError());
        error.setErrorMessage(errorResponse.getMessage());
        error.setCause(errorResponse.getTimestamp().toString());
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
