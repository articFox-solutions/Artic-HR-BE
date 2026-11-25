package com.articfox.hr.common;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class ManejadorErrores {

    @ExceptionHandler(ErrorApi.class)
    ResponseEntity<ProblemDetail> errorApi(ErrorApi error) {
        return ProblemaHttp.respuesta(error.getStatus(), error.getCodigo(), error.getMessage());
    }

    /** Errores de formato por campo, para que Angular los muestre junto a cada input. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> validacion(MethodArgumentNotValidException error) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError campo : error.getBindingResult().getFieldErrors()) {
            errores.putIfAbsent(campo.getField(), campo.getDefaultMessage());
        }
        ProblemDetail problema = ProblemaHttp.crear(HttpStatus.BAD_REQUEST, "VALIDACION",
                "Revisa los datos enviados.");
        problema.setProperty("errores", errores);
        return ResponseEntity.badRequest().body(problema);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ProblemDetail> cuerpoInvalido(HttpMessageNotReadableException error) {
        return ProblemaHttp.respuesta(HttpStatus.BAD_REQUEST, "VALIDACION", "El cuerpo de la petición no es válido.");
    }
}
