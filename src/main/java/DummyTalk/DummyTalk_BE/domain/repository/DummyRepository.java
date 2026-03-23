package DummyTalk.DummyTalk_BE.domain.repository;

import DummyTalk.DummyTalk_BE.domain.entity.Dummy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.Set;

public interface DummyRepository extends JpaRepository<Dummy, Long> {

    @Query(value = "select d from Dummy d join fetch d.rarity where d.id = :dummyId")
    Optional<Dummy> findByIdWithRarity (Long dummyId);

    @Query("select d.title from Dummy d")
    Set<String> findAllTitles(); // DataLoader 전용

    boolean existsDummiesByTitle(String title);

    boolean existsByTitle(String title);
}
