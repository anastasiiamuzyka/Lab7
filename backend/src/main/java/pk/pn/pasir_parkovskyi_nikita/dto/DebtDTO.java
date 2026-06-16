package pk.pn.pasir_parkovskyi_nikita.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DebtDTO {

    @NotNull(message = "Id dłużnika nie może być puste")
    private Long debtorId;

    @NotNull(message = "Id wierzyciela nie może być puste")
    private Long creditorId;

    @NotNull(message = "Id grupy nie może być puste")
    private Long groupId;

    @NotNull(message = "Kwota nie może być pusta")
    @Positive(message = "Kwota musi być większa od zera")
    private Double amount;

    @NotBlank(message = "Tytuł nie może być pusty")
    @Size(max = 100, message = "Tytuł nie może przekraczać 100 znaków")
    private String title;

    public Long getDebtorId() { return debtorId; }
    public void setDebtorId(Long debtorId) { this.debtorId = debtorId; }
    public Long getCreditorId() { return creditorId; }
    public void setCreditorId(Long creditorId) { this.creditorId = creditorId; }
    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
}
