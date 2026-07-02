package br.ufrpe.autenticacao.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

/**
 * Gera e valida SOMENTE o access token (JWT de curta duração, ex.: 15 min).
 * O refresh token NÃO é um JWT: é um valor aleatório opaco, armazenado com
 * hash no banco (ver SessaoUsuario) — assim é possível revogá-lo a qualquer
 * momento, o que não é trivial com um JWT auto-contido.
 */
@Component
public class JwtService {

    private final SecretKey chave;
    private final long expiracaoAccessTokenMs;

    public JwtService(
            @Value("${jwt.secret}") String segredo,
            @Value("${jwt.access-token.expiracao-ms:900000}") long expiracaoAccessTokenMs
    ) {
        this.chave = Keys.hmacShaKeyFor(segredo.getBytes());
        this.expiracaoAccessTokenMs = expiracaoAccessTokenMs;
    }

    public String gerarAccessToken(UUID usuarioId, String email, String role) {
        Date agora = new Date();
        Date expiracao = new Date(agora.getTime() + expiracaoAccessTokenMs);

        return Jwts.builder()
                .subject(usuarioId.toString())
                .claim("email", email)
                .claim("role", role)
                .issuedAt(agora)
                .expiration(expiracao)
                .signWith(chave)
                .compact();
    }

    public long getExpiracaoAccessTokenSegundos() {
        return expiracaoAccessTokenMs / 1000;
    }

    public Claims validarEExtrairClaims(String token) {
        return Jwts.parser()
                .verifyWith(chave)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UUID extrairUsuarioId(String token) {
        return UUID.fromString(validarEExtrairClaims(token).getSubject());
    }
}
