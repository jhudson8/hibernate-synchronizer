package com.hudson.hibernatesynchronizer.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;
import org.eclipse.ui.dialogs.ResourceSelectionDialog;

import com.hudson.hibernatesynchronizer.Constants;
import com.hudson.hibernatesynchronizer.Plugin;
import com.hudson.hibernatesynchronizer.dialogs.SourceLocationSelectorDialog;


/**
 * @author <a href="mailto: jhudson8@users.sourceforge.net">Joe Hudson</a>
 */
public class HSUtil {

	/**
	 * Return the contents of the Stream as a String.
	 * Note:  If the InputStream represents a null String, the Java implementation will try to read from the stream for a certain amount of time
	 * before timing out.
	 * @param is the InputStream to transform into a String
	 * @return the String representation of the Stream
	 */
	public static String getStringFromStream (InputStream is)
		throws IOException
	{
		try {
			InputStreamReader reader = new InputStreamReader(is);
			char[] buffer = new char[1024];
			StringWriter writer = new StringWriter();
			int bytes_read;
			while ((bytes_read = reader.read(buffer)) != -1)
			{
				writer.write(buffer, 0, bytes_read);
			}
			return (writer.toString());
		}
		finally {
			if (null != is) is.close();
		}
	}
	
	public static String firstLetterUpper (String s) {
		if (null == s) return null;
		else if (s.length() == 0) return s;
		else {
			return s.substring(0, 1).toUpperCase() + s.substring(1, s.length());
		}
	}

	public static String firstLetterLower (String s) {
		if (null == s) return null;
		else if (s.length() == 0) return s;
		else {
			return s.substring(0, 1).toLowerCase() + s.substring(1, s.length());
		}
	}

	public static String getClassPart (String fullClassName) {
		if (null == fullClassName) return null;
		int index = fullClassName.lastIndexOf(".");
		if (index == -1) return fullClassName;
		else return fullClassName.substring(index + 1, fullClassName.length());
	}
	
	private static final char[] vowelList = {'A', 'E', 'I', 'O', 'U'};
	public static String getPropName (String tableName) {
		if (tableName.toUpperCase().equals(tableName)) {
			boolean vowelsFound = false;
			for (int i=0; i<tableName.toCharArray().length; i++) {
				char c = tableName.toCharArray()[i];
				for (int j=0; j<vowelList.length; j++) {
					if (c == vowelList[j]) vowelsFound = true;
				}
			}
			if (vowelsFound) {
				tableName = tableName.toLowerCase();
			}
		}
		return getJavaNameCap(tableName);
	}
	
	public static String getJavaNameCap (String s) {
		if (s.indexOf("_") < 0 && s.indexOf("-") < 0) {
			return firstLetterUpper(s);
		}
		else {
			StringBuffer sb = new StringBuffer();
			boolean upper = true;
			char[] arr = s.toCharArray();
			for (int i=0; i<arr.length; i++) {
				if (arr[i] == '_' || arr[i] == '-') upper = true;
				else if (upper) {sb.append(Character.toUpperCase(arr[i])); upper = false;}
				else sb.append(Character.toLowerCase(arr[i]));
			}
			return sb.toString();
		}
	}

	public static String getJavaName (String s) {
		if (null == s) return null;
		else return firstLetterLower(getJavaNameCap(s));
	}

	public static String getPropDescription (String s) {
		if (null == s) return null;
		StringBuffer sb = new StringBuffer();
		boolean upper = true;
		char[] arr = s.toCharArray();
		for (int i=0; i<arr.length; i++) {
			if (i == 0) sb.append(Character.toUpperCase(arr[i]));
			else if (Character.isUpperCase(arr[i])) sb.append(" " + arr[i]);
			else sb.append(arr[i]);
		}
		return sb.toString();
	}
	
	public static String getPackagePart (String fullClassName) {
		if (null == fullClassName) return null;
		int index = fullClassName.lastIndexOf(".");
		if (index == -1) return "";
		else return fullClassName.substring(0, index);
	}
	
	public static String addPackageExtension (String packageStr, String extension) {
		if (null == packageStr || packageStr.trim().length() == 0) return extension;
		else return packageStr + "." + extension;
	}

