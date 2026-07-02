package br.ufrpe.autenticacao.service;

import br.ufrpe.autenticacao.dto.UsuarioResponseDTO;
import br.ufrpe.autenticacao.exception.CredenciaisInvalidasException;
import br.ufrpe.autenticacao.exception.RecursoNaoEncontradoException;
import br.ufrpe.autenticacao.exception.RegraDeNegocioException;
import br.ufrpe.autenticacao.exception.TokenInvalidoException;
import br.ufrpe.autenticacao.model.*;
import br.ufrpe.autenticacao.repository.TokenVerificacaoRepository;
import br.ufrpe.autenticacao.repository.UsuarioRepository;
import br.ufrpe.autenticacao.request.CadastroUsuarioRequest;
import br.ufrpe.autenticacao.request.TrocarEmailRequest;
import br.ufrpe.autenticacao.security.TokenHashUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class UsuarioService {

    private static final long EXPIRACAO_TOKEN_VERIFICACAO_HORAS = 24;
    private static final long EXPIRACAO_TOKEN_RECUPERACAO_MINUTOS = 30;

    private final UsuarioRepository usuarioRepository;
    private final TokenVerificacaoRepository tokenVerificacaoRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenHashUtil tokenHashUtil;
    private final EmailService emailService;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            TokenVerificacaoRepository tokenVerificacaoRepository,
            PasswordEncoder passwordEncoder,
            TokenHashUtil tokenHashUtil,
            EmailService emailService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.tokenVerificacaoRepository = tokenVerificacaoRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenHashUtil = tokenHashUtil;
        this.emailService = emailService;
    }

    @Transactional
    public UsuarioResponseDTO cadastrar(CadastroUsuarioRequest request) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new RegraDeNegocioException("Já existe um usuário cadastrado com este email");
        }
        if (usuarioRepository.existsByCpf(request.getCpf())) {
            throw new RegraDeNegocioException("Já existe um usuário cadastrado com este CPF");
        }
        if (usuarioRepository.existsByTelefone(request.getTelefone())) {
            throw new RegraDeNegocioException("Já existe um usuário cadastrado com este telefone");
        }

        Usuario usuario = new Usuario();
        usuario.setNome(request.getNome());
        usuario.setCpf(request.getCpf());
        usuario.setTelefone(request.getTelefone());
        usuario.setDataNascimento(request.getDataNascimento());
        usuario.setEmail(request.getEmail());
        usuario.setSenhaHash(passwordEncoder.encode(request.getSenha()));
        usuario.setStatus(StatusUsuario.PENDENTE_VERIFICACAO);
        usuario.setRole(Role.CLIENTE);

        usuario = usuarioRepository.save(usuario);

        criarEEnviarTokenVerificacaoEmail(usuario);

        return UsuarioResponseDTO.de(usuario);
    }

    @Transactional
    public void reenviarVerificacaoEmail(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));

        if (usuario.isEmailVerificado()) {
            throw new RegraDeNegocioException("Email já verificado");
        }

        tokenVerificacaoRepository.deleteByUsuarioIdAndTipo(usuario.getId(), TipoToken.VERIFICACAO_EMAIL);
        criarEEnviarTokenVerificacaoEmail(usuario);
    }

    @Transactional
    public void confirmarVerificacaoEmail(String tokenBruto) {
        TokenVerificacao token = buscarTokenValido(tokenBruto, TipoToken.VERIFICACAO_EMAIL);

        Usuario usuario = token.getUsuario();
        usuario.setEmailVerificado(true);
        usuario.setStatus(StatusUsuario.ATIVO);
        usuarioRepository.save(usuario);

        marcarTokenComoUsado(token);
    }

    @Transactional
    public void solicitarRecuperacaoSenha(String email) {
        // Não lança exceção se o email não existir, para não expor quais emails
        // estão cadastrados na base (evita enumeração de contas)
        usuarioRepository.findByEmail(email).ifPresent(usuario -> {
            tokenVerificacaoRepository.deleteByUsuarioIdAndTipo(usuario.getId(), TipoToken.RECUPERACAO_SENHA);

            String tokenBruto = tokenHashUtil.gerarTokenAleatorio();
            TokenVerificacao token = new TokenVerificacao();
            token.setUsuario(usuario);
            token.setTipo(TipoToken.RECUPERACAO_SENHA);
            token.setTokenHash(tokenHashUtil.hash(tokenBruto));
            token.setDataExpiracao(LocalDateTime.now().plusMinutes(EXPIRACAO_TOKEN_RECUPERACAO_MINUTOS));
            tokenVerificacaoRepository.save(token);

            String link = "https://SEU-FRONTEND/redefinir-senha?token=" + tokenBruto;
            emailService.enviarEmailRecuperacaoSenha(usuario.getEmail(), usuario.getNome(), link);
        });
    }

    @Transactional
    public void redefinirSenha(String tokenBruto, String novaSenha) {
        TokenVerificacao token = buscarTokenValido(tokenBruto, TipoToken.RECUPERACAO_SENHA);

        Usuario usuario = token.getUsuario();
        usuario.setSenhaHash(passwordEncoder.encode(novaSenha));
        usuario.setTentativasLoginFalhas(0);
        if (usuario.getStatus() == StatusUsuario.BLOQUEADO) {
            usuario.setStatus(StatusUsuario.ATIVO);
        }
        usuarioRepository.save(usuario);

        marcarTokenComoUsado(token);
    }

    @Transactional
    public void alterarSenha(UUID usuarioId, String senhaAtual, String novaSenha) {
        Usuario usuario = buscarEntidadePorId(usuarioId);

        if (!passwordEncoder.matches(senhaAtual, usuario.getSenhaHash())) {
            throw new CredenciaisInvalidasException("Senha atual incorreta");
        }

        usuario.setSenhaHash(passwordEncoder.encode(novaSenha));
        usuarioRepository.save(usuario);
    }

    @Transactional
    public void solicitarTrocaEmail(UUID usuarioId, TrocarEmailRequest request) {
        Usuario usuario = buscarEntidadePorId(usuarioId);

        if (!passwordEncoder.matches(request.getSenhaAtual(), usuario.getSenhaHash())) {
            throw new CredenciaisInvalidasException("Senha incorreta");
        }
        if (usuarioRepository.existsByEmail(request.getNovoEmail())) {
            throw new RegraDeNegocioException("Este email já está em uso");
        }

        tokenVerificacaoRepository.deleteByUsuarioIdAndTipo(usuario.getId(), TipoToken.TROCA_EMAIL);

        String tokenBruto = tokenHashUtil.gerarTokenAleatorio();
        TokenVerificacao token = new TokenVerificacao();
        token.setUsuario(usuario);
        token.setTipo(TipoToken.TROCA_EMAIL);
        token.setTokenHash(tokenHashUtil.hash(tokenBruto));
        token.setNovoEmail(request.getNovoEmail());
        token.setDataExpiracao(LocalDateTime.now().plusHours(EXPIRACAO_TOKEN_VERIFICACAO_HORAS));
        tokenVerificacaoRepository.save(token);

        // O link vai para o NOVO email, garantindo que o dono realmente tem acesso a ele
        String link = "https://SEU-FRONTEND/confirmar-novo-email?token=" + tokenBruto;
        emailService.enviarEmailConfirmacaoTrocaEmail(request.getNovoEmail(), usuario.getNome(), link);
    }

    @Transactional
    public void confirmarTrocaEmail(String tokenBruto) {
        TokenVerificacao token = buscarTokenValido(tokenBruto, TipoToken.TROCA_EMAIL);

        if (usuarioRepository.existsByEmail(token.getNovoEmail())) {
            throw new RegraDeNegocioException("Este email já está em uso");
        }

        Usuario usuario = token.getUsuario();
        usuario.setEmail(token.getNovoEmail());
        usuarioRepository.save(usuario);

        marcarTokenComoUsado(token);
    }

    public UsuarioResponseDTO buscarPorId(UUID id) {
        return UsuarioResponseDTO.de(buscarEntidadePorId(id));
    }

    public Usuario buscarEntidadePorId(UUID id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
    }

    private void criarEEnviarTokenVerificacaoEmail(Usuario usuario) {
        String tokenBruto = tokenHashUtil.gerarTokenAleatorio();
        TokenVerificacao token = new TokenVerificacao();
        token.setUsuario(usuario);
        token.setTipo(TipoToken.VERIFICACAO_EMAIL);
        token.setTokenHash(tokenHashUtil.hash(tokenBruto));
        token.setDataExpiracao(LocalDateTime.now().plusHours(EXPIRACAO_TOKEN_VERIFICACAO_HORAS));
        tokenVerificacaoRepository.save(token);

        String link = "https://SEU-FRONTEND/verificar-email?token=" + tokenBruto;
        emailService.enviarEmailVerificacao(usuario.getEmail(), usuario.getNome(), link);
    }

    private TokenVerificacao buscarTokenValido(String tokenBruto, TipoToken tipo) {
        String hash = tokenHashUtil.hash(tokenBruto);
        TokenVerificacao token = tokenVerificacaoRepository.findByTokenHashAndTipo(hash, tipo)
                .orElseThrow(() -> new TokenInvalidoException("Token inválido"));

        if (token.isUsado()) {
            throw new TokenInvalidoException("Token já utilizado");
        }
        if (token.getDataExpiracao().isBefore(LocalDateTime.now())) {
            throw new TokenInvalidoException("Token expirado");
        }
        return token;
    }

    private void marcarTokenComoUsado(TokenVerificacao token) {
        token.setUsado(true);
        token.setDataUso(LocalDateTime.now());
        tokenVerificacaoRepository.save(token);
    }
}
