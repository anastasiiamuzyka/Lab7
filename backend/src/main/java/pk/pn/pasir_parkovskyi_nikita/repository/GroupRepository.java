package pk.pn.pasir_parkovskyi_nikita.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pk.pn.pasir_parkovskyi_nikita.model.Group;
import pk.pn.pasir_parkovskyi_nikita.model.User;

import java.util.List;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {

    List<Group> findByMemberships_User(User user);
}
