package br.ufrpe.autenticacao.repository;

import br.ufrpe.autenticacao.model.SessaoUsuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessaoUsuarioRepository extends JpaRepository<SessaoUsuario, UUID> {

    Optional<SessaoUsuario> findByRefreshTokenHash(String refreshTokenHash);

    List<SessaoUsuario> findByUsuarioIdAndRevogadoFalse(UUID usuarioId);

    List<SessaoUsuario> findByUsuarioIdOrderByDataCriacaoDesc(UUID usuarioId);
}
