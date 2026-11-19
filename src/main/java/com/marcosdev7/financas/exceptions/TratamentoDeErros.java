package com.marcosdev7.financas.exceptions;

import com.marcosdev7.financas.dto.DadosErroMensagemDTO;
import com.marcosdev7.financas.dto.DadosErroValidacaoDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.NoSuchElementException;

@RestControllerAdvice
public class TratamentoDeErros {

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity tratarErro404(NoSuchElementException ex){
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new DadosErroMensagemDTO("ID não encontrado"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity tratarRegraDeNegocio(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(new DadosErroMensagemDTO(ex.getMessage()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity tratarCredenciaisInvalidas(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new DadosErroMensagemDTO(ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity tratarAcessoNegado(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new DadosErroMensagemDTO(ex.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity tratarErro400() {
        return ResponseEntity.badRequest().body("O corpo da requisição e obrigatório");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity tratarErroValidacao(MethodArgumentNotValidException exception){
        var listaErros = exception.getFieldErrors().stream()
                .map(erro -> new DadosErroValidacaoDTO(erro.getField(), erro.getDefaultMessage())).toList();
        return ResponseEntity.badRequest().body(listaErros);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity tratarErroDeRota(){
       return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new DadosErroMensagemDTO("Rota não encontrada"));
    }
}
