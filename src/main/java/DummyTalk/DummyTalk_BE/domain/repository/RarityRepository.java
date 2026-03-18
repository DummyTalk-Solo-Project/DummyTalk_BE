package DummyTalk.DummyTalk_BE.domain.repository;

import DummyTalk.DummyTalk_BE.domain.entity.Rarity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RarityRepository extends JpaRepository<Rarity, Long> {
    Optional<Rarity> findByName(String name);

}
