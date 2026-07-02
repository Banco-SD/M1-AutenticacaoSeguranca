package br.ufrpe.autenticacao.exception;

import br.ufrpe.autenticacao.dto.ErroResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponseDTO> tratarNaoEncontrado(RecursoNaoEncontradoException ex) {
        return construir(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<ErroResponseDTO> tratarRegraNegocio(RegraDeNegocioException ex) {
        return construir(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ResponseEntity<ErroResponseDTO> tratarCredenciais(CredenciaisInvalidasException ex) {
        return construir(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(ContaBloqueadaException.class)
    public ResponseEntity<ErroResponseDTO> tratarContaBloqueada(ContaBloqueadaException ex) {
        return construir(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(TokenInvalidoException.class)
    public ResponseEntity<ErroResponseDTO> tratarTokenInvalido(TokenInvalidoException ex) {
        return construir(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponseDTO> tratarValidacao(MethodArgumentNotValidException ex) {
        List<String> detalhes = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .toList();
        ErroResponseDTO erro = new ErroResponseDTO(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Dados inválidos",
                "Um ou mais campos não passaram na validação",
                detalhes
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erro);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponseDTO> tratarGenerico(Exception ex) {
        logger.error("Erro interno não tratado", ex);
        return construir(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno no servidor");
    }

    private ResponseEntity<ErroResponseDTO> construir(HttpStatus status, String mensagem) {
        ErroResponseDTO erro = new ErroResponseDTO(
                LocalDateTime.now(), status.value(), status.getReasonPhrase(), mensagem, null
        );
        return ResponseEntity.status(status).body(erro);
    }
}
