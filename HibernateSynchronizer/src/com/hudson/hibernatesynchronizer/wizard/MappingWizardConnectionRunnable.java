package com.hudson.hibernatesynchronizer.wizard;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;

import com.hudson.hibernatesynchronizer.Plugin;
import com.hudson.hibernatesynchronizer.db.Container;
import com.hudson.hibernatesynchronizer.db.DBTable;
import com.hudson.hibernatesynchronizer.db.MetaDataRetriever;

/**
 * @author Joe Hudson
 */
public class MappingWizardConnectionRunnable implements Runnable {

	private MappingWizardPage page;
	private IProgressMonitor progressMonitor;
	
	/**
	 * 
	 */
	public MappingWizardConnectionRunnable(MappingWizardPage page, IProgressMonitor progressMonitor) {
		this.page = page;
		this.progressMonitor = progressMonitor;
	}

	public void run() {
		Connection conn = null;
		try {
			conn = page.getConnection(progressMonitor);
			MetaDataRetriever mdr = null;
			if (null != conn) {
				try {
					progressMonitor.subTask("Retrieving table metadata from connection");
					page.tableContainer = new Container();
					int count = MetaDataRetriever.getTableCount(conn.getMetaData(), page.schemaPattern.getText(), page.tablePattern.getText());
					MetaDataRetriever.getTables(page.tableContainer, conn.getMetaData(), page.schemaPattern.getText(), page.tablePattern.getText());
					page.tables = page.tableContainer.getTables();
					progressMonitor.worked(3);
					Shell warnShell = null;
					mdr = new MetaDataRetriever(page.tableContainer, conn.getMetaData(), page);
					try {
						page.notifiedDriver = (Class) MappingWizardPage.drivers.get(page.currentDriverClass).getClass();
					}
					catch (Exception e) {}
				}
				catch (Exception e) {
					Plugin.log(e);
					if (null != conn) conn.close();
					page.onError(null, e);
					return;
				}
				// the connection will get closed in the thread
				mdr.start();
				progressMonitor.subTask("Populating table list");
				List tablesList = new ArrayList(page.tables.size());
				for (Iterator i=page.tables.values().iterator(); i.hasNext(); ) {
					tablesList.add(i.next());
				}
				Collections.sort(tablesList);
				for (Iterator i=tablesList.iterator(); i.hasNext(); ) {
					DBTable tbl = (DBTable) i.next();
					TableItem item = new TableItem(page.table, SWT.NULL);
					String[] arr = {tbl.getName()};
					item.setText(arr);
				}
				progressMonitor.worked(1);
				page.dialogChanged();
			}
		}
		catch (SQLException e) {
			page.onError(null, e);
		}
	}
}