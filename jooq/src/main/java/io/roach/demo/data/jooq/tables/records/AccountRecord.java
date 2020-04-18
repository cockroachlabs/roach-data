/*
 * This file is generated by jOOQ.
 */
package io.roach.demo.data.jooq.tables.records;


import io.roach.demo.data.jooq.tables.Account;

import java.math.BigDecimal;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record4;
import org.jooq.Row4;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class AccountRecord extends UpdatableRecordImpl<AccountRecord> implements Record4<Long, BigDecimal, String, String> {

    private static final long serialVersionUID = 799984895;

    /**
     * Setter for <code>public.account.id</code>.
     */
    public void setId(Long value) {
        set(0, value);
    }

    /**
     * Getter for <code>public.account.id</code>.
     */
    public Long getId() {
        return (Long) get(0);
    }

    /**
     * Setter for <code>public.account.balance</code>.
     */
    public void setBalance(BigDecimal value) {
        set(1, value);
    }

    /**
     * Getter for <code>public.account.balance</code>.
     */
    public BigDecimal getBalance() {
        return (BigDecimal) get(1);
    }

    /**
     * Setter for <code>public.account.name</code>.
     */
    public void setName(String value) {
        set(2, value);
    }

    /**
     * Getter for <code>public.account.name</code>.
     */
    public String getName() {
        return (String) get(2);
    }

    /**
     * Setter for <code>public.account.type</code>.
     */
    public void setType(String value) {
        set(3, value);
    }

    /**
     * Getter for <code>public.account.type</code>.
     */
    public String getType() {
        return (String) get(3);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<Long> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record4 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row4<Long, BigDecimal, String, String> fieldsRow() {
        return (Row4) super.fieldsRow();
    }

    @Override
    public Row4<Long, BigDecimal, String, String> valuesRow() {
        return (Row4) super.valuesRow();
    }

    @Override
    public Field<Long> field1() {
        return Account.ACCOUNT.ID;
    }

    @Override
    public Field<BigDecimal> field2() {
        return Account.ACCOUNT.BALANCE;
    }

    @Override
    public Field<String> field3() {
        return Account.ACCOUNT.NAME;
    }

    @Override
    public Field<String> field4() {
        return Account.ACCOUNT.TYPE;
    }

    @Override
    public Long component1() {
        return getId();
    }

    @Override
    public BigDecimal component2() {
        return getBalance();
    }

    @Override
    public String component3() {
        return getName();
    }

    @Override
    public String component4() {
        return getType();
    }

    @Override
    public Long value1() {
        return getId();
    }

    @Override
    public BigDecimal value2() {
        return getBalance();
    }

    @Override
    public String value3() {
        return getName();
    }

    @Override
    public String value4() {
        return getType();
    }

    @Override
    public AccountRecord value1(Long value) {
        setId(value);
        return this;
    }

    @Override
    public AccountRecord value2(BigDecimal value) {
        setBalance(value);
        return this;
    }

    @Override
    public AccountRecord value3(String value) {
        setName(value);
        return this;
    }

    @Override
    public AccountRecord value4(String value) {
        setType(value);
        return this;
    }

    @Override
    public AccountRecord values(Long value1, BigDecimal value2, String value3, String value4) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached AccountRecord
     */
    public AccountRecord() {
        super(Account.ACCOUNT);
    }

    /**
     * Create a detached, initialised AccountRecord
     */
    public AccountRecord(Long id, BigDecimal balance, String name, String type) {
        super(Account.ACCOUNT);

        set(0, id);
        set(1, balance);
        set(2, name);
        set(3, type);
    }
}
