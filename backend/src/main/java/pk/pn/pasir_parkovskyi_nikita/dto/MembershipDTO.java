package pk.pn.pasir_parkovskyi_nikita.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MembershipDTO {

    @NotBlank(message = "Email użytkownika nie może być pusty")
    @Email(message = "Email użytkownika musi być poprawnym adresem email")
    private String userEmail;

    @NotNull(message = "Id grupy nie może być puste")
    private Long groupId;

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }
}
