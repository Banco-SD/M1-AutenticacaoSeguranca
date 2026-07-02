package br.ufrpe.autenticacao.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.validator.constraints.br.CPF;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "usuario", uniqueConstraints = {
        @UniqueConstraint(name = "uk_usuario_cpf", columnNames = "cpf"),
        @UniqueConstraint(name = "uk_usuario_email", columnNames = "email"),
        @UniqueConstraint(name = "uk_usuario_telefone", columnNames = "telefone")
})
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 3, max = 150, message = "Nome deve ter entre 3 e 150 caracteres")
    @Column(nullable = false, length = 150)
    private String nome;

    @NotBlank(message = "CPF é obrigatório")
    @CPF(message = "CPF inválido")
    @Column(nullable = false, length = 14, updatable = false)
    private String cpf;

    @NotBlank(message = "Telefone é obrigatório")
    @Pattern(regexp = "^\\d{10,11}$", message = "Telefone deve conter DDD + número (10 ou 11 dígitos)")
    @Column(nullable = false, length = 11)
    private String telefone;

    @NotNull(message = "Data de nascimento é obrigatória")
    @Past(message = "Data de nascimento deve estar no passado")
    @Column(nullable = false)
    private LocalDate dataNascimento;

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email inválido")
    @Column(nullable = false, length = 180)
    private String email;

    //Nunca retornar no JSON, mesmo por engano (defesa extra além do DTO)
    @JsonIgnore
    @Column(nullable = false)
    private String senhaHash;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusUsuario status = StatusUsuario.PENDENTE_VERIFICACAO;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.CLIENTE;

    @Column(nullable = false)
    private boolean emailVerificado = false;

    @Column(nullable = false)
    private int tentativasLoginFalhas = 0;

    private LocalDateTime dataUltimoLogin;

    private LocalDateTime dataBloqueio;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime dataAtualizacao;

    //Lock otimista: evita condições de corrida em updates concorrentes
    //(ex.: duas requisições de troca de senha ao mesmo tempo)
    @Version
    private Long versao;
}
