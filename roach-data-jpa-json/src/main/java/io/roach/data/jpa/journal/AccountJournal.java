package io.roach.data.jpa.journal;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.hibernate.annotations.TypeDef;

@Entity
@DiscriminatorValue("ACCOUNT")
@TypeDef(name = "jsonb", typeClass = Account.JsonType.class, defaultForType = Account.class)
public class AccountJournal extends Journal<Account> {
}
