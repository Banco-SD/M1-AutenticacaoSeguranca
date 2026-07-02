package br.ufrpe.autenticacao.service;

import br.ufrpe.autenticacao.dto.LoginResponseDTO;
import br.ufrpe.autenticacao.dto.SessaoResponseDTO;
import br.ufrpe.autenticacao.dto.UsuarioResponseDTO;
import br.ufrpe.autenticacao.exception.ContaBloqueadaException;
import br.ufrpe.autenticacao.exception.CredenciaisInvalidasException;
import br.ufrpe.autenticacao.exception.RecursoNaoEncontradoException;
import br.ufrpe.autenticacao.exception.TokenInvalidoException;
import br.ufrpe.autenticacao.model.SessaoUsuario;
import br.ufrpe.autenticacao.model.StatusUsuario;
import br.ufrpe.autenticacao.model.Usuario;
import br.ufrpe.autenticacao.repository.SessaoUsuarioRepository;
import br.ufrpe.autenticacao.repository.UsuarioRepository;
import br.ufrpe.autenticacao.security.JwtService;
import br.ufrpe.autenticacao.security.TokenHashUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AutenticacaoService {

    private static final int MAX_TENTATIVAS_LOGIN = 5;
    private static final long EXPIRACAO_REFRESH_TOKEN_DIAS = 30;

    private final UsuarioRepository usuarioRepository;
    private final SessaoUsuarioRepository sessaoUsuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenHashUtil tokenHashUtil;
    private final EmailService emailService;

    public AutenticacaoService(
            UsuarioRepository usuarioRepository,
            SessaoUsuarioRepository sessaoUsuarioRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            TokenHashUtil tokenHashUtil,
            EmailService emailService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.sessaoUsuarioRepository = sessaoUsuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tokenHashUtil = tokenHashUtil;
        this.emailService = emailService;
    }

    @Transactional
    public LoginResponseDTO login(String email, String senha, String ip, String userAgent) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new CredenciaisInvalidasException("Email ou senha inválidos"));

        if (usuario.getStatus() == StatusUsuario.BLOQUEADO) {
            throw new ContaBloqueadaException("Conta bloqueada por excesso de tentativas. Use a recuperação de senha.");
        }
        if (usuario.getStatus() == StatusUsuario.PENDENTE_VERIFICACAO) {
            throw new ContaBloqueadaException("Confirme seu email antes de fazer login.");
        }
        if (usuario.getStatus() == StatusUsuario.INATIVO) {
            throw new ContaBloqueadaException("Conta inativa.");
        }

        if (!passwordEncoder.matches(senha, usuario.getSenhaHash())) {
            registrarTentativaFalha(usuario);
            throw new CredenciaisInvalidasException("Email ou senha inválidos");
        }

        usuario.setTentativasLoginFalhas(0);
        usuario.setDataUltimoLogin(LocalDateTime.now());
        usuarioRepository.save(usuario);

        return gerarTokens(usuario, ip, userAgent, null);
    }

    @Transactional
    public LoginResponseDTO renovarToken(String refreshTokenBruto, String ip, String userAgent) {
        String hash = tokenHashUtil.hash(refreshTokenBruto);
        SessaoUsuario sessao = sessaoUsuarioRepository.findByRefreshTokenHash(hash)
                .orElseThrow(() -> new TokenInvalidoException("Refresh token inválido"));

        if (sessao.isRevogado()) {
            // Reuso de um refresh token que já havia sido trocado por outro:
            // forte indício de roubo de token. Por segurança, revoga TODAS
            // as sessões do usuário, forçando login novamente em todo lugar.
            revogarTodasAsSessoes(sessao.getUsuario().getId());
            throw new TokenInvalidoException("Refresh token já utilizado. Todas as sessões foram revogadas por segurança.");
        }
        if (sessao.getDataExpiracao().isBefore(LocalDateTime.now())) {
            throw new TokenInvalidoException("Refresh token expirado");
        }

        sessao.setRevogado(true);
        sessao.setDataRevogacao(LocalDateTime.now());
        sessaoUsuarioRepository.save(sessao);

        Usuario usuario = sessao.getUsuario();
        return gerarTokens(usuario, ip, userAgent, sessao.getId());
    }

    @Transactional
    public void logout(String refreshTokenBruto) {
        String hash = tokenHashUtil.hash(refreshTokenBruto);
        sessaoUsuarioRepository.findByRefreshTokenHash(hash).ifPresent(sessao -> {
            sessao.setRevogado(true);
            sessao.setDataRevogacao(LocalDateTime.now());
            sessaoUsuarioRepository.save(sessao);
        });
    }

    @Transactional
    public void revogarTodasAsSessoes(UUID usuarioId) {
        List<SessaoUsuario> sessoes = sessaoUsuarioRepository.findByUsuarioIdAndRevogadoFalse(usuarioId);
        LocalDateTime agora = LocalDateTime.now();
        sessoes.forEach(s -> {
            s.setRevogado(true);
            s.setDataRevogacao(agora);
        });
        sessaoUsuarioRepository.saveAll(sessoes);
    }

    @Transactional
    public void revogarSessao(UUID usuarioId, UUID sessaoId) {
        SessaoUsuario sessao = sessaoUsuarioRepository.findById(sessaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sessão não encontrada"));

        if (!sessao.getUsuario().getId().equals(usuarioId)) {
            throw new RecursoNaoEncontradoException("Sessão não encontrada");
        }

        sessao.setRevogado(true);
        sessao.setDataRevogacao(LocalDateTime.now());
        sessaoUsuarioRepository.save(sessao);
    }

    public List<SessaoResponseDTO> listarSessoes(UUID usuarioId) {
        return sessaoUsuarioRepository.findByUsuarioIdOrderByDataCriacaoDesc(usuarioId).stream()
                .map(s -> new SessaoResponseDTO(
                        s.getId(), s.getIp(), s.getUserAgent(), s.getDataCriacao(), s.getDataExpiracao(), !s.isRevogado()
                ))
                .toList();
    }

    private void registrarTentativaFalha(Usuario usuario) {
        usuario.setTentativasLoginFalhas(usuario.getTentativasLoginFalhas() + 1);

        if (usuario.getTentativasLoginFalhas() >= MAX_TENTATIVAS_LOGIN) {
            usuario.setStatus(StatusUsuario.BLOQUEADO);
            usuario.setDataBloqueio(LocalDateTime.now());
            emailService.enviarEmailAlertaBloqueio(usuario.getEmail(), usuario.getNome());
        }
        usuarioRepository.save(usuario);
    }

    private LoginResponseDTO gerarTokens(Usuario usuario, String ip, String userAgent, UUID sessaoAnteriorId) {
        String accessToken = jwtService.gerarAccessToken(usuario.getId(), usuario.getEmail(), usuario.getRole().name());

        String refreshTokenBruto = tokenHashUtil.gerarTokenAleatorio();
        SessaoUsuario sessao = new SessaoUsuario();
        sessao.setUsuario(usuario);
        sessao.setRefreshTokenHash(tokenHashUtil.hash(refreshTokenBruto));
        sessao.setIp(ip);
        sessao.setUserAgent(userAgent);
        sessao.setDataExpiracao(LocalDateTime.now().plusDays(EXPIRACAO_REFRESH_TOKEN_DIAS));
        sessao.setSessaoAnteriorId(sessaoAnteriorId);
        sessaoUsuarioRepository.save(sessao);

        return new LoginResponseDTO(
                accessToken,
                refreshTokenBruto,
                "Bearer",
                jwtService.getExpiracaoAccessTokenSegundos(),
                UsuarioResponseDTO.de(usuario)
        );
    }
}