	public static boolean deleteDir (File dir) {
		if (dir.isDirectory()) {
			String[] children = dir.list();
			for (int i=0; i<children.length; i++) {
				boolean success = deleteDir(new File(dir, children[i]));
				if (!success) {
					return false;
				}
			}
		}
		
		// The directory is now empty so delete it
		return dir.delete();
	}

	public static IPackageFragmentRoot getProjectRoot (IJavaProject project, Shell shell) throws CoreException {
		String sourceLocation = Plugin.getProperty(project.getProject(), Constants.PROP_PROJECT_SOURCE_LOCATION);
		IPackageFragmentRoot root = getProjectRoot(project, sourceLocation);
		if (null == root) {
			// we need to figure it out...
			List potentialRoots = new ArrayList();
			IPackageFragmentRoot[] roots = project.getAllPackageFragmentRoots();
			for (int i=0; i<roots.length; i++) {
				if (null != roots[i].getCorrespondingResource() && roots[i].getJavaProject().equals(project) && !roots[i].isArchive()) {
					potentialRoots.add(roots[i]);
				}
			}
			if (potentialRoots.size() == 0) return null;
			else if (potentialRoots.size() == 1) {
				root = (IPackageFragmentRoot) potentialRoots.get(0);
				Plugin.setProperty(project.getProject(), Constants.PROP_PROJECT_SOURCE_LOCATION, root.getPath().toOSString());
			}
			else {
				// we must be told what the root is because there is more than 1 to choose from
				if (null == shell) shell = new Shell();
				SourceLocationSelectorDialog dialog = new SourceLocationSelectorDialog(shell, potentialRoots, project.getProject());
				dialog.open();
				return dialog.getPackageFragmentRoot();
			}
		}
		return root;
	}
	
	private static IPackageFragmentRoot getProjectRoot (IJavaProject project, String sourceLocation) throws CoreException {
		if (null == sourceLocation) return null;
		IPackageFragmentRoot[] roots = project.getAllPackageFragmentRoots();
		for (int i=0; i<roots.length; i++) {
			if (null != roots[i].getCorrespondingResource() && roots[i].getJavaProject().equals(project) && !roots[i].isArchive()) {
				if (sourceLocation.equals(roots[i].getPath().toOSString())) {
					return roots[i];
				}
			}
		}
		return null;
	}
	
	public static Object load(ClassLoader loader, String className) {
		try {
			return loader.loadClass(className).newInstance();
		}
		catch (Throwable t) {
			return null;
		}
	}

	public static void showError (String error, Shell shell) {
		MessageDialog.openError(
				shell,
				null,
				error);
	}
	
	public static void showError (String title, String error, Shell shell) {
		if (null == title) title = "An error has occured";
		if (null == shell) shell = new Shell();
		MessageDialog.openError(
				shell,
				title,
				error);
	}

	public static MarkerContents getMarkerContents(String content, String marker) {
		int startIndex = content.indexOf("[" + marker + " BEGIN]");
		if (startIndex > 0) {
			char[] arr = content.toCharArray();
			char compCharN = '\n';
			char compCharR = '\r';
			while (startIndex < arr.length) {
				char c = arr[startIndex];
				if (c == compCharN) {
					if (arr[startIndex + 1] == compCharR) startIndex ++;
					startIndex ++;
					break;
				}
				startIndex ++;
			}
			if (startIndex < arr.length) {
				int endIndex = content.indexOf("[" + marker + " END]");
				if (endIndex > startIndex) {
					while (endIndex > startIndex) {
						char c = arr[endIndex];
						if (c == compCharN) {
							// if (arr[endIndex - 1] == compCharR) endIndex --;
							endIndex ++;
							break;
						}
						endIndex --;
					}
					String previousContent = content.substring(0, startIndex);
					String postContent = content.substring(endIndex, content.length());
					String midContent = content.substring(startIndex, endIndex);
					MarkerContents mc = new MarkerContents();
					mc.setPreviousContents(previousContent);
					mc.setPostContents(postContent);
					mc.setContents(midContent);
					mc.setStartPosition(startIndex);
					mc.setEndPosition(endIndex);
					return mc;
				}
			}
		}
		return null;
	}
	
