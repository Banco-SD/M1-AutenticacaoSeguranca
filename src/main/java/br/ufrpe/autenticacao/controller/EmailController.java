package br.ufrpe.autenticacao.controller;

import br.ufrpe.autenticacao.dto.MensagemResponseDTO;
import br.ufrpe.autenticacao.request.ConfirmarTokenRequest;
import br.ufrpe.autenticacao.request.TrocarEmailRequest;
import br.ufrpe.autenticacao.security.CustomUserDetails;
import br.ufrpe.autenticacao.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/usuarios/me/email")
public class EmailController {

    private final UsuarioService usuarioService;

    public EmailController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    public ResponseEntity<MensagemResponseDTO> solicitarTroca(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody TrocarEmailRequest request
    ) {
        usuarioService.solicitarTrocaEmail(principal.getUsuario().getId(), request);
        return ResponseEntity.ok(new MensagemResponseDTO("Confirme a troca através do link enviado ao novo email"));
    }

    @PostMapping("/confirmar")
    public ResponseEntity<MensagemResponseDTO> confirmarTroca(@Valid @RequestBody ConfirmarTokenRequest request) {
        usuarioService.confirmarTrocaEmail(request.getToken());
        return ResponseEntity.ok(new MensagemResponseDTO("Email atualizado com sucesso"));
    }
}
