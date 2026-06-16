package pk.pn.pasir_parkovskyi_nikita.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import pk.pn.pasir_parkovskyi_nikita.model.Debt;

import java.util.List;

@Repository
public interface DebtRepository extends JpaRepository<Debt, Long> {

    List<Debt> findByGroupId(Long groupId);

    @Transactional
    void deleteByGroupId(Long groupId);
}
