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
public class TransactionFactoryResolver {

    private static Map values = new HashMap(20);

    private static TransactionFactoryResolver instance = new TransactionFactoryResolver();

    static {
        values.put("JBoss",
                "net.sf.hibernate.transaction.JBossTransactionManagerLookup");
        values
                .put("Weblogic",
                        "net.sf.hibernate.transaction.WeblogicTransactionManagerLookup");
        values
                .put("WebSphere",
                        "net.sf.hibernate.transaction.WebSphereTransactionManagerLookup");
        values.put("Orion",
                "net.sf.hibernate.transaction.OrionTransactionManagerLookup");
        values.put("Resin",
                "net.sf.hibernate.transaction.ResinTransactionManagerLookup");
        values.put("JOTM",
                "net.sf.hibernate.transaction.JOTMTransactionManagerLookup");
        values.put("JOnAS",
                "net.sf.hibernate.transaction.JOnASTransactionManagerLookup");
        values.put("JRun4",
                "net.sf.hibernate.transaction.JRun4TransactionManagerLookup");
    }

    public static TransactionFactoryResolver getInstance() {
        return instance;
    }

    public static List getApplicationServers() {
        List databaseNames = new ArrayList(values.size());
        for (Iterator i = values.keySet().iterator(); i.hasNext();) {
            databaseNames.add(i.next());
        }
        Collections.sort(databaseNames);
        return databaseNames;
    }

    public String resolve(String appServer) {
        if (null == appServer)
            return null;
        else if (appServer.equals("N/A"))
            return "net.sf.hibernate.transaction.JDBCTransactionFactory";
        return (String) values.get(appServer);
    }
}