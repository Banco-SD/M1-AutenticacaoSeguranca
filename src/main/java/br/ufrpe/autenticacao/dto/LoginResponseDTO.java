package br.ufrpe.autenticacao.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponseDTO {

    private String accessToken;
    private String refreshToken;
    private String tipoToken;
    private long expiraEmSegundos;
    private UsuarioResponseDTO usuario;
}
