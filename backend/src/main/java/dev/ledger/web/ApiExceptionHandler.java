package dev.ledger.web;

import dev.ledger.service.NotFoundException;
import dev.ledger.service.UnbalancedTransactionException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    public record ApiError(String error, Map<String, String> details) {

        static ApiError of(String error) {
            return new ApiError(error, Map.of());
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError onValidationError(MethodArgumentNotValidException e) {
        Map<String, String> details = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(f -> details.putIfAbsent(f.getField(), f.getDefaultMessage()));
        return new ApiError("Validation failed", details);
    }

    /**
     * Raised by method validation on the service, which is what catches bad
     * data arriving from the CSV importer rather than through a controller.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError onConstraintViolation(ConstraintViolationException e) {
        Map<String, String> details = new LinkedHashMap<>();
        for (ConstraintViolation<?> violation : e.getConstraintViolations()) {
            details.putIfAbsent(leafProperty(violation), violation.getMessage());
        }
        return new ApiError("Validation failed", details);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError onBadRequest(IllegalArgumentException e) {
        return ApiError.of(e.getMessage());
    }

    /**
     * A unique or check constraint rejected the write. In practice this is a
     * concurrent posting that reused an idempotency key: the first one won, and
     * retrying returns that original transaction instead of a duplicate.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError onConflict(DataIntegrityViolationException e) {
        return ApiError.of("The write conflicted with an existing record; retry the request");
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError onNotFound(NotFoundException e) {
        return ApiError.of(e.getMessage());
    }

    @ExceptionHandler(UnbalancedTransactionException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ApiError onUnbalanced(UnbalancedTransactionException e) {
        return ApiError.of(e.getMessage());
    }

    /** "post.request.legs[0].amount" reads better as just "legs[0].amount". */
    private static String leafProperty(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        int firstDot = path.indexOf('.');
        int secondDot = firstDot < 0 ? -1 : path.indexOf('.', firstDot + 1);
        return secondDot < 0 ? path : path.substring(secondDot + 1);
    }
}
