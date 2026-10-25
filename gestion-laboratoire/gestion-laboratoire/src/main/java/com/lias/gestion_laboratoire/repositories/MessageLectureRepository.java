package com.lias.gestion_laboratoire.repositories;

import com.lias.gestion_laboratoire.models.MessageLecture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/**
 * Repository pour le suivi de lecture des messages par membre.
 */
@Repository
public interface MessageLectureRepository extends JpaRepository<MessageLecture, Long> {

    boolean existsByMessage_IdAndLecteur_Id(Long messageId, Long lecteurId);

    Optional<MessageLecture> findByMessage_IdAndLecteur_Id(Long messageId, Long lecteurId);

    @Query("SELECT ml.message.id FROM MessageLecture ml WHERE ml.lecteur.id = :lecteurId AND ml.message.id IN :messageIds")
    Set<Long> findReadMessageIds(@Param("lecteurId") Long lecteurId, @Param("messageIds") Collection<Long> messageIds);

    void deleteByMessage_Id(Long messageId);
}
