package br.ufrpe.autenticacao.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EsqueciSenhaRequest {

    @NotBlank
    @Email(message = "Email inválido")
    private String email;
}
