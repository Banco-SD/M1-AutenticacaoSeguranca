package br.ufrpe.autenticacao.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class SessaoResponseDTO {

    private UUID id;
    private String ip;
    private String userAgent;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataExpiracao;
    private boolean sessaoAtiva;
}
