package DummyTalk.DummyTalk_BE.domain.repository.jpa;

import DummyTalk.DummyTalk_BE.domain.entity.Rarity;
import DummyTalk.DummyTalk_BE.domain.entity.constant.RarityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RarityRepository extends JpaRepository<Rarity, Long> {

    Optional<Rarity> findByName(String name);

    Optional<Rarity> findByName(RarityType name);

    @Query("select r from Rarity r where r.id in :idList")
    List<Rarity> findAllByIdList (@Param("idList") List<Long> idList);
}
