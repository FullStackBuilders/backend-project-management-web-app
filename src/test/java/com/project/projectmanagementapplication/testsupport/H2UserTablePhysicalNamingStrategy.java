package com.project.projectmanagementapplication.testsupport;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

/**
 * H2 treats {@code USER} as a reserved word; quote the {@code user} table only so
 * {@link com.project.projectmanagementapplication.model.User} maps without enabling
 * {@code globally_quoted_identifiers} (which breaks broader DDL in this project).
 */
public class H2UserTablePhysicalNamingStrategy implements PhysicalNamingStrategy {

    private static final PhysicalNamingStrategy DELEGATE = new PhysicalNamingStrategyStandardImpl();

    @Override
    public Identifier toPhysicalCatalogName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
        return DELEGATE.toPhysicalCatalogName(logicalName, jdbcEnvironment);
    }

    @Override
    public Identifier toPhysicalSchemaName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
        return DELEGATE.toPhysicalSchemaName(logicalName, jdbcEnvironment);
    }

    @Override
    public Identifier toPhysicalTableName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
        if (logicalName == null) {
            return null;
        }
        Identifier base = DELEGATE.toPhysicalTableName(logicalName, jdbcEnvironment);
        if (base != null && "user".equalsIgnoreCase(base.getText())) {
            return Identifier.toIdentifier(base.getText(), true);
        }
        return base;
    }

    @Override
    public Identifier toPhysicalSequenceName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
        return DELEGATE.toPhysicalSequenceName(logicalName, jdbcEnvironment);
    }

    @Override
    public Identifier toPhysicalColumnName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
        return DELEGATE.toPhysicalColumnName(logicalName, jdbcEnvironment);
    }
}
