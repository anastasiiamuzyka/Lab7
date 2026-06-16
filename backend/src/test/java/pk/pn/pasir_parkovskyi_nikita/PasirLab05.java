package pk.pn.pasir_parkovskyi_nikita;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import pk.pn.pasir_parkovskyi_nikita.dto.DebtDTO;
import pk.pn.pasir_parkovskyi_nikita.dto.GroupDTO;
import pk.pn.pasir_parkovskyi_nikita.dto.GroupTransactionDTO;
import pk.pn.pasir_parkovskyi_nikita.dto.MembershipDTO;
import pk.pn.pasir_parkovskyi_nikita.model.Debt;
import pk.pn.pasir_parkovskyi_nikita.model.Group;
import pk.pn.pasir_parkovskyi_nikita.model.Membership;
import pk.pn.pasir_parkovskyi_nikita.model.User;
import pk.pn.pasir_parkovskyi_nikita.repository.DebtRepository;
import pk.pn.pasir_parkovskyi_nikita.repository.GroupRepository;
import pk.pn.pasir_parkovskyi_nikita.repository.MembershipRepository;
import pk.pn.pasir_parkovskyi_nikita.repository.UserRepository;
import pk.pn.pasir_parkovskyi_nikita.service.DebtService;
import pk.pn.pasir_parkovskyi_nikita.service.GroupService;
import pk.pn.pasir_parkovskyi_nikita.service.GroupTransactionService;
import pk.pn.pasir_parkovskyi_nikita.service.MembershipService;

