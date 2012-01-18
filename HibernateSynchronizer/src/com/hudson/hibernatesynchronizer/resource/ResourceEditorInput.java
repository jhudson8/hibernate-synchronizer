package com.hudson.hibernatesynchronizer.resource;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.sourcelookup.containers.LocalFileStorage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.editors.text.ILocationProvider;


/**
 * @author Joe Hudson
 */
public class ResourceEditorInput implements IStorageEditorInput, IEditorInput {

	private File fFile;
	private IFile iFile;
	private IStorage storage;
	private Resource resource;

	public ResourceEditorInput(Resource resource) {
		this.resource = resource;
		if (null != resource.getFile()) {
			this.fFile = resource.getFile();
			storage = new EditableLocalFileStorage(fFile);
		}
		else if (null != resource.getIFile()) {
			this.iFile = resource.getIFile();
			storage = iFile;
		}
	}

	public File getFile () {
		return fFile;
	}

	public IFile getIFile () {
		return iFile;
	}
	
	public Resource getResource () {
		return resource;
	}

	/*
	 * @see org.eclipse.ui.IEditorInput#exists()
	 */
	public boolean exists() {
		return ((null != fFile &&fFile.exists()) || (null != iFile && iFile.exists()));
	}

	/*
	 * @see org.eclipse.ui.IEditorInput#getImageDescriptor()
	 */
	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	/*
	 * @see org.eclipse.ui.IEditorInput#getName()
	 */
	public String getName() {
		return resource.getName();
	}

	/*
	 * @see org.eclipse.ui.IEditorInput#getPersistable()
	 */
	public IPersistableElement getPersistable() {
		return null;
	}

	/*
	 * @see org.eclipse.ui.IEditorInput#getToolTipText()
	 */
	public String getToolTipText() {
		return resource.getName();
	}

	/*
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(Class adapter) {
		if (ILocationProvider.class.equals(adapter))
			return this;
		return Platform.getAdapterManager().getAdapter(this, adapter);
	}

	/*
	 * @see org.eclipse.ui.editors.text.ILocationProvider#getPath(java.lang.Object)
	 */
	public IPath getPath(Object element) {
		if (element instanceof ResourceEditorInput) {
			ResourceEditorInput input = (ResourceEditorInput) element;
			if (null != input.fFile)
				return new Path(input.fFile.getAbsolutePath());
			else if (null != input.iFile)
				return iFile.getFullPath();
		}
		return null;
	}
	
	/*
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (o instanceof ResourceEditorInput) {
			ResourceEditorInput input = (ResourceEditorInput) o;
			if (null != fFile && null != input.fFile)
				return fFile.equals(input.fFile);
			else if (null != iFile && null != input.iFile)
				return iFile.equals(input.iFile);
			else return false;
		}
		
		return false;
	}
	
	/*
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		if (null != fFile)
			return fFile.hashCode();
		else if (null != iFile)
			return iFile.hashCode();
		else return super.hashCode();
	}

	/**
	 * @see org.eclipse.ui.IStorageEditorInput#getStorage()
	 */
	public IStorage getStorage() throws CoreException {
		return storage;
	}

	public class EditableLocalFileStorage extends LocalFileStorage {
		/**
		 * @param file
		 */
		public EditableLocalFileStorage(File file) {
			super(file);
		}
		/**
		 * @see org.eclipse.core.resources.IStorage#isReadOnly()
		 */
		public boolean isReadOnly() {
			return false;
		}
	}
}