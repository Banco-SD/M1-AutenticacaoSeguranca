package br.ufrpe.autenticacao.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade genérica para tokens de uso único enviados por email:
 * verificação de cadastro, recuperação de senha e confirmação de troca de email.
 * Evita duplicar 3 classes quase idênticas.
 */
@Getter
@Setter
@Entity
@Table(name = "token_verificacao", indexes = {
        @Index(name = "idx_token_hash", columnList = "tokenHash")
})
public class TokenVerificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    // Só o hash é armazenado; o token bruto vai por email e nunca é persistido
    @Column(nullable = false, unique = true, length = 128)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoToken tipo;

    /**
     * Usado apenas quando tipo = TROCA_EMAIL: guarda o novo email
     * até a confirmação via token, sem alterar o email do usuário antes disso.
     */
    @Column(length = 180)
    private String novoEmail;

    @Column(nullable = false)
    private LocalDateTime dataCriacao = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime dataExpiracao;

    @Column(nullable = false)
    private boolean usado = false;

    private LocalDateTime dataUso;
}
