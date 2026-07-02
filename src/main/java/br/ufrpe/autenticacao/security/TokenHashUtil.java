package br.ufrpe.autenticacao.security;

import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Gera valores aleatórios criptograficamente seguros (para refresh tokens
 * e tokens de verificação/recuperação enviados por email) e calcula o hash
 * SHA-256 usado para armazenar esses valores no banco.
 * Nunca armazenamos o token "em claro" - só o hash, igual se faz com senha.
 */
@Component
public class TokenHashUtil {

    private final SecureRandom secureRandom = new SecureRandom();

    public String gerarTokenAleatorio() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String hash(String valor) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(valor.getBytes());
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo de hash indisponível", e);
        }
    }
}
