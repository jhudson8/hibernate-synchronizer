package com.hudson.hibernatesynchronizer;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;

import org.eclipse.core.internal.resources.ResourceException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.hudson.hibernatesynchronizer.resource.ResourceManager;
import com.hudson.hibernatesynchronizer.util.HSUtil;


/**
 * The main plugin class to be used in the desktop.
 */
public class Plugin extends AbstractUIPlugin {
	
	// TODO change the plugin id
	public static final String PLUGIN_ID = "com.hudson.hibernatesynchronizer";
	
	//The shared instance.
	private static Plugin plugin;
	//Resource bundle.
	private ResourceBundle resourceBundle;
	public static boolean devMode = false;
	
	/**
	 * The constructor.
	 */
	public Plugin() {
		super();
		plugin = this;

		try {
			resourceBundle = ResourceBundle.getBundle("com.hudson.hibernatesynchronizer.resources");
		} catch (MissingResourceException x) {
			resourceBundle = null;
		}
	}

	public boolean isDevMode () {
		return devMode;
	}
	
	/**
	 * This method is called upon plug-in activation
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		trace("\n\n\nLoading the plugin..." + new Date());
		try {
			URL url = getClass().getClassLoader().getResource("/hs-dev.test");
			if (null == url) devMode = true;
			else devMode = false;
			trace("DEV MODE: " + devMode);
			ResourceManager.initializePluginResources(devMode);
		}
		catch (Exception e) {
			Plugin.log(e);
		}
	}

	/**
	 * This method is called when the plug-in is stopped
	 */
	public void stop(BundleContext context) throws Exception {
		super.stop(context);
	}

	/**
	 * Returns the shared instance.
	 */
	public static Plugin getDefault() {
		return plugin;
	}

	public File getActualStateLocation () {
		return getStateLocation().makeAbsolute().toFile();
	}

	/**
	 * Returns the string from the plugin's resource bundle,
	 * or 'key' if not found.
	 */
	public static String getResourceString(String key) {
		ResourceBundle bundle = Plugin.getDefault().getResourceBundle();
		try {
			return (bundle != null) ? bundle.getString(key) : key;
		} catch (MissingResourceException e) {
			return key;
		}
	}

	/**
	 * Returns the plugin's resource bundle,
	 */
	public ResourceBundle getResourceBundle() {
		return resourceBundle;
	}

	protected void initializeImageRegistry(ImageRegistry reg) {
		reg.put("nav_class", getImageDescriptor("nav_class.gif"));
		reg.put("nav_key", getImageDescriptor("nav_key.gif"));
		reg.put("nav_list", getImageDescriptor("nav_list.gif"));
		reg.put("nav_property", getImageDescriptor("nav_property.gif"));
		reg.put("nav_property_required", getImageDescriptor("nav_property_required.gif"));
		reg.put("nav_query", getImageDescriptor("nav_query.gif"));
		reg.put("nav_component", getImageDescriptor("nav_component.gif"));
		reg.put("nav_many_to_one", getImageDescriptor("nav_many_to_one.gif"));
		reg.put("nav_many_to_one_required", getImageDescriptor("nav_many_to_one_required.gif"));
		reg.put("nav_one_to_one", getImageDescriptor("nav_one_to_one.gif"));
		reg.put("nav_one_to_one_required", getImageDescriptor("nav_one_to_one_required.gif"));
		reg.put("nav_required", getImageDescriptor("nav_required.gif"));
		reg.put("template", getImageDescriptor("template.gif"));
		reg.put("snippet", getImageDescriptor("snippet.gif"));
	}

	public ImageDescriptor getImageDescriptor(String key) {
		URL url = null;
		try {
			url = new URL(getDescriptor().getInstallURL(),
					"icons/" + key);
		} catch (MalformedURLException e) {
		}
		return ImageDescriptor.createFromURL(url);
	}

