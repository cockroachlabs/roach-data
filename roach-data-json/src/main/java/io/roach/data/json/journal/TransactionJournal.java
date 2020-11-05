package io.roach.data.json.journal;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.hibernate.annotations.TypeDef;

@Entity
@DiscriminatorValue("TRANSACTION")
@TypeDef(name = "jsonb", typeClass = Transaction.JsonType.class, defaultForType = Transaction.class)
public class TransactionJournal extends Journal<Transaction> {
}
