package com.hudson.hibernatesynchronizer.widgets;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import com.hudson.hibernatesynchronizer.Constants;
import com.hudson.hibernatesynchronizer.resource.ResourceManager;
import com.hudson.hibernatesynchronizer.resource.Template;

/**
 * @author Joe Hudson
 */
public class TemplateImportExportHandler implements ImportExportHandler {

	public void exportResources(List selectedResources, IProject project, Shell shell) throws Exception {
        FileDialog fd = new FileDialog(shell);
        fd.setFilterExtensions(new String[] { "*.zip" });
        String outFileName = fd.open();
        if (null != outFileName && outFileName.trim().length() > 0) {
        	outFileName = outFileName.trim();
            if (outFileName.indexOf(".") < 0)
            	outFileName = outFileName + ".zip";
            ZipOutputStream zos = null;
            try {
            	zos = new ZipOutputStream(new FileOutputStream(
                        new File(outFileName)));
            	for (Iterator i=selectedResources.iterator(); i.hasNext(); ) {
            		Template template = (Template) i.next();
            		ZipEntry entry = new ZipEntry(URLEncoder.encode(template.getName(), "UTF-8") + Constants.EXTENSION_TEMPLATE);
            		zos.putNextEntry(entry);
            		zos.write(template.getFormattedFileContents().getBytes());
            		zos.closeEntry();
            	}
            	MessageDialog.openInformation(shell, "Export Sucessful", selectedResources.size() + " templates were exported to " + outFileName);
            }
            finally {
            	if (null != zos) zos.close();
            }
         }
	}

	public List importResources(IProject project, Shell shell) throws Exception {
        FileDialog fd = new FileDialog(shell);
        fd.setFilterExtensions(new String[] { "*.zip" });
        String inFileName = fd.open();
        if (null != inFileName) {
        	int count = 0;
            ZipFile zipFile = null;
            try {
            	zipFile = new ZipFile(new File(inFileName),
                    ZipFile.OPEN_READ);
	            for (Enumeration e = zipFile.entries(); e
	                    .hasMoreElements();) {
	                ZipEntry entry = (ZipEntry) e.nextElement();
	                String currentEntry = entry.getName();
	                BufferedInputStream is = new BufferedInputStream(
	                        zipFile.getInputStream(entry));
	                String fileName = zipFile.getName();
	                fileName = fileName.replace('\\', '/');
	                int index = fileName.lastIndexOf('/');
	                if (index >= 0)
	                	fileName = fileName.substring(index+1, fileName.length());
	                index = fileName.lastIndexOf('.');
	                if (index >= 0) {
	                	String extension = fileName.substring(index+1, fileName.length());
	                	if (extension.equals(Constants.EXTENSION_TEMPLATE)) {
		                	fileName = fileName.substring(0, index);
			                fileName = URLDecoder.decode(fileName, "UTF-8");
			                Template template = new Template();
			                template.load(fileName, is);
			                ResourceManager.saveWorkspaceResource(template);
			                count ++;
		                }
	                }
	            }
            }
            finally {
            	if (null != zipFile) zipFile.close();
            }

            if (count == 0) MessageDialog.openInformation(shell, "Import Failed", "0 templates were imported");
            else if (count == 1) MessageDialog.openInformation(shell, "Import Sucessful", "1 template was imported");
            else MessageDialog.openInformation(shell, "Import Sucessful", count + " templates were imported");
            return ResourceManager.getWorkspaceTemplates();
        }
        else {
        	return null;
        }
	}
}