	private static Properties projectProperties = new Properties();
	private static IProject currentProject;
	private static synchronized Properties getProjectProperties (IProject project) {
		if (null == currentProject || !project.equals(currentProject)) {
			// reload properties for project
			projectProperties = new Properties();
			IFile file = project.getFile(new Path(Constants.FOLDER_HS));
			if (file.exists()) {
				try {
					file.delete(true, true, null);
				}
				catch (CoreException e) {}
			}
			IFolder folder = project.getFolder(new Path(Constants.FOLDER_HS));
			try {
				if (!folder.exists()) folder.create(true, true, null);
			}
			catch (CoreException e) {}
			file = project.getFile(new Path(Constants.FILE_HS));
			try {
				if (!file.exists()) {
					file.create(new ByteArrayInputStream("".getBytes()), true, null);
				}
				try {
					projectProperties.load(file.getContents());
				}
				catch (ResourceException re) {
					// resource might be out of sync
					file.refreshLocal(1, null);
					projectProperties.load(file.getContents());
				}
				currentProject = project;
			}
			catch (Exception e) {
				log(e);
			}
		}
		return projectProperties;
	}

	public static String getProperty (IAdaptable project, String propName) {
		if (project instanceof IProject)
			return getProperty((IProject) project, propName);
		else
			return null;
	}

	public static String getProperty (IProject project, String propName) {
		return getProjectProperties(project).getProperty(propName);
	}
	public static void setProperty (IProject project, String propName, String propValue) {
		getProjectProperties(project).setProperty(propName, propValue);
		saveProperties(project);
	}

	public static void setProperty (IAdaptable project, String propName, String propValue) {
		if (project instanceof IProject) {
			setProperty((IProject) project, propName, propValue);
			saveProperties((IProject) project);
		}
	}

	public static void clearProperty (IProject project, String propName) {
		getProjectProperties(project).remove(propName);
		saveProperties(project);
	}

	public static void clearProperty (IAdaptable project, String propName) {
		if (project instanceof IProject) {
			clearProperty((IProject) project, propName);
			saveProperties((IProject) project);
		}
	}
	
	private static void saveProperties (IProject project) {
		StringBuffer sb = new StringBuffer();
		for (Iterator i=getProjectProperties(project).entrySet().iterator(); i.hasNext(); ) {
			Map.Entry entry = (Map.Entry) i.next();
			sb.append((String) entry.getKey());
			sb.append("=");
			sb.append(checkEntry((String) entry.getValue()));
			sb.append("\n");
		}
		IFile file = project.getFile(new Path(Constants.FILE_HS));
		try {
			if (!file.exists()) {
				file.create(new ByteArrayInputStream(sb.toString().getBytes()), true, null);
			}
			else {
				String contents = HSUtil.getStringFromStream(file.getContents());
				if (!contents.equals(sb.toString())) {
					file.setContents(new ByteArrayInputStream(sb.toString().getBytes()), true, false, null);
				}
			}
		}
		catch (Exception e) {
			log(e);
		}
	}
	
	private static String checkEntry (String s) {
		if (null == s) return s;
		StringBuffer sb = new StringBuffer();
		char[] cArr = s.toCharArray();
		for (int i=0; i<cArr.length; i++) {
			if (cArr[i] == '\\') sb.append("\\\\");
			else sb.append(cArr[i]);
		}
		return sb.toString();
	}

	public static boolean getBooleanProperty (IProject project, String propertyName, boolean defaultValue) {
		String s = getProperty(project, propertyName);
		if (null == s) return defaultValue;
		else return s.toUpperCase().startsWith("T");
	}
	
	public static void log (Throwable t) {
		t.printStackTrace();
        log(null, t);
	}

	public static void log (String message, Throwable t) {
        StringWriter sw = new StringWriter();
        sw.write("\n------------------ " + new Date().toString() + "\n");
        if (null != message) sw.write(message + "\n");
        t.printStackTrace(new PrintWriter(sw));
        log(sw.toString(), true);
	}

	public static void log (String message) {
        log(message, false);
	}

	public static void log (String message, boolean error) {
	    if (getDefault().isDevMode()) {
	        if (error) System.err.println(message);
	        else System.out.println(message);
	    }
	    else {
	        FileOutputStream fos = null;
	        try {
	            String logFileName = Platform.resolve(
	                    plugin.getDescriptor().getInstallURL()).getFile()
	                    + "error.log";
	            fos = new FileOutputStream(logFileName, true);
	            fos.write(message.getBytes());
	            fos.write("\n".getBytes());
	        } catch (Exception e) {
	        } finally {
	            if (null != fos) {
	                try {
	                    fos.close();
	                } catch (Exception e) {
	                }
	            }
	        }
	    }
	}

	public static void trace (Throwable t) {
	    if (getDefault().isDevMode()) {
	        log(t);
	    }
	}

	public static void trace (String s) {
	    if (getDefault().isDevMode()) {
	        log(s);
	    }
	}
}