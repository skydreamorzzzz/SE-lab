package edu.market;

import java.util.Map;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = {DesignCatalogController.class, DesignProductController.class})
public class DesignApiErrors {
    @ExceptionHandler(BusinessException.class)
    ResponseEntity<Map<String, String>> business(BusinessException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Map<String, String>> duplicate(DataIntegrityViolationException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", "记录冲突，请刷新后重试"));
    }
}
