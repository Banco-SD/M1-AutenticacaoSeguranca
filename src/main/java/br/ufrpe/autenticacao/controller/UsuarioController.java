package br.ufrpe.autenticacao.controller;

import br.ufrpe.autenticacao.dto.MensagemResponseDTO;
import br.ufrpe.autenticacao.dto.SessaoResponseDTO;
import br.ufrpe.autenticacao.dto.UsuarioResponseDTO;
import br.ufrpe.autenticacao.request.AlterarSenhaRequest;
import br.ufrpe.autenticacao.request.CadastroUsuarioRequest;
import br.ufrpe.autenticacao.request.ConfirmarTokenRequest;
import br.ufrpe.autenticacao.security.CustomUserDetails;
import br.ufrpe.autenticacao.service.AutenticacaoService;
import br.ufrpe.autenticacao.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final AutenticacaoService autenticacaoService;

    public UsuarioController(UsuarioService usuarioService, AutenticacaoService autenticacaoService) {
        this.usuarioService = usuarioService;
        this.autenticacaoService = autenticacaoService;
    }

    @PostMapping
    public ResponseEntity<UsuarioResponseDTO> cadastrar(@Valid @RequestBody CadastroUsuarioRequest request) {
        UsuarioResponseDTO resposta = usuarioService.cadastrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(usuarioService.buscarPorId(id));
    }

    @GetMapping("/me")
    public ResponseEntity<UsuarioResponseDTO> meuPerfil(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(usuarioService.buscarPorId(principal.getUsuario().getId()));
    }

    @PatchMapping("/me/senha")
    public ResponseEntity<MensagemResponseDTO> alterarSenha(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody AlterarSenhaRequest request
    ) {
        usuarioService.alterarSenha(principal.getUsuario().getId(), request.getSenhaAtual(), request.getNovaSenha());
        return ResponseEntity.ok(new MensagemResponseDTO("Senha alterada com sucesso"));
    }

    @GetMapping("/me/sessoes")
    public ResponseEntity<List<SessaoResponseDTO>> minhasSessoes(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(autenticacaoService.listarSessoes(principal.getUsuario().getId()));
    }

    @DeleteMapping("/me/sessoes/{sessaoId}")
    public ResponseEntity<MensagemResponseDTO> revogarSessao(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID sessaoId
    ) {
        autenticacaoService.revogarSessao(principal.getUsuario().getId(), sessaoId);
        return ResponseEntity.ok(new MensagemResponseDTO("Sessão revogada com sucesso"));
    }

    @DeleteMapping("/me/sessoes")
    public ResponseEntity<MensagemResponseDTO> revogarTodasSessoes(@AuthenticationPrincipal CustomUserDetails principal) {
        autenticacaoService.revogarTodasAsSessoes(principal.getUsuario().getId());
        return ResponseEntity.ok(new MensagemResponseDTO("Todas as sessões foram revogadas"));
    }

    @PostMapping("/verificar-email")
    public ResponseEntity<MensagemResponseDTO> verificarEmail(@Valid @RequestBody ConfirmarTokenRequest request) {
        usuarioService.confirmarVerificacaoEmail(request.getToken());
        return ResponseEntity.ok(new MensagemResponseDTO("Email verificado com sucesso"));
    }

    @PostMapping("/reenviar-verificacao")
    public ResponseEntity<MensagemResponseDTO> reenviarVerificacao(@RequestParam String email) {
        usuarioService.reenviarVerificacaoEmail(email);
        return ResponseEntity.ok(new MensagemResponseDTO("Se o email existir, um novo link de verificação foi enviado"));
    }
}
