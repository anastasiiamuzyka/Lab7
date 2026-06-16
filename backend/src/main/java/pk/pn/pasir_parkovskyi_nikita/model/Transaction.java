package pk.pn.pasir_parkovskyi_nikita.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * The Transaction entity represents a single financial transaction.
 * Each transaction has a unique identifier, amount, type, tags, notes, and creation date.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "transactions") // You should use "transactions" instead of "transaction",
                              // because "transaction" is a reserved word in SQL.
@SuppressWarnings("JpaDataSourceORMInspection") // Suppress IDE warning - table will be created by Hibernate
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double amount; // Transaction amount

    @Enumerated(EnumType.STRING)
    private TransactionType type; // Transaction type (INCOME or EXPENSE)

    private String tags; // List of tags or a single tag (as a String for simplicity)

    private String notes; // Additional notes

    private LocalDateTime timestamp; // Date and time the transaction was created

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    public Transaction(Double amount, TransactionType type, String tags, String notes, User user) {
        this.amount = amount;
        this.type = type;
        this.tags = tags;
        this.notes = notes;
        this.user = user;
        this.timestamp = LocalDateTime.now(ZoneId.systemDefault());
    }
}
