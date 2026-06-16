package pk.pn.pasir_parkovskyi_nikita.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pk.pn.pasir_parkovskyi_nikita.dto.GroupDTO;
import pk.pn.pasir_parkovskyi_nikita.model.Group;
import pk.pn.pasir_parkovskyi_nikita.model.Membership;
import pk.pn.pasir_parkovskyi_nikita.model.User;
import pk.pn.pasir_parkovskyi_nikita.repository.DebtRepository;
import pk.pn.pasir_parkovskyi_nikita.repository.GroupRepository;
import pk.pn.pasir_parkovskyi_nikita.repository.MembershipRepository;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class GroupService {
    private final GroupRepository groupRepository;
    private final MembershipRepository membershipRepository;
    private final DebtRepository debtRepository;
    private final CurrentUserService currentUserService;

    public GroupService(
            GroupRepository groupRepository,
            MembershipRepository membershipRepository,
            DebtRepository debtRepository,
            CurrentUserService currentUserService) {
        this.groupRepository = groupRepository;
        this.membershipRepository = membershipRepository;
        this.debtRepository = debtRepository;
        this.currentUserService = currentUserService;
    }

    public List<Group> getAllGroups() {
        User currentUser = currentUserService.getCurrentUser();
        return groupRepository.findByMemberships_User(currentUser);
    }

    public Group createGroup(GroupDTO groupDTO) {
        User owner = currentUserService.getCurrentUser(); // get currently logged user
        Group group = new Group();
        group.setName(groupDTO.getName());
        group.setOwner(owner);
        group.setCreatedAt(LocalDateTime.now(ZoneId.systemDefault()));
        Group savedGroup = groupRepository.save(group);

        // Add the owner as the first member of the newly created group
        Membership membership = new Membership();
        membership.setUser(owner);
        membership.setGroup(savedGroup);
        membershipRepository.save(membership);

        return savedGroup;
    }

    @Transactional
    public void deleteGroup(Long id) {
        Group group = groupRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Nie można usunąć grupy. Grupa o ID " + id + " nie istnieje."));

        User currentUser = currentUserService.getCurrentUser();
        if (!group.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Tylko właściciel grupy może ją usunąć.");
        }

        // Remove debts and members first, then delete the group
        debtRepository.deleteByGroupId(id);
        membershipRepository.deleteByGroupId(id);
        groupRepository.delete(group);
    }
}
