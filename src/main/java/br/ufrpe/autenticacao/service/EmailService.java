package br.ufrpe.autenticacao.service;

public interface EmailService {
    void enviarEmailVerificacao(String destinatario, String nome, String linkVerificacao);
    void enviarEmailRecuperacaoSenha(String destinatario, String nome, String linkRecuperacao);
    void enviarEmailConfirmacaoTrocaEmail(String destinatario, String nome, String linkConfirmacao);
    void enviarEmailAlertaBloqueio(String destinatario, String nome);
}
