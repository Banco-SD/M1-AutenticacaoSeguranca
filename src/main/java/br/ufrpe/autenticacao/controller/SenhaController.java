package br.ufrpe.autenticacao.controller;

import br.ufrpe.autenticacao.dto.MensagemResponseDTO;
import br.ufrpe.autenticacao.request.EsqueciSenhaRequest;
import br.ufrpe.autenticacao.request.RedefinirSenhaRequest;
import br.ufrpe.autenticacao.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/senha")
public class SenhaController {

    private final UsuarioService usuarioService;

    public SenhaController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping("/esqueci")
    public ResponseEntity<MensagemResponseDTO> esqueciSenha(@Valid @RequestBody EsqueciSenhaRequest request) {
        usuarioService.solicitarRecuperacaoSenha(request.getEmail());
        return ResponseEntity.ok(new MensagemResponseDTO("Se o email existir, enviamos instruções de recuperação"));
    }

    @PostMapping("/redefinir")
    public ResponseEntity<MensagemResponseDTO> redefinirSenha(@Valid @RequestBody RedefinirSenhaRequest request) {
        usuarioService.redefinirSenha(request.getToken(), request.getNovaSenha());
        return ResponseEntity.ok(new MensagemResponseDTO("Senha redefinida com sucesso"));
    }
}
