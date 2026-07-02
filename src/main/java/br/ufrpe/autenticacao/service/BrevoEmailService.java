package br.ufrpe.autenticacao.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Envia email via API HTTP da Brevo (https://api.brevo.com) em vez de SMTP.
 *
 * Isso é proposital: muitos provedores de deploy gratuitos (Render, Railway,
 * etc.) bloqueiam as portas SMTP (25/465/587) na saída, mas nunca bloqueiam
 * HTTPS (443). Como a Brevo (e a maioria dos serviços modernos de email
 * transacional, como Resend, SendGrid, Mailgun) oferece API REST sobre
 * HTTPS, não existe esse problema.
 *
 * Free tier da Brevo: 300 emails/dia, sem cartão de crédito.
 * Docs: https://developers.brevo.com/reference/sendtransacemail
 */
@Service
public class BrevoEmailService implements EmailService {

    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${brevo.api-key}")
    private String apiKey;

    @Value("${brevo.remetente.email}")
    private String remetenteEmail;

    @Value("${brevo.remetente.nome:Sistema Bancário - Autenticação}")
    private String remetenteNome;

    @Override
    public void enviarEmailVerificacao(String destinatario, String nome, String linkVerificacao) {
        String assunto = "Confirme seu email";
        String corpo = "<p>Olá, " + nome + "!</p>"
                + "<p>Confirme seu email clicando no link abaixo:</p>"
                + "<p><a href=\"" + linkVerificacao + "\">Confirmar email</a></p>"
                + "<p>Se você não solicitou este cadastro, ignore este email.</p>";
        enviar(destinatario, nome, assunto, corpo);
    }

    @Override
    public void enviarEmailRecuperacaoSenha(String destinatario, String nome, String linkRecuperacao) {
        String assunto = "Recuperação de senha";
        String corpo = "<p>Olá, " + nome + "!</p>"
                + "<p>Recebemos uma solicitação para redefinir sua senha:</p>"
                + "<p><a href=\"" + linkRecuperacao + "\">Redefinir senha</a></p>"
                + "<p>Este link expira em breve. Se você não solicitou, ignore este email.</p>";
        enviar(destinatario, nome, assunto, corpo);
    }

    @Override
    public void enviarEmailConfirmacaoTrocaEmail(String destinatario, String nome, String linkConfirmacao) {
        String assunto = "Confirme seu novo email";
        String corpo = "<p>Olá, " + nome + "!</p>"
                + "<p>Confirme a troca do seu email clicando no link abaixo:</p>"
                + "<p><a href=\"" + linkConfirmacao + "\">Confirmar novo email</a></p>";
        enviar(destinatario, nome, assunto, corpo);
    }

    @Override
    public void enviarEmailAlertaBloqueio(String destinatario, String nome) {
        String assunto = "Sua conta foi bloqueada";
        String corpo = "<p>Olá, " + nome + "!</p>"
                + "<p>Detectamos várias tentativas de login inválidas e bloqueamos sua conta por segurança.</p>"
                + "<p>Use a recuperação de senha para desbloquear ou entre em contato com o suporte.</p>";
        enviar(destinatario, nome, assunto, corpo);
    }

    private void enviar(String destinatario, String nomeDestinatario, String assunto, String corpoHtml) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("api-key", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        Map<String, Object> corpo = Map.of(
                "sender", Map.of("email", remetenteEmail, "name", remetenteNome),
                "to", List.of(Map.of("email", destinatario, "name", nomeDestinatario)),
                "subject", assunto,
                "htmlContent", corpoHtml
        );

        HttpEntity<Map<String, Object>> requisicao = new HttpEntity<>(corpo, headers);
        restTemplate.postForEntity(BREVO_API_URL, requisicao, String.class);
    }
}
