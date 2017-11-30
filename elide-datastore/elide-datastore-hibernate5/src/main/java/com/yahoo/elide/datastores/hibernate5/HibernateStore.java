/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.hibernate5;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import org.hibernate.ScrollMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.jpa.HibernateEntityManager;
import org.hibernate.metadata.ClassMetadata;

/**
 * Hibernate interface library.
 */
public abstract class HibernateStore implements DataStore {
    protected final SessionFactory sessionFactory;
    protected final boolean isScrollEnabled;
    protected final ScrollMode scrollMode;
    protected final HibernateTransactionSupplier transactionSupplier;

    /**
     * Constructor.
     *
     * @param aSessionFactory Session factory
     * @param isScrollEnabled Whether or not scrolling is enabled on driver
     * @param scrollMode Scroll mode to use for scrolling driver
     */
    protected HibernateStore(SessionFactory aSessionFactory, boolean isScrollEnabled, ScrollMode scrollMode) {
        this(aSessionFactory, isScrollEnabled, scrollMode, HibernateTransaction::new);
    }

    /**
     * Constructor.
     *
     * Useful for extending the store and relying on existing code
     * to instantiate custom hibernate transaction.
     *
     * @param aSessionFactory Session factory
     * @param isScrollEnabled Whether or not scrolling is enabled on driver
     * @param scrollMode Scroll mode to use for scrolling driver
     * @param transactionSupplier Supplier for transaction
     */
    protected HibernateStore(SessionFactory aSessionFactory,
                             boolean isScrollEnabled,
                             ScrollMode scrollMode,
                             HibernateTransactionSupplier transactionSupplier) {
        this.sessionFactory = aSessionFactory;
        this.isScrollEnabled = isScrollEnabled;
        this.scrollMode = scrollMode;
        this.transactionSupplier = transactionSupplier;
    }

    /**
     * Builder object to configuration hibernate store.
     */
    public static class Builder {
        private final SessionFactory sessionFactory;
        private final HibernateEntityManager entityManager;
        private boolean isScrollEnabled;
        private ScrollMode scrollMode;

        public Builder(final SessionFactory sessionFactory) {
            this.sessionFactory = sessionFactory;
            this.isScrollEnabled = true;
            this.scrollMode = ScrollMode.FORWARD_ONLY;
            this.entityManager = null;
        }

        public Builder(final HibernateEntityManager entityManager) {
            this.sessionFactory = null;
            this.isScrollEnabled = true;
            this.scrollMode = ScrollMode.FORWARD_ONLY;
            this.entityManager = entityManager;
        }

        public Builder withScrollEnabled(final boolean isScrollEnabled) {
            this.isScrollEnabled = isScrollEnabled;
            return this;
        }

        public Builder withScrollMode(final ScrollMode scrollMode) {
            this.scrollMode = scrollMode;
            return this;
        }

        public HibernateStore build() {
            if (sessionFactory != null) {
                return new HibernateSessionFactoryStore(sessionFactory, isScrollEnabled, scrollMode);
            } else if (entityManager != null) {
                return new HibernateEntityManagerStore(entityManager, isScrollEnabled, scrollMode);
            }
            throw new IllegalStateException("Either an EntityManager or SessionFactory is required!");
        }
    }

    @Override
    public void populateEntityDictionary(EntityDictionary dictionary) {
        /* bind all entities */
        for (ClassMetadata meta : sessionFactory.getAllClassMetadata().values()) {
            dictionary.bindEntity(meta.getMappedClass());
        }
    }

    /**
     * Get current Hibernate session.
     *
     * @return session
     */
    abstract public Session getSession();

    /**
     * Start Hibernate transaction.
     *
     * @return transaction
     */
    @Override
    abstract public DataStoreTransaction beginTransaction();

    /**
     * Functional interface for describing a method to supply a custom Hibernate transaction.
     */
    @FunctionalInterface
    public interface HibernateTransactionSupplier {
        HibernateTransaction get(Session session, boolean isScrollEnabled, ScrollMode scrollMode);
    }
}
