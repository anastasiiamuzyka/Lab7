package pk.pn.pasir_parkovskyi_nikita.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Data Transfer Object for user balance summary.
 */
@Data
@AllArgsConstructor
public class BalanceDTO {
    private double totalIncome;
    private double totalExpense;
    private double balance;
}
