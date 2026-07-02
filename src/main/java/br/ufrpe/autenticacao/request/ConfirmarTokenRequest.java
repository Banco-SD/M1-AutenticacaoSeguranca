package br.ufrpe.autenticacao.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConfirmarTokenRequest {

    @NotBlank(message = "Token é obrigatório")
    private String token;
}
