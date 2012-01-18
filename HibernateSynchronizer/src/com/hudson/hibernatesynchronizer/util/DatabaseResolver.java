/**
 * This software is licensed under the general public license.  See http://www.gnu.org/copyleft/gpl.html
 * for more information.
 */
package com.hudson.hibernatesynchronizer.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto: joe@binamics.com">Joe Hudson </a>
 */
public class DatabaseResolver {

    private static Map values = new HashMap(20);

    private static DatabaseResolver instance = new DatabaseResolver();

    static {
        values.put("DB2", "net.sf.hibernate.dialect.DB2Dialect");
        values.put("MySQL", "net.sf.hibernate.dialect.MySQLDialect");
        values.put("SAP DB", "net.sf.hibernate.dialect.SAPDBDialect");
        values.put("Oracle (any version)",
                "net.sf.hibernate.dialect.OracleDialect");
        values.put("Oracle 9", "net.sf.hibernate.dialect.Oracle9Dialect");
        values.put("Sybase", "net.sf.hibernate.dialect.SybaseDialect");
        values.put("Sybase Anywhere",
                "net.sf.hibernate.dialect.SybaseAnywhereDialect");
        values.put("Progress", "net.sf.hibernate.dialect.ProgressDialect");
        values.put("Mckoi SQL", "net.sf.hibernate.dialect.MckoiDialect");
        values.put("Interbase", "net.sf.hibernate.dialect.InterbaseDialect");
        values.put("Pointbase", "net.sf.hibernate.dialect.PointbaseDialect");
        values.put("PostgreSQL", "net.sf.hibernate.dialect.PostgreSQLDialect");
        values.put("HypersonicSQL", "net.sf.hibernate.dialect.HSQLDialect");
        values.put("Microsoft SQL Server",
                "net.sf.hibernate.dialect.SQLServerDialect");
        values.put("Ingres", "net.sf.hibernate.dialect.IngresDialect");
        values.put("Informix", "net.sf.hibernate.dialect.InformixDialect");
        values.put("FrontBase", "net.sf.hibernate.dialect.FrontbaseDialect");
    }

    public static DatabaseResolver getInstance() {
        return instance;
    }

    public static List getDatabaseNames() {
        List databaseNames = new ArrayList(values.size());
        for (Iterator i = values.keySet().iterator(); i.hasNext();) {
            databaseNames.add(i.next());
        }
        Collections.sort(databaseNames);
        return databaseNames;
    }

    public String resolve(String databaseName) {
        return (String) values.get(databaseName);
    }
}