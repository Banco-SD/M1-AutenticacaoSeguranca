package br.ufrpe.autenticacao.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Representa uma sessão de login (equivalente a um refresh token).
 * Guarda de onde e quando o usuário logou, e permite revogar sessões
 * individualmente (ex.: "sair deste dispositivo") ou todas de uma vez.
 */
@Getter
@Setter
@Entity
@Table(name = "sessao_usuario", indexes = {
        @Index(name = "idx_sessao_refresh_token_hash", columnList = "refreshTokenHash"),
        @Index(name = "idx_sessao_usuario", columnList = "usuario_id")
})
public class SessaoUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    // Nunca armazenamos o refresh token em texto puro, só o hash (SHA-256)
    @Column(nullable = false, unique = true, length = 128)
    private String refreshTokenHash;

    @Column(length = 45)
    private String ip;

    @Column(length = 255)
    private String userAgent;

    @Column(nullable = false)
    private LocalDateTime dataCriacao = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime dataExpiracao;

    private LocalDateTime dataRevogacao;

    @Column(nullable = false)
    private boolean revogado = false;

    /**
     * Referencia a sessão anterior desta "cadeia" de refresh tokens.
     * Toda vez que o /auth/refresh é chamado, o token antigo é revogado
     * e um novo é criado apontando pra este campo — assim dá pra
     * reconstruir quando e quantas vezes o token foi renovado, e detectar
     * reuso indevido de um refresh token já trocado (possível roubo de token).
     */
    private UUID sessaoAnteriorId;
}
