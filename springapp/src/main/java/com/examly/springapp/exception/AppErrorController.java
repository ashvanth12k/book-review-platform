package com.examly.springapp.exception;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
public class AppErrorController implements ErrorController {

    @RequestMapping("/error")
    public ResponseEntity<Map<String, Object>> handleError(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object path = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);

        int statusCode = (status != null) ? Integer.parseInt(status.toString()) : 500;
        String errorMessage = (message != null && !message.toString().isBlank())
                ? message.toString()
                : "No message available";

        return ResponseEntity.status(statusCode).body(Map.of(
                "status", statusCode,
                "error", errorMessage,
                "path", path != null ? path.toString() : "unknown"
        ));
    }
}
