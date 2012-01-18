package com.hudson.hibernatesynchronizer.editors.synchronizer.actions;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.NoRouteToHostException;
import java.util.HashMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

import com.hudson.hibernatesynchronizer.Constants;
import com.hudson.hibernatesynchronizer.Plugin;
import com.hudson.hibernatesynchronizer.exceptions.HibernateSynchronizerException;
import com.hudson.hibernatesynchronizer.util.EditorUtil;
import com.hudson.hibernatesynchronizer.util.HSUtil;
import com.hudson.hibernatesynchronizer.util.HibernateDOMParser;
import com.hudson.hibernatesynchronizer.util.XMLFormatter;

public class AddMappingReference implements IObjectActionDelegate {

	private IWorkbenchPart part;

	/**
	 * Constructor for Action1.
	 */
	public AddMappingReference() {
		super();
	}

	/**
	 * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		this.part = targetPart;
	}

	/**
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(IAction action) {
		Shell shell = new Shell();
		ISelectionProvider provider = part.getSite().getSelectionProvider();
		if (null != provider) {
			if (provider.getSelection() instanceof StructuredSelection) {
				StructuredSelection  selection = (StructuredSelection) provider.getSelection();
				Object[] obj = selection.toArray();
				IFile[] files = new IFile[obj.length];
				IProject singleProject = null;
				boolean isSingleProject = true;
				for (int i=0; i<obj.length; i++) {
					if (obj[i] instanceof IFile) {
						IFile file = (IFile) obj[i];
						files[i] = file;
						if (null == singleProject) singleProject = file.getProject();
						if (!singleProject.getName().equals(file.getProject().getName())) {
							isSingleProject = false;
						}
					}
				}
				if (isSingleProject) {
					addMappingReference (singleProject, files, true, shell);
				}
				else {
					HSUtil.showError("The selected files must belong to a single project", shell);
				}
			}
		}
	}

	public static void addMappingReference (IProject singleProject, IFile[] files, boolean force, Shell shell) {
		IFile configFile = HSUtil.getConfigFile(singleProject, force, shell);
		if (null != configFile) {
			
			HibernateDOMParser domParser = null;
			try {
				configFile.deleteMarkers(null, true, IResource.DEPTH_ONE);
				domParser = new HibernateDOMParser();
				domParser.parse(new InputSource(configFile.getContents()));
				Document doc = domParser.getDocument();
				Node node = doc.getDocumentElement();
				Node sessionFactoryNode = null;
				NodeList nodes = node.getChildNodes();
				for (int i=0; i<nodes.getLength(); i++) {
					Node subNode = nodes.item(i);
					if (subNode.getNodeName().equals("session-factory")) sessionFactoryNode = subNode;
				}
				if (null != sessionFactoryNode) {
					HashMap existingMappings = new HashMap();
					Node lastMapping = null;
					nodes = sessionFactoryNode.getChildNodes();
					for (int i=0; i<nodes.getLength(); i++) {
						Node subNode = nodes.item(i);
						if (subNode.getNodeName().equals("mapping")) {
							NamedNodeMap attributes = subNode.getAttributes();
							Node mapName = attributes.getNamedItem("resource");
							if (null != mapName && mapName.getNodeValue().trim().length() > 0) {
								existingMappings.put(mapName.getNodeValue().trim().replace('\\', '/'), subNode);
								lastMapping = subNode;
							}
						}
					}
					
					for (int i=0; i<files.length; i++) {
						String directoryDifference = getDirectoryDifference(configFile.getLocation().toOSString(), files[i].getLocation().toOSString());
						String mappingName = null;
						if (null != directoryDifference && directoryDifference.trim().length() > 0) {
							mappingName = directoryDifference + "/" + files[i].getName();
						}
						else {
							mappingName = files[i].getName();
						}
						if (null == existingMappings.get(mappingName) && null == existingMappings.get("/" + mappingName)) {
							lastMapping = addMapping (mappingName, sessionFactoryNode, lastMapping, doc);
						}
					}
					String contents = getDocumentContents(doc, domParser, configFile.getContents());
					configFile.setContents(new ByteArrayInputStream(contents.getBytes()), true, true, null);
					EditorUtil.openPage(configFile);
				}
				else {
					HSUtil.showError("The session-factory node could not be located", null);
				}
			}
			catch (SAXParseException e) {
				Plugin.clearProperty(singleProject, Constants.PROP_CONFIGURATION_FILE);
				HSUtil.showError(e.getMessage(), null);
			}
			catch (HibernateSynchronizerException e) {
				Plugin.clearProperty(singleProject, Constants.PROP_CONFIGURATION_FILE);
				int lineNumber = e.getLineNumber();
				if (null != e.getNode() && null != domParser.getLineNumber(e.getNode())) {
					lineNumber = domParser.getLineNumber(e.getNode()).intValue();
				}
				if (lineNumber <= 0) {
					lineNumber = 1;
				}
				EditorUtil.addProblemMarker(configFile, e.getMessage(), lineNumber);
			}
			catch (NoRouteToHostException e) {
				Plugin.clearProperty(singleProject, Constants.PROP_CONFIGURATION_FILE);
				EditorUtil.addProblemMarker(configFile, "A NoRouteToHostException occured while process the synchronization.  Either remove the external namespace and DTD definitions or connect to the internet (or configure your proxy).", 1);
				MessageDialog.openError(shell, "An error has occured: NoRouteToHostException", "This usually occurs if you have namespace references or DTD validation and are either not connected to the internet or do not have your proxy setting correctly configured.\n\nPlease resolve these issues to use the HibernateSynchronizer plugin.");
			}
			catch (Exception e) {
				Plugin.clearProperty(singleProject, Constants.PROP_CONFIGURATION_FILE);
				HSUtil.showError(e.getMessage(), null);
			}
		}
	}

	/**
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
	}

	private static Node addMapping (String mappingName, Node configNode, Node lastMappingNode, Document doc) {
		Element newElement = doc.createElement("mapping");
		newElement.setAttribute("resource", mappingName);
		configNode.appendChild(newElement);
		return newElement;
	}

	private static String getFileDirectory (String fileName) {
		if (null == fileName) return null;
		fileName = fileName.replace('\\', '/');
		int index = fileName.lastIndexOf('/');
		if (index >= 0) {
			return fileName.substring(0, index);
		}
		else return fileName;
	}
	
	private static String getDirectoryDifference (String configFile, String mapFile) {
		String confDir = getFileDirectory(configFile);
		String mapDir = getFileDirectory(mapFile);
		if (mapDir.startsWith(confDir)) {
			if (mapDir.length() > confDir.length()) {
				return mapDir.substring(confDir.length() + 1, mapDir.length()).replace('\\', '/');
			}
			else {
				return "";
			}
		}
		else {
			return null;
		}
	}

	public static String getDocumentContents (Document doc, HibernateDOMParser parser, InputStream contents) throws Exception
	{
		StringWriter sw = new StringWriter();
		XMLFormatter formatter = new XMLFormatter(new PrintWriter(sw));
		formatter.printTree(doc.getDocumentElement(), "");
		return sw.toString();
	}
}