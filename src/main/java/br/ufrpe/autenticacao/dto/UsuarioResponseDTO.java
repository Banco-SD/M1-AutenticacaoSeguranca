package br.ufrpe.autenticacao.dto;

import br.ufrpe.autenticacao.model.Role;
import br.ufrpe.autenticacao.model.StatusUsuario;
import br.ufrpe.autenticacao.model.Usuario;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

// Nunca expõe senhaHash - isolamento da entidade, como pedido
@Getter
@AllArgsConstructor
public class UsuarioResponseDTO {

    private UUID id;
    private String nome;
    private String cpf;
    private String telefone;
    private LocalDate dataNascimento;
    private String email;
    private boolean emailVerificado;
    private StatusUsuario status;
    private Role role;
    private LocalDateTime dataCriacao;

    public static UsuarioResponseDTO de(Usuario usuario) {
        return new UsuarioResponseDTO(
                usuario.getId(),
                usuario.getNome(),
                usuario.getCpf(),
                usuario.getTelefone(),
                usuario.getDataNascimento(),
                usuario.getEmail(),
                usuario.isEmailVerificado(),
                usuario.getStatus(),
                usuario.getRole(),
                usuario.getDataCriacao()
        );
    }
}