	public static IFile getConfigFile (IProject project, boolean askUser, Shell shell) {
		IFile configFile = null;
		try {
			String configFileStr = Plugin.getProperty(project, Constants.PROP_CONFIGURATION_FILE);
			if (null != configFileStr) configFile = project.getWorkspace().getRoot().getFile(new Path(configFileStr));
			if (null != configFile && !configFile.exists()) configFile = null;
		}
		catch (Exception e) {}
		if (null == configFile && askUser) {
			ResourceSelectionDialog dialog =
				new ResourceSelectionDialog(
					null,
					project,
					"Select the hibernate configuration file");
			if (dialog.open() == ContainerSelectionDialog.OK) {
				Object[] result = dialog.getResult();
				if (result.length == 1) {
					configFile = (IFile) result[0];
					Plugin.setProperty(project, Constants.PROP_CONFIGURATION_FILE, configFile.getFullPath().toOSString());
				}
				else {
					HSUtil.showError("You must select only 1 configuration file", shell);
				}
			}
		}
		return configFile;
	}
	
	public static String getStaticName (String s) {
		boolean wasLastSpace = false;
		if (null == s) return null;
		if (s.toUpperCase().equals(s)) return s;
		else {
			StringBuffer sb = new StringBuffer();
			for (int i=0; i<s.toCharArray().length; i++) {
				char c = s.toCharArray()[i];
				if (Character.isLetterOrDigit(c)) {
					if (c == ' ' || c == '-') {
						sb.append("_");
						wasLastSpace = true;
					}
					else if (Character.isUpperCase(c) || i == 0) {
						if (sb.length() > 0 && !wasLastSpace) sb.append("_");
						sb.append(Character.toUpperCase(c));
						wasLastSpace = false;
					}
					else {
						sb.append(Character.toUpperCase(c));
						wasLastSpace = false;
					}
				}
				else if (Character.isWhitespace(c) || c == '.' || c == '-' || c == '_') {
					if (!wasLastSpace) {
						sb.append("_");
						wasLastSpace = true;
					}
				}
			}
			return sb.toString();
		}
	}

	/*
	public static void copyFile (File f1, File f2) throws IOException {
	    FileInputStream fis  = null;
	    FileOutputStream fos  = null;
	    try {
	    	if (!f2.exists()) f2.createNewFile();
	    	fis = new FileInputStream(f1);
	    	fos = new FileOutputStream(f2);
	    	byte[] buf = new byte[1024];
	    	int i = 0;
	    	while((i=fis.read(buf))!=-1) {
	    		fos.write(buf, 0, i);
	    	}
	    }
	    catch (IOException e) {
	    	Plugin.log("Can not copy " + f1.getAbsolutePath() + " to " + f2.getAbsolutePath());
	    	throw e;
	    }
	    finally {
	    	if (null != fis) fis.close();
	    	if (null != fos) fos.close();
	    }
	}
	*/

	/**
	 * Replace the contents of the from string with the contents of the to string in the base string
	 * @param base the string to replace part of
	 * @param from the string to be replaced
	 * @param to the string to replace
	 */
	public static String stringReplace( String base, String from, String to )
	{
		StringBuffer sb1 = new StringBuffer(base);
		StringBuffer sb2 = new StringBuffer(base.length() + 50);
		char[] f = from.toCharArray();
		char[] t = to.toCharArray();
		char[] test = new char[f.length];
		
		for (int x = 0; x < sb1.length(); x++) {
			
			if (x + test.length > sb1.length()) {
				sb2.append(sb1.charAt(x));
			} else {
				sb1.getChars(x, x + test.length, test, 0);
				if (aEquals(test, f)) {
					sb2.append(t);
					x = x + test.length - 1;		
				} else {
					sb2.append(sb1.charAt(x));
				}
			}
		}
		return sb2.toString();
	}
	
	static private boolean aEquals(char[] a, char[] b) {
		if (a.length != b.length) {
			return false;
		}
		for (int x = 0; x < a.length; x++) {
			if (a[x] != b[x]) {
				return false;
			}	
		}
		return true;	
	}
}