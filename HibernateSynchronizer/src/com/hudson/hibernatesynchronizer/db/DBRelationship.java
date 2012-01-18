/**
 * This software is licensed under the general public license.  See http://www.gnu.org/copyleft/gpl.html
 * for more information.
 */
package com.hudson.hibernatesynchronizer.db;

/**
 * @author <a href="mailto: joe@binamics.com">Joe Hudson </a>
 */
public class DBRelationship {
    private DBColumn primaryKey;

    private DBColumn foreignKey;

    /**
     * @return
     */
    public DBColumn getForeignKey() {
        return foreignKey;
    }

    /**
     * @param foreignKey
     */
    public void setForeignKey(DBColumn foreignKey) {
        this.foreignKey = foreignKey;
    }

    /**
     * @return
     */
    public DBColumn getPrimaryKey() {
        return primaryKey;
    }

    /**
     * @param primaryKey
     */
    public void setPrimaryKey(DBColumn primaryKey) {
        this.primaryKey = primaryKey;
    }
}