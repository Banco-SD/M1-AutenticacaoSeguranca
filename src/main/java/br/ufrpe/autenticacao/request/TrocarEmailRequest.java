package br.ufrpe.autenticacao.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TrocarEmailRequest {

    @NotBlank
    @Email(message = "Email inválido")
    private String novoEmail;

    @NotBlank(message = "Senha atual é obrigatória para confirmar a troca")
    private String senhaAtual;
}