/**
 * PasirLab05 – testy automatyczne dla funkcjonalności grup, długów i członkostw.
 * Uruchamiane na bazie H2 in-memory (profil "test").
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@SuppressWarnings({"java:S3577", "SpringJavaInjectionPointsAutowiringInspection", "SpellCheckingInspection"})
class PasirLab05 {

    private static final String EXPENSE_TYPE = "EXPENSE";

    @Autowired private GroupService groupService;
    @Autowired private MembershipService membershipService;
    @Autowired private DebtService debtService;
    @Autowired private GroupTransactionService groupTransactionService;
    @Autowired private UserRepository userRepository;
    @Autowired private GroupRepository groupRepository;
    @Autowired private MembershipRepository membershipRepository;
    @Autowired private DebtRepository debtRepository;
    @Autowired private jakarta.validation.Validator validator;

    private User owner;
    private User member;
    private User outsider;

    // -------------------------------------------------------------------------
    // Setup helpers
    // -------------------------------------------------------------------------

    private User createUser(String username, String email) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(email);
        u.setPassword("hashedPass");
        return userRepository.save(u);
    }

    private void loginAs(User user) {
        var auth = new UsernamePasswordAuthenticationToken(
                user.getEmail(), null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        owner    = createUser("owner",    "owner@test.com");
        member   = createUser("member",   "member@test.com");
        outsider = createUser("outsider", "outsider@test.com");
    }

    // -------------------------------------------------------------------------
    // 1. Utworzenie grupy dodaje właściciela jako członka i zwraca ją w myGroups
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("1. Utworzenie grupy dodaje właściciela jako członka i zwraca ją w myGroups")
    void createGroupAddsOwnerAsMemberAndAppearsInMyGroups() {
        loginAs(owner);
        GroupDTO dto = new GroupDTO();
        dto.setName("Mieszkanie");
        Group group = groupService.createGroup(dto);

        // właściciel musi być członkiem
        boolean ownerIsMember = membershipRepository
                .existsByGroupIdAndUserId(group.getId(), owner.getId());
        assertThat(ownerIsMember).isTrue();

        // grupa pojawia się w myGroups (getAllGroups zwraca grupy zalogowanego)
        List<Group> myGroups = groupService.getAllGroups();
        assertThat(myGroups).extracting(Group::getId).contains(group.getId());
    }

    // -------------------------------------------------------------------------
    // 2. Tylko właściciel grupy może dodawać członków
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("2. Tylko właściciel grupy może dodawać członków")
    void addMemberOnlyOwnerCanAdd() {
        loginAs(owner);
        GroupDTO dto = new GroupDTO();
        dto.setName("Wakacje");
        Group group = groupService.createGroup(dto);

        // zwykły member próbuje dodać kogoś
        // najpierw dodajemy go do grupy
        MembershipDTO addDto = new MembershipDTO();
        addDto.setGroupId(group.getId());
        addDto.setUserEmail(member.getEmail());
        membershipService.addMember(addDto);

        // member loguje się i próbuje dodać outsidera
        loginAs(member);
        MembershipDTO illegalDto = new MembershipDTO();
        illegalDto.setGroupId(group.getId());
        illegalDto.setUserEmail(outsider.getEmail());

        assertThatThrownBy(() -> membershipService.addMember(illegalDto))
                .isInstanceOf(AccessDeniedException.class);
    }

    // -------------------------------------------------------------------------
    // 3. groupMembers zwraca członków grupy tylko członkowi tej grupy
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("3. groupMembers zwraca członków grupy tylko członkowi tej grupy")
    void groupMembersOnlyVisibleToMember() {
        loginAs(owner);
        GroupDTO dto = new GroupDTO();
        dto.setName("Sklep");
        Group group = groupService.createGroup(dto);

        // outsider nie jest członkiem – powinien dostać AccessDeniedException
        loginAs(outsider);
        assertThatThrownBy(() -> membershipService.getGroupMembers(group.getId()))
                .isInstanceOf(AccessDeniedException.class);

        // owner jest członkiem – powinien dostać listę
        loginAs(owner);
        List<Membership> members = membershipService.getGroupMembers(group.getId());
        assertThat(members).isNotEmpty();
    }

    // -------------------------------------------------------------------------
    // 4. groupDebts zwraca długi grupy tylko członkowi tej grupy
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("4. groupDebts zwraca długi grupy tylko członkowi tej grupy")
    void groupDebtsOnlyVisibleToMember() {
        loginAs(owner);
        GroupDTO dto = new GroupDTO();
        dto.setName("Podróż");
        Group group = groupService.createGroup(dto);

        // outsider nie jest członkiem
        loginAs(outsider);
        assertThatThrownBy(() -> debtService.getGroupDebts(group.getId()))
                .isInstanceOf(AccessDeniedException.class);

        // owner jest członkiem – dostaje pustą listę (brak długów)
        loginAs(owner);
        List<Debt> debts = debtService.getGroupDebts(group.getId());
        assertThat(debts).isEmpty();
    }

    // -------------------------------------------------------------------------
    // 5. Nowy członek dostaje tylko długi z transakcji dodanych po dołączeniu
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("5. Nowy członek dostaje tylko długi z transakcji dodanych po dołączeniu")
    void newMemberSeesOnlyDebtsAfterJoining() {
        loginAs(owner);
        GroupDTO dto = new GroupDTO();
        dto.setName("Dom");
        Group group = groupService.createGroup(dto);

        // transakcja PRZED dołączeniem member – tylko owner w grupie
        GroupTransactionDTO txBefore = new GroupTransactionDTO();
        txBefore.setGroupId(group.getId());
        txBefore.setAmount(100.0);
        txBefore.setType(EXPENSE_TYPE);
        txBefore.setTitle("Rachunek przed");
        groupTransactionService.addGroupTransaction(txBefore, owner);

        long debtsBeforeJoin = debtRepository.findByGroupId(group.getId()).size();

        // dodaj member do grupy
        MembershipDTO mDto = new MembershipDTO();
        mDto.setGroupId(group.getId());
        mDto.setUserEmail(member.getEmail());
        membershipService.addMember(mDto);

        // transakcja PO dołączeniu member
        GroupTransactionDTO txAfter = new GroupTransactionDTO();
        txAfter.setGroupId(group.getId());
        txAfter.setAmount(60.0);
        txAfter.setType(EXPENSE_TYPE);
        txAfter.setTitle("Rachunek po");
        groupTransactionService.addGroupTransaction(txAfter, owner);

        long debtsAfterJoin = debtRepository.findByGroupId(group.getId()).size();

        // po dołączeniu member pojawiły się nowe długi (więcej niż przed)
        assertThat(debtsAfterJoin).isGreaterThan(debtsBeforeJoin);

        // member widzi długi z transakcji po dołączeniu
        loginAs(member);
        List<Debt> memberDebts = debtService.getGroupDebts(group.getId());
        assertThat(memberDebts).anyMatch(d -> "Rachunek po".equals(d.getTitle()));
    }

    // -------------------------------------------------------------------------
    // 6. Transakcja grupowa INCOME tworzy długi od aktualnego użytkownika do pozostałych
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("6. Transakcja INCOME tworzy długi od aktualnego użytkownika do pozostałych członków")
    void groupTransactionIncomeCurrentUserIsDebtor() {
        loginAs(owner);
        GroupDTO dto = new GroupDTO();
        dto.setName("Praca");
        Group group = groupService.createGroup(dto);

        MembershipDTO mDto = new MembershipDTO();
        mDto.setGroupId(group.getId());
        mDto.setUserEmail(member.getEmail());
        membershipService.addMember(mDto);

        GroupTransactionDTO tx = new GroupTransactionDTO();
        tx.setGroupId(group.getId());
        tx.setAmount(200.0);
        tx.setType("INCOME");
        tx.setTitle("Premia");
        groupTransactionService.addGroupTransaction(tx, owner);

        List<Debt> debts = debtRepository.findByGroupId(group.getId());
        // owner (currentUser) powinien być dłużnikiem wobec member
        assertThat(debts).anyMatch(d ->
                d.getDebtor().getId().equals(owner.getId()) &&
                d.getCreditor().getId().equals(member.getId()));
    }

    // -------------------------------------------------------------------------
    // 7. Usunięcie członka nie usuwa jego historycznych długów
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("7. Usunięcie członka nie usuwa jego historycznych długów")
    void removeMemberDoesNotDeleteHistoricalDebts() {
        loginAs(owner);
        GroupDTO dto = new GroupDTO();
        dto.setName("Historia");
        Group group = groupService.createGroup(dto);

        MembershipDTO mDto = new MembershipDTO();
        mDto.setGroupId(group.getId());
        mDto.setUserEmail(member.getEmail());
        Membership membership = membershipService.addMember(mDto);

        // dodaj dług ręczny
        DebtDTO debtDto = new DebtDTO();
        debtDto.setGroupId(group.getId());
        debtDto.setDebtorId(member.getId());
        debtDto.setCreditorId(owner.getId());
        debtDto.setAmount(50.0);
        debtDto.setTitle("Stary dług");
        debtService.createDebt(debtDto);

        long debtCountBefore = debtRepository.findByGroupId(group.getId()).size();

        // usuń member
        membershipService.removeMember(membership.getId());

        long debtCountAfter = debtRepository.findByGroupId(group.getId()).size();
        assertThat(debtCountAfter).isEqualTo(debtCountBefore);
    }

    // -------------------------------------------------------------------------
    // 8. Nie można usunąć właściciela z jego grupy przez removeMember
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("8. Nie można usunąć właściciela z jego grupy przez removeMember")
    void removeMemberCannotRemoveOwner() {
        loginAs(owner);
        GroupDTO dto = new GroupDTO();
        dto.setName("Moja Grupa");
        Group group = groupService.createGroup(dto);

        Membership ownerMembership = membershipRepository
                .findByGroupId(group.getId()).stream()
                .filter(m -> m.getUser().getId().equals(owner.getId()))
                .findFirst()
                .orElseThrow();

        assertThatThrownBy(() -> membershipService.removeMember(ownerMembership.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("właściciela");
    }

    // -------------------------------------------------------------------------
    // 9. Członek grupy niebędący właścicielem nie może usunąć grupy
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("9. Członek grupy niebędący właścicielem nie może usunąć grupy")
    void deleteGroupOnlyOwnerCanDelete() {
        loginAs(owner);
        GroupDTO dto = new GroupDTO();
        dto.setName("Czyja Grupa");
        Group group = groupService.createGroup(dto);

        MembershipDTO mDto = new MembershipDTO();
        mDto.setGroupId(group.getId());
        mDto.setUserEmail(member.getEmail());
        membershipService.addMember(mDto);

        // member próbuje usunąć grupę
        loginAs(member);
        assertThatThrownBy(() -> groupService.deleteGroup(group.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    // -------------------------------------------------------------------------
    // 10. createDebt tworzy ręczny dług tylko między członkami tej samej grupy
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("10. createDebt tworzy ręczny dług tylko między członkami tej samej grupy")
    void createDebtOnlyBetweenGroupMembers() {
        loginAs(owner);
        GroupDTO dto = new GroupDTO();
        dto.setName("Rachunki");
        Group group = groupService.createGroup(dto);

        MembershipDTO mDto = new MembershipDTO();
        mDto.setGroupId(group.getId());
        mDto.setUserEmail(member.getEmail());
        membershipService.addMember(mDto);

        DebtDTO debtDto = new DebtDTO();
        debtDto.setGroupId(group.getId());
        debtDto.setDebtorId(member.getId());
        debtDto.setCreditorId(owner.getId());
        debtDto.setAmount(30.0);
        debtDto.setTitle("Czynsz");

        Debt debt = debtService.createDebt(debtDto);
        assertThat(debt.getId()).isNotNull();
        assertThat(debt.getAmount()).isEqualTo(30.0);
    }

    // -------------------------------------------------------------------------
    // 11. createDebt odrzuca użytkownika spoza grupy i dług do samego siebie
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("11. createDebt odrzuca użytkownika spoza grupy i dług do samego siebie")
    void createDebtRejectsOutsiderAndSelfDebt() {
        loginAs(owner);
        GroupDTO dto = new GroupDTO();
        dto.setName("Prywatna");
        Group group = groupService.createGroup(dto);

        // dług z outsiderem (nie jest członkiem)
        DebtDTO outsiderDebt = new DebtDTO();
        outsiderDebt.setGroupId(group.getId());
        outsiderDebt.setDebtorId(outsider.getId());
        outsiderDebt.setCreditorId(owner.getId());
        outsiderDebt.setAmount(10.0);
        outsiderDebt.setTitle("Test");

        assertThatThrownBy(() -> debtService.createDebt(outsiderDebt))
                .isInstanceOf(AccessDeniedException.class);

        // dług do samego siebie
        DebtDTO selfDebt = new DebtDTO();
        selfDebt.setGroupId(group.getId());
        selfDebt.setDebtorId(owner.getId());
        selfDebt.setCreditorId(owner.getId());
        selfDebt.setAmount(10.0);
        selfDebt.setTitle("Sam do siebie");

        assertThatThrownBy(() -> debtService.createDebt(selfDebt))
                .isInstanceOf(IllegalStateException.class);
    }

    // -------------------------------------------------------------------------
    // 12. Właściciel grupy może utworzyć dług między innymi członkami grupy
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("12. Właściciel grupy może utworzyć dług między innymi członkami grupy")
    void createDebtOwnerCanCreateBetweenOtherMembers() {
        loginAs(owner);
        GroupDTO dto = new GroupDTO();
        dto.setName("Projekt");
        Group group = groupService.createGroup(dto);

        User member2 = createUser("member2", "member2@test.com");

        MembershipDTO m1 = new MembershipDTO();
        m1.setGroupId(group.getId());
        m1.setUserEmail(member.getEmail());
        membershipService.addMember(m1);

        MembershipDTO m2 = new MembershipDTO();
        m2.setGroupId(group.getId());
        m2.setUserEmail(member2.getEmail());
        membershipService.addMember(m2);

        // owner tworzy dług między member i member2 (nie jest uczestnikiem)
        DebtDTO debtDto = new DebtDTO();
        debtDto.setGroupId(group.getId());
        debtDto.setDebtorId(member.getId());
        debtDto.setCreditorId(member2.getId());
        debtDto.setAmount(75.0);
        debtDto.setTitle("Sprzęt");

        Debt debt = debtService.createDebt(debtDto);
        assertThat(debt).isNotNull();
        assertThat(debt.getDebtor().getId()).isEqualTo(member.getId());
    }

    // -------------------------------------------------------------------------
    // 13. Członek grupy może utworzyć dług tylko gdy jest jego uczestnikiem
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("13. Członek grupy może utworzyć dług tylko gdy jest jego uczestnikiem")
    void createDebtMemberCanCreateOnlyAsParticipant() {
        loginAs(owner);
        GroupDTO dto = new GroupDTO();
        dto.setName("Sport");
        Group group = groupService.createGroup(dto);

        User member2 = createUser("member2b", "member2b@test.com");

        MembershipDTO m1 = new MembershipDTO();
        m1.setGroupId(group.getId());
        m1.setUserEmail(member.getEmail());
        membershipService.addMember(m1);

        MembershipDTO m2 = new MembershipDTO();
        m2.setGroupId(group.getId());
        m2.setUserEmail(member2.getEmail());
        membershipService.addMember(m2);

        // member loguje się i próbuje stworzyć dług między owner a member2 (nie jest uczestnikiem)
        loginAs(member);
        DebtDTO illegalDebt = new DebtDTO();
        illegalDebt.setGroupId(group.getId());
        illegalDebt.setDebtorId(owner.getId());
        illegalDebt.setCreditorId(member2.getId());
        illegalDebt.setAmount(20.0);
        illegalDebt.setTitle("Nielegalny");

        assertThatThrownBy(() -> debtService.createDebt(illegalDebt))
                .isInstanceOf(AccessDeniedException.class);

        // member może stworzyć dług gdzie sam jest uczestnikiem
        loginAs(member);
        DebtDTO legalDebt = new DebtDTO();
        legalDebt.setGroupId(group.getId());
        legalDebt.setDebtorId(member.getId());
        legalDebt.setCreditorId(owner.getId());
        legalDebt.setAmount(20.0);
        legalDebt.setTitle("Legalny");

        Debt created = debtService.createDebt(legalDebt);
        assertThat(created.getId()).isNotNull();
    }

    // -------------------------------------------------------------------------
    // 14. deleteDebt usuwa dług dostępny dla uczestnika długu
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("14. deleteDebt usuwa dług dostępny dla uczestnika długu")
    void deleteDebtParticipantCanDelete() {
        loginAs(owner);
        GroupDTO dto = new GroupDTO();
        dto.setName("Usuwanie");
        Group group = groupService.createGroup(dto);

        MembershipDTO mDto = new MembershipDTO();
        mDto.setGroupId(group.getId());
        mDto.setUserEmail(member.getEmail());
        membershipService.addMember(mDto);

        DebtDTO debtDto = new DebtDTO();
        debtDto.setGroupId(group.getId());
        debtDto.setDebtorId(member.getId());
        debtDto.setCreditorId(owner.getId());
        debtDto.setAmount(40.0);
        debtDto.setTitle("Do usunięcia");
        Debt debt = debtService.createDebt(debtDto);

        // member (dłużnik) może usunąć ten dług
        loginAs(member);
        debtService.deleteDebt(debt.getId());

        assertThat(debtRepository.findById(debt.getId())).isEmpty();
    }

    // -------------------------------------------------------------------------
    // 15. deleteDebt odrzuca członka grupy, który nie jest właścicielem ani uczestnikiem długu
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("15. deleteDebt odrzuca członka niebędącego właścicielem ani uczestnikiem długu")
    void deleteDebtNonParticipantNonOwnerCannotDelete() {
        loginAs(owner);
        GroupDTO dto = new GroupDTO();
        dto.setName("Ochrona");
        Group group = groupService.createGroup(dto);

        User member2 = createUser("member2c", "member2c@test.com");

        MembershipDTO m1 = new MembershipDTO();
        m1.setGroupId(group.getId());
        m1.setUserEmail(member.getEmail());
        membershipService.addMember(m1);

        MembershipDTO m2 = new MembershipDTO();
        m2.setGroupId(group.getId());
        m2.setUserEmail(member2.getEmail());
        membershipService.addMember(m2);

        // dług między owner i member
        DebtDTO debtDto = new DebtDTO();
        debtDto.setGroupId(group.getId());
        debtDto.setDebtorId(owner.getId());
        debtDto.setCreditorId(member.getId());
        debtDto.setAmount(25.0);
        debtDto.setTitle("Cudzy dług");
        Debt debt = debtService.createDebt(debtDto);

        // member2 nie jest uczestnikiem tego długu i nie jest właścicielem grupy
        loginAs(member2);
        assertThatThrownBy(() -> debtService.deleteDebt(debt.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    // -------------------------------------------------------------------------
    // 16. Właściciel grupy może usunąć dług, którego nie jest uczestnikiem
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("16. Właściciel grupy może usunąć dług, którego nie jest uczestnikiem")
    void deleteDebtOwnerCanDeleteAnyDebt() {
        loginAs(owner);
        GroupDTO dto = new GroupDTO();
        dto.setName("Admin");
        Group group = groupService.createGroup(dto);

        User member2 = createUser("member2d", "member2d@test.com");

        MembershipDTO m1 = new MembershipDTO();
        m1.setGroupId(group.getId());
        m1.setUserEmail(member.getEmail());
        membershipService.addMember(m1);

        MembershipDTO m2 = new MembershipDTO();
        m2.setGroupId(group.getId());
        m2.setUserEmail(member2.getEmail());
        membershipService.addMember(m2);

        // dług między member a member2 (owner nie jest uczestnikiem)
        DebtDTO debtDto = new DebtDTO();
        debtDto.setGroupId(group.getId());
        debtDto.setDebtorId(member.getId());
        debtDto.setCreditorId(member2.getId());
        debtDto.setAmount(15.0);
        debtDto.setTitle("Dług między memberami");
        Debt debt = debtService.createDebt(debtDto);

        // owner (nie uczestnik) może usunąć jako właściciel grupy
        loginAs(owner);
        debtService.deleteDebt(debt.getId());

        assertThat(debtRepository.findById(debt.getId())).isEmpty();
    }

    // -------------------------------------------------------------------------
    // 17. Walidacje DTO odrzucają puste lub niepoprawne wartości
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("17. Walidacje DTO odrzucają puste lub niepoprawne wartości")
    void validationRejectsBlankAndInvalidValues() {
        loginAs(owner);
        GroupDTO dto = new GroupDTO();
        dto.setName("Walidacja");
        Group group = groupService.createGroup(dto);

        MembershipDTO mDto = new MembershipDTO();
        mDto.setGroupId(group.getId());
        mDto.setUserEmail(member.getEmail());
        membershipService.addMember(mDto);

        // DebtDTO z kwotą ujemną
        DebtDTO negativeAmount = new DebtDTO();
        negativeAmount.setGroupId(group.getId());
        negativeAmount.setDebtorId(member.getId());
        negativeAmount.setCreditorId(owner.getId());
        negativeAmount.setAmount(-10.0);
        negativeAmount.setTitle("Ujemna kwota");
        assertThat(validator.validate(negativeAmount)).isNotEmpty();

        // GroupDTO pusta nazwa
        GroupDTO emptyName = new GroupDTO();
        emptyName.setName("");
        assertThat(validator.validate(emptyName)).isNotEmpty();

        // MembershipDTO nieprawidłowy email
        MembershipDTO badEmail = new MembershipDTO();
        badEmail.setGroupId(group.getId());
        badEmail.setUserEmail("not-an-email");
        assertThat(validator.validate(badEmail)).isNotEmpty();

        // DebtDTO null debtorId
        DebtDTO nullDebtor = new DebtDTO();
        nullDebtor.setGroupId(group.getId());
        nullDebtor.setDebtorId(null);
        nullDebtor.setCreditorId(owner.getId());
        nullDebtor.setAmount(10.0);
        nullDebtor.setTitle("Brak dłużnika");
        assertThat(validator.validate(nullDebtor)).isNotEmpty();

        // Sprawdzenie null groupId w DebtDTO nie przejdzie przez serwis
        DebtDTO nullGroup = new DebtDTO();
        nullGroup.setGroupId(null);
        nullGroup.setDebtorId(member.getId());
        nullGroup.setCreditorId(owner.getId());
        nullGroup.setAmount(10.0);
        nullGroup.setTitle("Brak grupy");

        assertThatThrownBy(() -> debtService.createDebt(nullGroup))
                .isInstanceOf(Exception.class);
    }

    // -------------------------------------------------------------------------
    // 18. Usunięcie grupy przez właściciela usuwa powiązane długi i grupę
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("18. Usunięcie grupy przez właściciela usuwa powiązane długi i grupę")
    void deleteGroupRemovesDebtsAndMembers() {
        loginAs(owner);
        GroupDTO dto = new GroupDTO();
        dto.setName("Do usunięcia");
        Group group = groupService.createGroup(dto);
        Long groupId = group.getId();

        MembershipDTO mDto = new MembershipDTO();
        mDto.setGroupId(groupId);
        mDto.setUserEmail(member.getEmail());
        membershipService.addMember(mDto);

        // dodaj dług w grupie
        GroupTransactionDTO tx = new GroupTransactionDTO();
        tx.setGroupId(groupId);
        tx.setAmount(100.0);
        tx.setType(EXPENSE_TYPE);
        tx.setTitle("Wydatek");
        groupTransactionService.addGroupTransaction(tx, owner);

        assertThat(debtRepository.findByGroupId(groupId)).isNotEmpty();
        assertThat(membershipRepository.findByGroupId(groupId)).isNotEmpty();

        // owner usuwa grupę
        groupService.deleteGroup(groupId);

        // wszystko powinno zostać usunięte
        assertThat(groupRepository.findById(groupId)).isEmpty();
        assertThat(debtRepository.findByGroupId(groupId)).isEmpty();
        assertThat(membershipRepository.findByGroupId(groupId)).isEmpty();
    }
}
