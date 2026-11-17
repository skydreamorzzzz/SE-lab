package edu.market;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@ControllerAdvice
public class WebErrors {
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    String business(BusinessException error, Model model) {
        model.addAttribute("message", error.getMessage());
        return "business-error";
    }

    @ExceptionHandler({DataIntegrityViolationException.class, MethodArgumentTypeMismatchException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    String input(Exception error, Model model) {
        model.addAttribute("message", "参数无效或请求重复，请检查后重试");
        return "business-error";
    }
}
