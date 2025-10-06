package DummyTalk.DummyTalk_BE.domain.repository;

import DummyTalk.DummyTalk_BE.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Integer> {

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM User u WHERE u.email = :email")
    void deleteByEmail(@Param("email") String email);

    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u JOIN FETCH Info i ON i.user.id = u.id WHERE u.email = :email")
    Optional<User>findByEmailFetchInfo(@Param("email") String email);

    Optional<User> findByEmailAndPassword(String email, String password);
}
