package br.ufrpe.autenticacao.repository;

import br.ufrpe.autenticacao.model.TipoToken;
import br.ufrpe.autenticacao.model.TokenVerificacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TokenVerificacaoRepository extends JpaRepository<TokenVerificacao, UUID> {

    Optional<TokenVerificacao> findByTokenHashAndTipo(String tokenHash, TipoToken tipo);

    void deleteByUsuarioIdAndTipo(UUID usuarioId, TipoToken tipo);
}
