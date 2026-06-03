package com.fpt.swp.repository;

import com.fpt.swp.model.PaperNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaperNoteRepository extends JpaRepository<PaperNote, Long> {
    Optional<PaperNote> findByUserIdAndPaperExternalId(Long userId, String paperExternalId);
    List<PaperNote> findByUserId(Long userId);
}
