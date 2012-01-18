package com.hudson.hibernatesynchronizer.util;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.internal.editors.text.JavaFileEditorInput;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.MarkerUtilities;
import org.eclipse.ui.texteditor.TextEditorAction;

import com.hudson.hibernatesynchronizer.Plugin;
import com.hudson.hibernatesynchronizer.resource.Resource;
import com.hudson.hibernatesynchronizer.resource.ResourceEditorInput;


public class EditorUtil {

	public static int countMarkers (IFile file) throws CoreException {
		return file.findMarkers(null, false, IResource.DEPTH_ONE).length;
	}

	public static void addWarningMarker (IFile file, String message, int line) {
		addMarker(file, message, line, IMarker.SEVERITY_WARNING);
	}

	public static void addProblemMarker (IFile file, String message, int line) {
		addMarker(file, message, line, IMarker.SEVERITY_ERROR);
	}

	public static void addMarker(IFile file, String message, int line, int markerType) {
		try {
			Map attributes = new HashMap(5);
			attributes.put(IMarker.SEVERITY, new Integer(markerType));
			attributes.put(IMarker.MESSAGE, message);
			attributes.put(IMarker.TEXT, message);
			attributes.put(IMarker.LINE_NUMBER, new Integer(line));
			MarkerUtilities.createMarker(file, attributes, IMarker.PROBLEM);
		} catch (Exception e) {}
	}
	
	public static IEditorPart openPage (IFile file) {
		if (file != null) {
			try {
				IWorkbenchPage p = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				if (p != null) {
					IEditorPart editorPart= IDE.openEditor(p, file, true);
					initializeHighlightRange(editorPart);
					return editorPart;
				}
			}
			catch (PartInitException e) {
				return null;
			}
		}
		return null;
	}

	public static void openPage (Resource resource, Shell shell) {
		IFile iFile = resource.getIFile();
		IEditorInput input = new ResourceEditorInput(resource);
		String editorId = PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(resource.getFileName()).getId();
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		IWorkbenchPage page = window.getActivePage();
		try {
			page.openEditor(input, editorId);
		}
		catch (Exception e) {
			UIUtil.pluginError(e, shell);
		}
	}

	private static void initializeHighlightRange(IEditorPart editorPart) {
		if (editorPart instanceof ITextEditor) {
			IAction toggleAction= editorPart.getEditorSite().getActionBars().getGlobalActionHandler(ITextEditorActionDefinitionIds.TOGGLE_SHOW_SELECTED_ELEMENT_ONLY);
			if (toggleAction != null && toggleAction.isEnabled() && toggleAction.isChecked()) {
				if (toggleAction instanceof TextEditorAction) {
					// Reset the action 
					((TextEditorAction)toggleAction).setEditor(null);
					// Restore the action 
					((TextEditorAction)toggleAction).setEditor((ITextEditor)editorPart);
				} else {
					// Uncheck 
					toggleAction.run();
					// Check
					toggleAction.run();
				}
			}
		}
	}

	public static void openExternalFile (File file, IWorkbenchWindow window) throws PartInitException {
		try {
			if (null != file && file.exists()) {
				IEditorInput input = createEditorInput(file);
				String editorId = getEditorId(file, window);
				IWorkbenchPage page = window.getActivePage();
				page.openEditor(input, editorId);
			}
		}
		catch (Exception e) {
			Plugin.log(e);
		}
	}

	private static String getEditorId(File file, IWorkbenchWindow window) {
		IWorkbench workbench= window.getWorkbench();
		IEditorRegistry editorRegistry= workbench.getEditorRegistry();
		IEditorDescriptor descriptor= editorRegistry.getDefaultEditor(file.getName());
		if (descriptor != null)
			return descriptor.getId();
		return EditorsUI.DEFAULT_TEXT_EDITOR_ID;
	}

	private static IEditorInput createEditorInput(File file) {
		IFile workspaceFile= getWorkspaceFile(file);
		if (workspaceFile != null)
			return new FileEditorInput(workspaceFile);
		return new JavaFileEditorInput(file);
	}
	
	private static IFile getWorkspaceFile(File file) {
		IWorkspace workspace= ResourcesPlugin.getWorkspace();
		IPath location= new Path(file.getAbsolutePath());
		IFile[] files= workspace.getRoot().findFilesForLocation(location);
		if (files == null || files.length == 0)
			return null;
		if (files.length == 1)
			return files[0];
		else return null;
	}
}