package br.ufrpe.autenticacao.controller;

import br.ufrpe.autenticacao.dto.LoginResponseDTO;
import br.ufrpe.autenticacao.dto.MensagemResponseDTO;
import br.ufrpe.autenticacao.request.LoginRequest;
import br.ufrpe.autenticacao.request.RefreshTokenRequest;
import br.ufrpe.autenticacao.service.AutenticacaoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AutenticacaoController {

    private final AutenticacaoService autenticacaoService;

    public AutenticacaoController(AutenticacaoService autenticacaoService) {
        this.autenticacaoService = autenticacaoService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        LoginResponseDTO resposta = autenticacaoService.login(
                request.getEmail(), request.getSenha(), extrairIp(httpRequest), httpRequest.getHeader("User-Agent")
        );
        return ResponseEntity.ok(resposta);
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponseDTO> refresh(@Valid @RequestBody RefreshTokenRequest request, HttpServletRequest httpRequest) {
        LoginResponseDTO resposta = autenticacaoService.renovarToken(
                request.getRefreshToken(), extrairIp(httpRequest), httpRequest.getHeader("User-Agent")
        );
        return ResponseEntity.ok(resposta);
    }

    @PostMapping("/logout")
    public ResponseEntity<MensagemResponseDTO> logout(@Valid @RequestBody RefreshTokenRequest request) {
        autenticacaoService.logout(request.getRefreshToken());
        return ResponseEntity.ok(new MensagemResponseDTO("Logout realizado com sucesso"));
    }

    private String extrairIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return (forwarded != null && !forwarded.isBlank()) ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
    }
}
