package com.lias.gestion_laboratoire.repositories;

import com.lias.gestion_laboratoire.models.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    List<PasswordResetToken> findByMembre_IdAndRevoqueLeIsNullAndUtiliseLeIsNull(Long membreId);

    Optional<PasswordResetToken> findFirstByMembre_IdOrderByDateCreationDesc(Long membreId);
}
