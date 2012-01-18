package com.hudson.hibernatesynchronizer.properties;

/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2001 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Velocity", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.internal.core.JavaProject;
import org.eclipse.jdt.internal.core.PackageFragment;
import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ColumnLayoutData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.ui.dialogs.SelectionDialog;

import com.hudson.hibernatesynchronizer.Constants;
import com.hudson.hibernatesynchronizer.Plugin;
import com.hudson.hibernatesynchronizer.resource.ResourceManager;
import com.hudson.hibernatesynchronizer.resource.Snippet;
import com.hudson.hibernatesynchronizer.resource.Template;
import com.hudson.hibernatesynchronizer.resource.TemplateLocation;
import com.hudson.hibernatesynchronizer.util.EditorUtil;
import com.hudson.hibernatesynchronizer.util.HSUtil;
import com.hudson.hibernatesynchronizer.util.UIUtil;

/**
 * @author <a href="mailto: jhudson8@users.sourceforge.net">Joe Hudson</a>
 * 
 * If anyone is looking at this page, you probably don't want to use it as an
 * example if you are learning how to create a plugin for yourself.  I just started
 * learning and I know that I am doing things the incorrect way but I just wasn't able
 * to find out how to do them the correct way.  I'll be working on the quality of this
 * code... and documenting.
 */
public class HibernateProperties extends PropertyPage {


	private Combo sourceLocation;
	private BooleanFieldEditor generationEnabled;
	private BooleanFieldEditor boEnabled;
	private Text basePackageText;
	private RadioGroupFieldEditor basePackageStyle;
	private String basePackageStyleSelection;
	private Label basePackageHelp;
	private Label basePackageNameLbl;
	private Group baseGroup;

	private Text managerPackageText;
	private RadioGroupFieldEditor managerPackageStyle;
	private String managerPackageStyleSelection;
	private Label managerPackageHelp;
	private Label managerPackageNameLbl;
	private BooleanFieldEditor managersEnabled;
	private Group managerGroup;
	private Label baseManagerPackageNameLbl;
	private RadioGroupFieldEditor baseManagerPackageStyle;
	private Text baseManagerPackageText;
	private Group baseManagerGroup;
	private Composite baseManagerPackageStyleSelectionParent;
	private String baseManagerPackageStyleSelection;
	private BooleanFieldEditor managerUseBasePackage;
	private BooleanFieldEditor baseManagerPackage;
	private Button baseManagerPackageSelectionButton;
	private BooleanFieldEditor customManager;
	private Composite managerRootComposite;
	private Text daoRootClass;
	private Text daoExceptionClass;
	private Composite customManagerComposite;
	private Button basePackageSelectionButton;
	private Button managerPackageSelectionButton;

	private Table templateTable;
	private Table parametersTable;
	private Table snippetTable;
	
	private IPreferenceStore preferenceStore;
	private Button editButton;
	private Button deleteButton;
	private Button addButton;
	private Button importButton;
	private Button exportButton;
	private Button selectAllButton;
	private Button selectNoneButton;
	private Button editParameterButton;
	private Button deleteParameterButton;
	private Button addParameterButton;
	private Text contextObject;
	private Button editSnippetButton;
	private Button deleteSnippetButton;
	
	/**
	 * Constructor for SamplePropertyPage.
	 */
	public HibernateProperties() {
		super();
	}

	/**
	 * @see PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {
		setPreferenceStore(preferenceStore);
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(1, true));
		composite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_BOTH));
		((GridData) composite.getLayoutData()).grabExcessHorizontalSpace = true;
		((GridData) composite.getLayoutData()).grabExcessVerticalSpace = true;

		TabFolder folder = new TabFolder(composite, SWT.NONE);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.grabExcessHorizontalSpace = true;
		data.grabExcessVerticalSpace = true;
		folder.setLayoutData(data);
		TabItem item = new TabItem(folder, SWT.NONE);
		item.setText("General");
		composite = createDefaultComposite(folder);
		item.setControl(composite);
		addGeneral(composite);

		item = new TabItem(folder, SWT.NONE);
		item.setText("Templates");
		composite = createDefaultComposite(folder);
		item.setControl(composite);
		addTemplates(composite);

		item = new TabItem(folder, SWT.NONE);
		item.setText("Snippets");
		composite = createDefaultComposite(folder);
		item.setControl(composite);
		addSnippets(composite);
		
		checkScreen();
		return parent;
	}
	
	private void addGeneral(Composite parent) {		

		Composite subComp = new Composite(parent, SWT.NULL);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		subComp.setLayoutData(gd);
		generationEnabled = new BooleanFieldEditor(Constants.PROP_GENERATION_ENABLED, "I would like to have the synchronization performed automatically.", subComp);
		generationEnabled.setPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				checkScreen();
			}
		});
		generationEnabled.setPreferenceStore(getLocalPreferenceStore());
		generationEnabled.load();

		Label label = new Label(subComp, SWT.NULL);
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		IJavaProject javaProject = (JavaProject) JavaCore.create((IProject) getElement());
		List sourceRoots = new ArrayList();
		try {
			String value = Plugin.getProperty(getElement(), Constants.PROP_SOURCE_LOCATION);
			IPackageFragmentRoot[] roots = javaProject.getAllPackageFragmentRoots();
			for (int i=0; i<roots.length; i++) {
				try {
					if (null != roots[i].getCorrespondingResource() && roots[i].getJavaProject().equals(javaProject) && !roots[i].isArchive()) {
						sourceRoots.add(roots[i].getPath().toOSString());
					}
				}
				catch (JavaModelException jme) {}
			}
			if (sourceRoots.size() > 1) {
				Label pathLabel = new Label(parent, SWT.NONE);
				pathLabel.setText("Source Location");
				sourceLocation = new Combo(parent, SWT.READ_ONLY);
				int selection = 0;
				for (int i=0; i<sourceRoots.size(); i++) {
					sourceLocation.add(sourceRoots.get(i).toString());
					if (null != value && value.equals(sourceRoots.get(i).toString())) {
						selection = i;
					}
				}
				sourceLocation.select(selection);
			}
		}
		catch (Exception e) {}
		
		TabFolder folder = new TabFolder(parent, SWT.NONE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		gd.grabExcessHorizontalSpace = true;
		folder.setLayoutData(gd);
		TabItem item = new TabItem(folder, SWT.NONE);
		item.setText("Value Objects");
		Composite composite = new Composite(folder, SWT.NULL);
		GridLayout gl = new GridLayout(1, false);
		composite.setLayout(gl);
		gd = new GridData();
		gd.grabExcessHorizontalSpace = true;
		composite.setLayoutData(gd);
		item.setControl(composite);
		
		Composite tComp = new Composite(composite, SWT.NONE);
		boEnabled = new BooleanFieldEditor(Constants.PROP_GENERATION_VALUE_OBJECT_ENABLED, "I would like to have value objects created for me.", tComp);
		boEnabled.setPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				checkScreen();
			}
		});
		boEnabled.setPreferenceStore(getLocalPreferenceStore());
		boEnabled.load();
		
		// Path text field
		baseGroup = new Group(composite, SWT.NULL);
		baseGroup.setText("Base Package Location");
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 1;
		baseGroup.setLayout(gridLayout);
		baseGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));

		String[][] params = new String[3][2];
		params[0][0] = Constants.PROP_VALUE_RELATIVE;
		params[0][1] = Constants.PROP_VALUE_RELATIVE;
		params[1][0] = Constants.PROP_VALUE_ABSOLUTE;
		params[1][1] = Constants.PROP_VALUE_ABSOLUTE;
		params[2][0] = "Same Package";
		params[2][1] = Constants.PROP_VALUE_SAME;
		basePackageStyle = new RadioGroupFieldEditor(
			Constants.PROP_BASE_VO_PACKAGE_STYLE,
			"",
			3,
			params,
			baseGroup);
		basePackageStyle.setPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				basePackageStyleSelection = event.getNewValue().toString();
				checkScreen();
			}
		});
		basePackageStyle.setPreferenceStore(getLocalPreferenceStore());
		basePackageStyle.load();
		
		Composite bgc1 = new Composite(baseGroup, SWT.NONE);
		gridLayout = new GridLayout();
		gridLayout.numColumns = 3;
		bgc1.setLayout(gridLayout);
		bgc1.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));

		basePackageHelp = new Label(bgc1, SWT.NULL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 3;
		basePackageHelp.setLayoutData(gd);

		basePackageNameLbl = new Label(bgc1, SWT.NULL);
		basePackageNameLbl.setText("Package:");
		basePackageText = new Text(bgc1, SWT.SINGLE | SWT.BORDER);
		basePackageText.setSize(50, 12);
		gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL);
		gd.widthHint=230;
		basePackageText.setLayoutData(gd);
		String basePackageString = Plugin.getProperty(getElement(), Constants.PROP_BASE_VO_PACKAGE_NAME);
		if (null != basePackageString) basePackageText.setText(basePackageString);
		else basePackageText.setText(Constants.DEFAULT_BASE_VO_PACKAGE);

		basePackageSelectionButton = new Button(bgc1, SWT.NATIVE);
		basePackageSelectionButton.setText("Browse");
		basePackageSelectionButton.addMouseListener(new MouseListener() {
			public void mouseDown(MouseEvent e) {
				try {
					JavaProject javaProject = (JavaProject) JavaCore.create((IProject) getElement());
					IJavaSearchScope searchScope = SearchEngine.createWorkspaceScope();
					SelectionDialog sd = JavaUI.createPackageDialog(getShell(), javaProject, IJavaElementSearchConstants.CONSIDER_REQUIRED_PROJECTS);
					sd.open();
					Object[] objects = sd.getResult();
					if (null != objects && objects.length > 0) {
						PackageFragment pf = (PackageFragment) objects[0];
						basePackageText.setText(pf.getElementName());
					}
				}
				catch (JavaModelException jme) {
					jme.printStackTrace();
				}
			}
			public void mouseDoubleClick(MouseEvent e) {}
			public void mouseUp(MouseEvent e) {}

		});

		item = new TabItem(folder, SWT.NONE);
		item.setText("Data Access Objects");
		composite = new Composite(folder, SWT.NULL);
		gl = new GridLayout(1, false);
		composite.setLayout(gl);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.grabExcessHorizontalSpace = true;
		composite.setLayoutData(gd);
		item.setControl(composite);

		tComp = new Composite(composite, SWT.NONE);
		managersEnabled = new BooleanFieldEditor(Constants.PROP_GENERATION_DAO_ENABLED, "I would like to have DAOs created for me.", tComp);
		managersEnabled.setPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				checkScreen();
			}
		});
		managersEnabled.setPreferenceStore(getLocalPreferenceStore());
		managersEnabled.load();

		new Label(parent, SWT.NULL);

		// Path text field
		managerGroup = new Group(composite, SWT.NULL);
		managerGroup.setText("Base Package Location");
		gridLayout = new GridLayout();
		gridLayout.numColumns = 1;
		managerGroup.setLayout(gridLayout);
		managerGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		params = new String[3][2];
		params[0][0] = Constants.PROP_VALUE_RELATIVE;
		params[0][1] = Constants.PROP_VALUE_RELATIVE;
		params[1][0] = Constants.PROP_VALUE_ABSOLUTE;
		params[1][1] = Constants.PROP_VALUE_ABSOLUTE;
		params[2][0] = "Same Package";
		params[2][1] = Constants.PROP_VALUE_SAME;
		managerPackageStyle = new RadioGroupFieldEditor(
				Constants.PROP_DAO_PACKAGE_STYLE,
				"",
				3,
				params,
				managerGroup);
		managerPackageStyle.setPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				managerPackageStyleSelection = event.getNewValue().toString();
				checkScreen();
			}
		});
		managerPackageStyle.setPreferenceStore(getLocalPreferenceStore());
		managerPackageStyle.load();

		bgc1 = new Composite(managerGroup, SWT.NONE);
		gridLayout = new GridLayout();
		gridLayout.numColumns = 3;
		bgc1.setLayout(gridLayout);
		bgc1.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		managerPackageHelp = new Label(bgc1, SWT.NULL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 3;
		managerPackageHelp.setLayoutData(gd);

		managerPackageNameLbl = new Label(bgc1, SWT.NULL);
		managerPackageNameLbl.setText("Package:");
		managerPackageText = new Text(bgc1, SWT.SINGLE | SWT.BORDER);
		managerPackageText.setSize(50, 12);
		gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
		gd.widthHint=230;
		managerPackageText.setLayoutData(gd);
		String managerPackageString = Plugin.getProperty(getElement(), Constants.PROP_DAO_PACKAGE_NAME);
		if (null != managerPackageString) managerPackageText.setText(managerPackageString);
		else managerPackageText.setText(Constants.DEFAULT_DAO_PACKAGE);

		managerPackageSelectionButton = new Button(bgc1, SWT.NATIVE);
		managerPackageSelectionButton.setText("Browse");
		managerPackageSelectionButton.addMouseListener(new MouseListener() {
			public void mouseDown(MouseEvent e) {
				try {
					JavaProject javaProject = (JavaProject) JavaCore.create((IProject) getElement());
					IJavaSearchScope searchScope = SearchEngine.createWorkspaceScope();
					SelectionDialog sd = JavaUI.createPackageDialog(getShell(), javaProject, IJavaElementSearchConstants.CONSIDER_REQUIRED_PROJECTS);
					sd.open();
					Object[] objects = sd.getResult();
					if (null != objects && objects.length > 0) {
						PackageFragment pf = (PackageFragment) objects[0];
						managerPackageText.setText(pf.getElementName());
					}
				}
				catch (JavaModelException jme) {
					jme.printStackTrace();
				}
			}
			public void mouseDoubleClick(MouseEvent e) {}
			public void mouseUp(MouseEvent e) {}

		});

		subComp = new Composite(bgc1, SWT.NULL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 3;
		subComp.setLayoutData(gd);
		managerUseBasePackage = new BooleanFieldEditor(Constants.PROP_BASE_DAO_USE_BASE_PACKAGE, "I would like the base DAOs in the base value object package", subComp);
		managerUseBasePackage.setPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				checkScreen();
			}
		});
		managerUseBasePackage.setPreferenceStore(getLocalPreferenceStore());
		managerUseBasePackage.load();

		baseManagerPackageStyleSelectionParent = new Composite(bgc1, SWT.NULL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 3;
		baseManagerPackageStyleSelectionParent.setLayoutData(gd);
		params = new String[3][2];
		params[0][0] = Constants.PROP_VALUE_RELATIVE;
		params[0][1] = Constants.PROP_VALUE_RELATIVE;
		params[1][0] = Constants.PROP_VALUE_ABSOLUTE;
		params[1][1] = Constants.PROP_VALUE_ABSOLUTE;
		params[2][0] = "DAO Package";
		params[2][1] = Constants.PROP_VALUE_SAME;
		baseManagerPackageStyle = new RadioGroupFieldEditor(
				Constants.PROP_BASE_DAO_PACKAGE_STYLE,
				"Base DAO Location",
				3,
				params,
		baseManagerPackageStyleSelectionParent);
		baseManagerPackageStyle.setPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				baseManagerPackageStyleSelection = event.getNewValue().toString();
				checkScreen();
			}
		});
		baseManagerPackageStyle.setPreferenceStore(getLocalPreferenceStore());
		baseManagerPackageStyle.load();

		baseManagerPackageNameLbl = new Label(bgc1, SWT.NULL);
		baseManagerPackageNameLbl.setText("Package:");
		baseManagerPackageText = new Text(bgc1, SWT.SINGLE | SWT.BORDER);
		baseManagerPackageText.setSize(50, 12);
		gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
		gd.widthHint=230;
		baseManagerPackageText.setLayoutData(gd);
		String baseManagerPackageString = Plugin.getProperty(getElement(), Constants.PROP_BASE_DAO_PACKAGE_NAME);
		if (null != baseManagerPackageString) baseManagerPackageText.setText(baseManagerPackageString);
		else baseManagerPackageText.setText(Constants.DEFAULT_BASE_DAO_PACKAGE);

		baseManagerPackageSelectionButton = new Button(bgc1, SWT.NATIVE);
		baseManagerPackageSelectionButton.setText("Browse");
		baseManagerPackageSelectionButton.addMouseListener(new MouseListener() {
			public void mouseDown(MouseEvent e) {
				try {
					JavaProject javaProject = (JavaProject) JavaCore.create((IProject) getElement());
					IJavaSearchScope searchScope = SearchEngine.createWorkspaceScope();
					SelectionDialog sd = JavaUI.createPackageDialog(getShell(), javaProject, IJavaElementSearchConstants.CONSIDER_REQUIRED_PROJECTS);
					sd.open();
					Object[] objects = sd.getResult();
					if (null != objects && objects.length > 0) {
						PackageFragment pf = (PackageFragment) objects[0];
						baseManagerPackageText.setText(pf.getElementName());
					}
				}
				catch (JavaModelException jme) {
					jme.printStackTrace();
				}
			}
			public void mouseDoubleClick(MouseEvent e) {}
			public void mouseUp(MouseEvent e) {}

		});


		customManagerComposite = new Composite(composite, SWT.NONE);
		customManager = new BooleanFieldEditor(Constants.PROP_USE_CUSTOM_ROOT_DAO, "I would like to use a custom DAO root.", customManagerComposite);
		customManager.setPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				checkScreen();
			}
		});
		customManager.setPreferenceStore(getLocalPreferenceStore());
		customManager.load();

		managerRootComposite = new Composite(composite, SWT.NONE);
		managerRootComposite.setLayout(new GridLayout(3, false));
		((GridLayout) managerRootComposite.getLayout()).horizontalSpacing = 9;
		gd = new GridData(GridData.FILL_HORIZONTAL);;
		gd.grabExcessHorizontalSpace = true;
		managerRootComposite.setLayoutData(gd);
		Label lbl = new Label(managerRootComposite, SWT.NONE);
		lbl.setText("DAO Class:");
		daoRootClass = new Text(managerRootComposite, SWT.SINGLE | SWT.BORDER);
		String managerRootClassStr = Plugin.getProperty(getElement(), Constants.PROP_CUSTOM_ROOT_DAO_CLASS);
		if (null != managerRootClassStr) daoRootClass.setText(managerRootClassStr);

		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.grabExcessHorizontalSpace = true;
		daoRootClass.setLayoutData(gd);
		Button managerRootButton = new Button(managerRootComposite, SWT.NATIVE);
		managerRootButton.setText("Browse");
		managerRootButton.addMouseListener(new MouseListener() {
			public void mouseDown(MouseEvent e) {
				try {
					JavaProject javaProject = (JavaProject) JavaCore.create((IProject) getElement());
					IJavaSearchScope searchScope = SearchEngine.createWorkspaceScope();
					SelectionDialog sd = JavaUI.createTypeDialog(getShell(), new ApplicationWindow(getShell()), searchScope, IJavaElementSearchConstants.CONSIDER_CLASSES, false);
					sd.open();
					Object[] objects = sd.getResult();
					if (null != objects && objects.length > 0) {
						IType type = (IType) objects[0];
						daoRootClass.setText(type.getFullyQualifiedName());
					}
				}
				catch (JavaModelException jme) {
					jme.printStackTrace();
				}
			}
			public void mouseDoubleClick(MouseEvent e) {}
			public void mouseUp(MouseEvent e) {}

		});

		lbl = new Label(managerRootComposite, SWT.NONE);
		lbl.setText("DAO Exception:");
		daoExceptionClass = new Text(managerRootComposite, SWT.SINGLE | SWT.BORDER);
		String daoExceptionClassStr = Plugin.getProperty(getElement(), Constants.PROP_BASE_DAO_EXCEPTION);
		if (null != daoExceptionClassStr) daoExceptionClass.setText(daoExceptionClassStr);
		else daoExceptionClass.setText("net.sf.hibernate.HibernateException");
	
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.grabExcessHorizontalSpace = true;
		daoExceptionClass.setLayoutData(gd);
		Button daoExceptionButton = new Button(managerRootComposite, SWT.NATIVE);
		daoExceptionButton.setText("Browse");
		daoExceptionButton.addMouseListener(new MouseListener() {
			public void mouseDown(MouseEvent e) {
				try {
					JavaProject javaProject = (JavaProject) JavaCore.create((IProject) getElement());
					IJavaSearchScope searchScope = SearchEngine.createWorkspaceScope();
					SelectionDialog sd = JavaUI.createTypeDialog(getShell(), new ApplicationWindow(getShell()), searchScope, IJavaElementSearchConstants.CONSIDER_CLASSES, false);
					sd.open();
					Object[] objects = sd.getResult();
					if (null != objects && objects.length > 0) {
						IType type = (IType) objects[0];
						daoExceptionClass.setText(type.getFullyQualifiedName());
					}
				}
				catch (JavaModelException jme) {
					jme.printStackTrace();
				}
			}
			public void mouseDoubleClick(MouseEvent e) {}
			public void mouseUp(MouseEvent e) {}
	
		});
	}

	private void addTemplates(Composite parent) {
		Composite tComp = new Composite(parent, SWT.NULL);
		GridLayout gl = new GridLayout(1, false);
		gl.horizontalSpacing = 0;
		gl.verticalSpacing = 0;
		tComp.setLayout(gl);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.grabExcessHorizontalSpace = true;
		gd.grabExcessVerticalSpace = true;
		tComp.setLayoutData(gd);
		
		Composite composite = new Composite(tComp, SWT.NULL);
		gl = new GridLayout(2, false);
		composite.setLayout(gl);
		gd = new GridData(GridData.FILL_BOTH);
		gd.grabExcessHorizontalSpace = true;
		gd.grabExcessVerticalSpace = true;
		composite.setLayoutData(gd);
		
		templateTable = new Table(composite, SWT.BORDER | SWT.H_SCROLL | SWT.CHECK | SWT.FULL_SELECTION);
		templateTable.setVisible(true);
		templateTable.setLinesVisible (false);
		templateTable.setHeaderVisible(true);
		templateTable.addSelectionListener(new SelectionListener () {
			public void widgetSelected(SelectionEvent e) {
				int rowsChecked = 0;
				for (int i=0; i<templateTable.getItemCount(); i++) {
					if (templateTable.getItem(i).getChecked()) rowsChecked ++;
				}
				if (rowsChecked > 0) {
					exportButton.setEnabled(true);
					deleteButton.setEnabled(true);
				}
				else {
					exportButton.setEnabled(false);
					deleteButton.setEnabled(false);
				}
				if (templateTable.getSelectionIndex() >= 0) {
					editButton.setEnabled(true);
					deleteButton.setEnabled(true);
				}
			}

			public void widgetDefaultSelected(SelectionEvent e) {}
		});
		templateTable.addKeyListener(new DeleteKeyListener(this, (IProject) getElement()));
		templateTable.addMouseListener(new TableDoubleClickListener(this));
		GridData data = new GridData (GridData.FILL_BOTH);
		data.heightHint = 140;
		data.grabExcessHorizontalSpace = true;
		data.grabExcessVerticalSpace = true;
		templateTable.setLayoutData(data);

		// create the columns
		TableColumn templateColumn = new TableColumn(templateTable, SWT.LEFT);
		TableColumn nameColumn = new TableColumn(templateTable, SWT.LEFT);
		TableColumn typeColumn = new TableColumn(templateTable, SWT.LEFT);
		templateColumn.setText("Template");
		nameColumn.setText("Name");
		ColumnLayoutData templateColumnLayout = new ColumnWeightData(50, false);
		ColumnLayoutData nameColumnLayout = new ColumnWeightData(50, false);

		// set columns in Table layout
		TableLayout tableLayout = new TableLayout();
		tableLayout.addColumnData(templateColumnLayout);
		tableLayout.addColumnData(nameColumnLayout);
		templateTable.setLayout(tableLayout);
		
		Composite buttonComposite = new Composite(composite, SWT.NULL);
		FillLayout fl = new FillLayout();
		fl.type = SWT.VERTICAL;
		fl.spacing = 2;
		buttonComposite.setLayout(fl);
		data = new GridData ();
		data.horizontalAlignment = GridData.BEGINNING;
		data.verticalAlignment = GridData.BEGINNING;
		buttonComposite.setLayoutData(data);

		importButton = new Button(buttonComposite, SWT.NATIVE);
		importButton.setText("Import");
		importButton.addSelectionListener(new SelectionListener () {
			public void widgetSelected(SelectionEvent e) {
				FileDialog fd = new FileDialog(getShell());
				fd.setFilterExtensions(new String[] {"*.zip"});
				String fileName = fd.open();
				if (null != fileName) {
					try {
						IProject project = (IProject) getElement();
						long uniqueId = System.currentTimeMillis();
						ZipFile zipFile = new ZipFile(new File(fileName), ZipFile.OPEN_READ);
						for (Enumeration enum=zipFile.entries(); enum.hasMoreElements(); ) {
							ZipEntry entry = (ZipEntry) enum.nextElement();
							String currentEntry = entry.getName();
							if (currentEntry.endsWith(".pt")) {
								BufferedInputStream is = new BufferedInputStream(zipFile.getInputStream(entry));
								TemplateLocation templateLocation = new TemplateLocation(is, project);
								if (null != templateLocation.getTemplate()) {
									ResourceManager.getInstance(project).addTemplateLocation(templateLocation);
								}
							}
						}
						reloadTemplates();
					}
					catch (Exception exc) {
						HSUtil.showError(exc.getMessage(), getShell());
					}
				}
			}
			public void widgetDefaultSelected(SelectionEvent e) {}
		});
		exportButton = new Button(buttonComposite, SWT.NATIVE);
		exportButton.setText("Export");
		exportButton.setEnabled(false);
		exportButton.addSelectionListener(new SelectionListener () {
			public void widgetSelected(SelectionEvent e) {
				FileDialog fd = new FileDialog(getShell());
				fd.setFilterExtensions(new String[] {"*.zip"});
				String fileName = fd.open();
				if (null != fileName && fileName.trim().length() > 0) {
					fileName = fileName.trim();
					if (fileName.indexOf(".") < 0) fileName = fileName + ".zip";
					try {
						IProject project = (IProject) getElement();
						long uniqueId = System.currentTimeMillis();
						List projectTemplates = new ArrayList();
						for (int i=0; i<templateTable.getItemCount(); i++) {
							if (templateTable.getItem(i).getChecked()) {
								TemplateLocation templateLocation = (TemplateLocation) ResourceManager.getInstance(project).getTemplateLocations().get(i);
								projectTemplates.add(templateLocation);
								templateTable.getItem(i).setChecked(false);
							}
						}
						exportButton.setEnabled(false);
						ZipOutputStream zos = null;
						try {
							zos = new ZipOutputStream(new FileOutputStream(new File(fileName)));
							for (Iterator i=projectTemplates.iterator(); i.hasNext(); ) {
								TemplateLocation templateLocation = (TemplateLocation) i.next();
								ZipEntry entry = new ZipEntry(uniqueId++ + ".pt");
								zos.putNextEntry(entry);
								zos.write(templateLocation.toString().getBytes());
								zos.closeEntry();
							}
						}
						finally {
							if (null != zos) zos.close();
						}
						MessageDialog.openInformation(getShell(), "Export Complete", projectTemplates.size() + " project templates sucessfully exported to " + fileName + ".");
					}
					catch (Exception exc) {
						HSUtil.showError(exc.getMessage(), getShell());
					}
				}
			}
			public void widgetDefaultSelected(SelectionEvent e) {}
		});

		new Label(buttonComposite, SWT.NULL);
		selectAllButton = new Button(buttonComposite, SWT.NATIVE);
		selectAllButton.setText("Select All");
		selectAllButton.addSelectionListener(new SelectionListener () {
			public void widgetSelected(SelectionEvent e) {
				TableItem[] items = templateTable.getItems();
				for (int i=0; i<items.length; i++) {
					items[i].setChecked(true);
				}
				if (items.length > 0) {
					deleteButton.setEnabled(true);
				}
			}
			public void widgetDefaultSelected(SelectionEvent e) {}
		});
		selectNoneButton = new Button(buttonComposite, SWT.NATIVE);
		selectNoneButton.setText("Deselect All");
		selectNoneButton.addSelectionListener(new SelectionListener () {
			public void widgetSelected(SelectionEvent e) {
				TableItem[] items = templateTable.getItems();
				for (int i=0; i<items.length; i++) {
					items[i].setChecked(false);
				}
				if (templateTable.getSelection().length == 0) {
					deleteButton.setEnabled(false);
				}
			}
			public void widgetDefaultSelected(SelectionEvent e) {}
		});

		buttonComposite = new Composite(tComp, SWT.NULL);
		fl = new FillLayout();
		fl.type = SWT.HORIZONTAL;
		fl.marginWidth = 5;
		fl.spacing = 2;
		buttonComposite.setLayout(fl);
		data = new GridData ();
		data.horizontalAlignment = GridData.BEGINNING;
		data.verticalAlignment = GridData.BEGINNING;
		data.horizontalSpan = 2;
		buttonComposite.setLayoutData(data);
		addButton = new Button(buttonComposite, SWT.NATIVE);
		addButton.setText("New");
		addButton.setVisible(true);
		addButton.addSelectionListener(new AddButtonListener(this, ((IProject) getElement())));
		editButton = new Button(buttonComposite, SWT.NATIVE);
		editButton.setText("Edit");
		editButton.addSelectionListener(new EditButtonListener(this));
		deleteButton = new Button(buttonComposite, SWT.NATIVE);
		deleteButton.setText("Delete");
		deleteButton.addSelectionListener(new DeleteButtonListener(this, ((IProject) getElement())));
		
		reloadTemplates();

		composite = new Composite(tComp, SWT.NONE);
		composite.setLayout(new GridLayout(1, false));
		gd = new GridData(GridData.FILL_BOTH);
		gd.grabExcessHorizontalSpace = true;
		gd.grabExcessVerticalSpace = true;
		composite.setLayoutData(gd);
		
		Label label = new Label(composite, SWT.NULL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.grabExcessHorizontalSpace = true;
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);
		label.setText("Context Parameters");
		
		parametersTable = new Table(composite, SWT.BORDER | SWT.H_SCROLL | SWT.FULL_SELECTION);
		parametersTable.setVisible(true);
		parametersTable.setLinesVisible (false);
		parametersTable.setHeaderVisible(true);
		parametersTable.addSelectionListener(new SelectionListener () {
			public void widgetSelected(SelectionEvent e) {
				editParameterButton.setEnabled(true);
				deleteParameterButton.setEnabled(true);
			}

			public void widgetDefaultSelected(SelectionEvent e) {}
		});
		parametersTable.addKeyListener(new ParameterDeleteKeyListener(this, (IProject) getElement()));
		parametersTable.addMouseListener(new ParameterDoubleClickListener(this));

		// create the columns
		TableColumn keyColumn = new TableColumn(parametersTable, SWT.LEFT);
		TableColumn valueColumn = new TableColumn(parametersTable, SWT.LEFT);
		keyColumn.setText("Name");
		valueColumn.setText("Value");
		ColumnLayoutData keyColumnLayout = new ColumnWeightData(35, false);
		ColumnLayoutData valueColumnLayout = new ColumnWeightData(65, false);

		// set columns in Table layout
		tableLayout = new TableLayout();
		tableLayout.addColumnData(keyColumnLayout);
		tableLayout.addColumnData(valueColumnLayout);
		parametersTable.setLayout(tableLayout);

		data = new GridData (GridData.FILL_BOTH);
		data.heightHint = 50;
		data.grabExcessHorizontalSpace = true;
		data.grabExcessVerticalSpace = true;
		parametersTable.setLayoutData(data);
		
		buttonComposite = new Composite(composite, SWT.NONE);
		data = new GridData ();
		data.horizontalAlignment = GridData.BEGINNING;
		data.verticalAlignment = GridData.BEGINNING;
		buttonComposite.setLayoutData(data);
		fl = new FillLayout();
		fl.type = SWT.HORIZONTAL;
		fl.spacing = 2;
		buttonComposite.setLayout(fl);
		buttonComposite.setVisible(true);
		addParameterButton = new Button(buttonComposite, SWT.NATIVE);
		addParameterButton.setText("New");
		addParameterButton.setVisible(true);
		addParameterButton.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL));
		addParameterButton.addSelectionListener(new AddParameterButtonListener(this, (IProject) getElement()));
		editParameterButton = new Button(buttonComposite, SWT.NATIVE);
		editParameterButton.setText("Edit");
		editParameterButton.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL));
		editParameterButton.addSelectionListener(new EditParameterButtonListener(this, (IProject) getElement()));
		deleteParameterButton = new Button(buttonComposite, SWT.NATIVE);
		deleteParameterButton.setText("Delete");
		deleteParameterButton.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL));
		deleteParameterButton.addSelectionListener(new DeleteParameterButtonListener(this, ((IProject) getElement())));

		reloadTemplateParameters();
		
		Composite subComp = new Composite(tComp, SWT.NULL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.grabExcessHorizontalSpace = true;
		subComp.setLayoutData(gd);
		subComp.setLayout(new GridLayout(4, false));
		label = new Label(subComp, SWT.NONE);
		label.setText("Context Object");
		contextObject = new Text(subComp, SWT.BORDER);
		contextObject.setEnabled(false);
		contextObject.setBackground(new Color(null, 255, 255, 255));
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 200;
		gd.grabExcessHorizontalSpace = true;
		contextObject.setLayoutData(gd);
		String contextObjString = Plugin.getProperty(getElement(), Constants.PROP_CONTEXT_OBJECT);
		if (null != contextObjString) contextObject.setText(contextObjString);

		Button contextObjectButton = new Button(subComp, SWT.NATIVE);
		contextObjectButton.setText("Browse");
		contextObjectButton.addMouseListener(new MouseListener() {
			public void mouseDown(MouseEvent e) {
				try {
					JavaProject javaProject = (JavaProject) JavaCore.create((IProject) getElement());
					IJavaSearchScope searchScope = SearchEngine.createWorkspaceScope();
					SelectionDialog sd = JavaUI.createTypeDialog(getShell(), 
								new ApplicationWindow(getShell()),
								searchScope,
								IJavaElementSearchConstants.CONSIDER_CLASSES,
								false);
					sd.open();
					Object[] objects = sd.getResult();
					if (null != objects && objects.length > 0) {
						IType type = (IType) objects[0];
						contextObject.setText(type.getFullyQualifiedName());
					}
				}
				catch (JavaModelException jme) {
					jme.printStackTrace();
				}
			}
			public void mouseDoubleClick(MouseEvent e) {}
			public void mouseUp(MouseEvent e) {}

		});
		Button clearButton = new Button(subComp, SWT.NATIVE);
		clearButton.setText("Clear");
		clearButton.addMouseListener(new MouseListener() {
			public void mouseDown(MouseEvent e) {
				contextObject.setText("");
			}
			public void mouseDoubleClick(MouseEvent e) {}
			public void mouseUp(MouseEvent e) {}

		});
		
		label = new Label(subComp, SWT.NONE);
		label.setText("Tip: You can reference this object in your templates as ${obj}");
		gd = new GridData();
		gd.horizontalSpan = 2;
		gd.grabExcessHorizontalSpace = true;
		label.setLayoutData(gd);
	}

	private void addSnippets(Composite parent) {	
		Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayout(new GridLayout(1, false));
		GridData gd = new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(gd);

		snippetTable = new Table(composite, SWT.BORDER | SWT.H_SCROLL | SWT.FULL_SELECTION);
		snippetTable.setVisible(true);
		snippetTable.setLinesVisible (false);
		snippetTable.setHeaderVisible(false);
		snippetTable.addSelectionListener(new SelectionListener () {
			public void widgetSelected(SelectionEvent e) {
				if (snippetTable.getSelectionIndex() >= 0) {
					editSnippetButton.setEnabled(true);
					deleteSnippetButton.setEnabled(true);
				}
			}
			public void widgetDefaultSelected(SelectionEvent e) {}
		});
		snippetTable.addMouseListener(new SnippetTableDoubleClickListener(this, (IProject) getElement(), getShell()));
		snippetTable.addKeyListener(new DeleteSnippetKeyListener(this, (IProject) getElement(), getShell()));
		GridData data = new GridData (GridData.FILL_BOTH);
		data.heightHint = 280;
		data.widthHint = 350;
		data.grabExcessHorizontalSpace = true;
		data.grabExcessVerticalSpace = true;
		snippetTable.setLayoutData(data);

		// create the columns
//		TableColumn nameColumn = new TableColumn(snippetTable, SWT.LEFT);
//		nameColumn.setText("Name");
//		ColumnWeightData nameColumnLayout = new ColumnWeightData(40, false);
//		TableColumn descColumn = new TableColumn(snippetTable, SWT.LEFT);
//		descColumn.setText("Description");
//		ColumnWeightData descColumnLayout = new ColumnWeightData(60, false);

		// set columns in Table layout
//		TableLayout tableLayout = new TableLayout();
//		tableLayout.addColumnData(nameColumnLayout);
//		tableLayout.addColumnData(descColumnLayout);
//		snippetTable.setLayout(tableLayout);
//		snippetTable.layout();

		Composite buttonComposite = new Composite(composite, SWT.NULL);
		data = new GridData ();
		data.horizontalAlignment = GridData.BEGINNING;
		data.verticalAlignment = GridData.BEGINNING;
		buttonComposite.setLayoutData(data);
		FillLayout fl = new FillLayout();
		fl.spacing = 2;
		fl.type = SWT.HORIZONTAL;
		buttonComposite.setLayout(fl);
		Button button = new Button(buttonComposite, SWT.NATIVE);
		button.setText("New");
		button.addSelectionListener(new AddSnippetListener(this, (IProject) getElement(), getShell()));

		editSnippetButton = new Button(buttonComposite, SWT.NATIVE);
		editSnippetButton.setText("Edit");
		editSnippetButton.addSelectionListener(new EditSnippetButtonListener(this, (IProject) getElement(), getShell()));
		deleteSnippetButton = new Button(buttonComposite, SWT.NATIVE);
		deleteSnippetButton.setText("Delete");
		deleteSnippetButton.addSelectionListener(new DeleteSnippetButtonListener(this, (IProject) getElement(), getShell()));

		reloadSnippets();
	}
	
	public class AddSnippetListener implements SelectionListener {
		private HibernateProperties props;
		private IProject project;
		private Shell shell;
		public AddSnippetListener (HibernateProperties props, IProject project, Shell shell) {
			this.props = props;
			this.project = project;
			this.shell = shell;
		}
		public void widgetSelected(SelectionEvent e) {
			AddProjectSnippet aps = new AddProjectSnippet(shell, props, project);
			aps.open();
		}

		public void widgetDefaultSelected(SelectionEvent e) {
		}
	}

	public class EditSnippetButtonListener implements SelectionListener {
		private HibernateProperties props;
		private IProject project;
		private Shell shell;
		public EditSnippetButtonListener (HibernateProperties props, IProject project, Shell shell) {
			this.props = props;
			this.project = project;
			this.shell = shell;
		}
		public void widgetSelected(SelectionEvent e) {
			int index = snippetTable.getSelectionIndex();
			Snippet snippet = (Snippet) ResourceManager.getInstance(project).getProjectSnippets().get(index);
			EditorUtil.openPage(snippet, shell);
			MessageDialog.openInformation(shell, UIUtil.getResourceTitle("ResourceModification"), UIUtil.getResourceText("ResourceInEditor"));
		}

		public void widgetDefaultSelected(SelectionEvent e) {
		}
	}

	public class SnippetTableDoubleClickListener implements MouseListener {
		private HibernateProperties props;
		private IProject project;
		private Shell shell;
		public SnippetTableDoubleClickListener (HibernateProperties props, IProject project, Shell shell) {
			this.props = props;
			this.project = project;
			this.shell = shell;
		}
		public void mouseDoubleClick(MouseEvent e) {
			int index = snippetTable.getSelectionIndex();
			Snippet snippet = (Snippet) ResourceManager.getInstance(project).getProjectSnippets().get(index);
			EditorUtil.openPage(snippet, shell);
			MessageDialog.openInformation(shell, UIUtil.getResourceTitle("ResourceModification"), UIUtil.getResourceText("ResourceInEditor"));
		}
		public void mouseDown(MouseEvent e) {};
		public void mouseUp(MouseEvent e) {};
	}

	public class DeleteSnippetButtonListener implements SelectionListener {
		private HibernateProperties props;
		private IProject project;
		private Shell shell;
		public DeleteSnippetButtonListener (HibernateProperties props, IProject project, Shell shell) {
			this.props = props;
			this.project = project;
			this.shell = shell;
		}
		public void widgetSelected(SelectionEvent e) {
			try {
				if (MessageDialog.openConfirm(shell, "Delete Confirmation", "Are you sure you want to delete this snippet?")) {
					int index = snippetTable.getSelectionIndex();
					Snippet snippet = (Snippet) ResourceManager.getInstance(project).getProjectSnippets().get(index);
					ResourceManager.getInstance(project).delete(snippet);
					reloadSnippets();
				}
			}
			catch (Exception e1) {
				Plugin.log(e1);
			}
		}

		public void widgetDefaultSelected(SelectionEvent e) {
		}
	}

	public class DeleteSnippetKeyListener implements KeyListener {
		private HibernateProperties props;
		private IProject project;
		private Shell shell;
		public DeleteSnippetKeyListener (HibernateProperties props, IProject project, Shell shell) {
			this.props = props;
			this.project = project;
			this.shell = shell;
		}
		public void keyPressed(KeyEvent e) {
			if (e.keyCode == SWT.DEL) {
				try {
					if (MessageDialog.openConfirm(shell, "Delete Confirmation", "Are you sure you want to delete this snippet?")) {
						int index = snippetTable.getSelectionIndex();
						Snippet snippet = (Snippet) ResourceManager.getInstance(project).getProjectSnippets().get(index);
						ResourceManager.getInstance(project).delete(snippet);
						reloadSnippets();
					}
				}
				catch (Exception e1) {
					Plugin.log(e1);
				}
			}
		}
		public void keyReleased(KeyEvent e) {}
	}

	public void reloadTemplateParameters () {
		try {
			parametersTable.removeAll();
			IProject project = (IProject) getElement();
			List templateNames = ResourceManager.getInstance(project).getTemplateParameterNames();
			for (Iterator i=templateNames.iterator(); i.hasNext(); ) {
				String key = (String) i.next();
				String value = ResourceManager.getInstance(project).getTemplateParameter(key);
				TableItem item = new TableItem(parametersTable, SWT.NULL);
				String[] arr = {key, value};
				item.setText(arr);
			}
			editParameterButton.setEnabled(false);
			deleteParameterButton.setEnabled(false);
		}
		catch (Exception e)
		{
			Plugin.log(e);
		}
		parametersTable.redraw();
	}
	
	public void reloadTemplates () {
		editButton.setEnabled(false);
		deleteButton.setEnabled(false);
		templateTable.removeAll();
		try {
			IProject project = (IProject) getElement();
			List templates = ResourceManager.getInstance(project).getTemplateLocations();
			for (Iterator i=templates.iterator(); i.hasNext(); ) {
				TemplateLocation templateLocation = (TemplateLocation) i.next();
				TableItem item = new TableItem(templateTable, SWT.NULL);
				String[] arr = {templateLocation.getTemplate().getName(), templateLocation.getName()};
				item.setText(arr);
			}
			if (templates.size() > 0) {
				selectAllButton.setEnabled(true);
				selectNoneButton.setEnabled(true);
			}
			else {
				selectAllButton.setEnabled(false);
				selectNoneButton.setEnabled(false);
			}

			templates = ResourceManager.getInstance(project).getTemplates();
			addButton.setEnabled(true);
			if (templateTable.getItemCount() == 0) {
				exportButton.setEnabled(false);
			}
		}
		catch (Exception e) {
			Plugin.log(e);
		}
		templateTable.redraw();
	}

	public void reloadSnippets () {
		editSnippetButton.setEnabled(false);
		deleteSnippetButton.setEnabled(false);
		snippetTable.removeAll();
		IProject project = (IProject) getElement();
		try {
			List snippets = ResourceManager.getInstance(project).getProjectSnippets();
			for (Iterator i=snippets.iterator(); i.hasNext(); ) {
				Snippet snippet = (Snippet) i.next();
				TableItem item = new TableItem(snippetTable, SWT.NULL);
				String[] arr = {snippet.getName()};
				item.setText(arr);
			}
		}
		catch (Exception e) {
			Plugin.log(e);
		}
		templateTable.redraw();
	}

	private void checkScreen () {
		if (Constants.PROP_VALUE_ABSOLUTE.equals(basePackageStyleSelection)) {
			basePackageHelp.setText("Choose the fully qualified package selection for your base objects.");
			basePackageHelp.setVisible(true);
			basePackageNameLbl.setVisible(true);
			basePackageText.setVisible(true);
			basePackageSelectionButton.setVisible(true);
		}
		else if (Constants.PROP_VALUE_SAME.equals(basePackageStyleSelection)) {
			basePackageHelp.setVisible(false);
			basePackageNameLbl.setVisible(false);
			basePackageText.setVisible(false);
			basePackageSelectionButton.setVisible(false);
		}
		else {
			basePackageHelp.setText("Choose a package location relative to the business objects.");
			basePackageHelp.setVisible(true);
			basePackageNameLbl.setVisible(true);
			basePackageText.setVisible(true);
			basePackageSelectionButton.setVisible(false);
		}

		if (Constants.PROP_VALUE_ABSOLUTE.equals(managerPackageStyleSelection)) {
			managerPackageHelp.setText("Choose the fully qualified package selection for your DAOs.");
			managerPackageHelp.setVisible(true);
			managerPackageNameLbl.setVisible(true);
			managerPackageText.setVisible(true);
			managerPackageSelectionButton.setVisible(true);
		}
		else if (Constants.PROP_VALUE_SAME.equals(managerPackageStyleSelection)) {
			managerPackageHelp.setVisible(false);
			managerPackageNameLbl.setVisible(false);
			managerPackageText.setVisible(false);
			managerPackageSelectionButton.setVisible(false);
		}
		else {
			managerPackageHelp.setText("Choose a package location relative to the business objects.");
			managerPackageHelp.setVisible(true);
			managerPackageNameLbl.setVisible(true);
			managerPackageText.setVisible(true);
			managerPackageSelectionButton.setVisible(false);
		}

		if (managerUseBasePackage.getBooleanValue()) {
			// baseManagerPackageStyle.setEnabled(false, baseManagerPackageStyleSelectionParent);
			baseManagerPackageStyleSelectionParent.setVisible(false);
			baseManagerPackageNameLbl.setVisible(false);
			baseManagerPackageText.setVisible(false);
			baseManagerPackageSelectionButton.setVisible(false);
		}
		else {
			// baseManagerPackageStyle.setEnabled(true, baseManagerPackageStyleSelectionParent);
			baseManagerPackageStyleSelectionParent.setVisible(true);
			if (Constants.PROP_VALUE_ABSOLUTE.equals(baseManagerPackageStyleSelection)) {
				baseManagerPackageNameLbl.setVisible(true);
				baseManagerPackageText.setVisible(true);
				baseManagerPackageSelectionButton.setVisible(true);
			}
			else if (Constants.PROP_VALUE_SAME.equals(baseManagerPackageStyleSelection)) {
				baseManagerPackageNameLbl.setVisible(false);
				baseManagerPackageText.setVisible(false);
				baseManagerPackageSelectionButton.setVisible(false);
			}
			else {
				baseManagerPackageNameLbl.setVisible(true);
				baseManagerPackageText.setVisible(true);
				baseManagerPackageSelectionButton.setVisible(false);
			}
		}

		boolean enabled = boEnabled.getBooleanValue();
		basePackageText.setEnabled(enabled);
		basePackageStyle.setEnabled(enabled, baseGroup);
		basePackageHelp.setEnabled(enabled);
		basePackageNameLbl.setEnabled(enabled);
		
		enabled = managersEnabled.getBooleanValue();
		managerPackageText.setEnabled(enabled);
		managerPackageStyle.setEnabled(enabled, managerGroup);
		managerPackageHelp.setEnabled(enabled);
		managerPackageNameLbl.setEnabled(enabled);
		customManagerComposite.setVisible(enabled);
		managerRootComposite.setVisible(enabled);
		
		if (enabled) {
			boolean isCustomManager = customManager.getBooleanValue();
			managerRootComposite.setVisible(isCustomManager);
		}
	}

	private Composite createDefaultComposite(Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		composite.setLayout(layout);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.verticalAlignment = GridData.FILL;
		data.horizontalAlignment = GridData.FILL;
		composite.setLayoutData(data);

		return composite;
	}

	public boolean performOk() {
		try {
			Plugin.setProperty(getElement(), Constants.PROP_GENERATION_ENABLED, new Boolean(generationEnabled.getBooleanValue()).toString());
			Plugin.setProperty(getElement(), Constants.PROP_GENERATION_VALUE_OBJECT_ENABLED, new Boolean(boEnabled.getBooleanValue()).toString());
			String s = basePackageText.getText();
			if (null == s) s = Constants.DEFAULT_BASE_VO_PACKAGE;
			s = s.trim();
			while (s.startsWith(".")) s = s.substring(1, s.length());
			while (s.endsWith(".")) s = s.substring(0, s.length() - 1);
			if (s.length() == 0) s = Constants.DEFAULT_BASE_VO_PACKAGE;
			Plugin.setProperty(getElement(), Constants.PROP_BASE_VO_PACKAGE_NAME, s);
			Plugin.setProperty(getElement(), Constants.PROP_BASE_VO_PACKAGE_STYLE, basePackageStyleSelection);
			Plugin.setProperty(getElement(), Constants.PROP_GENERATION_DAO_ENABLED, new Boolean(managersEnabled.getBooleanValue()).toString());

			s = managerPackageText.getText();
			if (null == s) s = Constants.DEFAULT_DAO_PACKAGE;
			s = s.trim();
			while (s.startsWith(".")) s = s.substring(1, s.length());
			while (s.endsWith(".")) s = s.substring(0, s.length() - 1);
			if (s.length() == 0) s = Constants.DEFAULT_DAO_PACKAGE;
			Plugin.setProperty(getElement(), Constants.PROP_DAO_PACKAGE_NAME, s);
			Plugin.setProperty(getElement(), Constants.PROP_DAO_PACKAGE_STYLE, managerPackageStyleSelection);
			Plugin.setProperty(getElement(), Constants.PROP_BASE_DAO_USE_BASE_PACKAGE, new Boolean(managerUseBasePackage.getBooleanValue()).toString());
			s = baseManagerPackageText.getText();
			if (null == s) s = Constants.DEFAULT_BASE_DAO_PACKAGE;
			s = s.trim();
			while (s.startsWith(".")) s = s.substring(1, s.length());
			while (s.endsWith(".")) s = s.substring(0, s.length() - 1);
			if (s.length() == 0) s = Constants.DEFAULT_BASE_DAO_PACKAGE;
			Plugin.setProperty(getElement(), Constants.PROP_BASE_DAO_PACKAGE_NAME, s);
			Plugin.setProperty(getElement(), Constants.PROP_BASE_DAO_PACKAGE_STYLE, baseManagerPackageStyleSelection);
			Plugin.setProperty(getElement(), Constants.PROP_USE_CUSTOM_ROOT_DAO, new Boolean(customManager.getBooleanValue()).toString());
			Plugin.setProperty(getElement(), Constants.PROP_CUSTOM_ROOT_DAO_CLASS, daoRootClass.getText());
			Plugin.setProperty(getElement(), Constants.PROP_BASE_DAO_EXCEPTION, daoExceptionClass.getText());
			Plugin.setProperty(getElement(), Constants.PROP_CONTEXT_OBJECT, contextObject.getText());
			
			if (null != sourceLocation) {
				// System.out.println("Source Location: " + sourceLocation.getItem(sourceLocation.getSelectionIndex()));
				Plugin.setProperty(getElement(), Constants.PROP_SOURCE_LOCATION, sourceLocation.getItem(sourceLocation.getSelectionIndex()));
			}
			else {
				Plugin.clearProperty(getElement(), Constants.PROP_SOURCE_LOCATION);
			}
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	public IPreferenceStore getLocalPreferenceStore() {
		if (null == preferenceStore) {
			preferenceStore = new PreferenceStore();
			try {
				String s = Plugin.getProperty(getElement(), Constants.PROP_GENERATION_ENABLED);
				if (null == s) s = Boolean.TRUE.toString();
				preferenceStore.setValue(Constants.PROP_GENERATION_ENABLED, s);
				s = Plugin.getProperty(getElement(), Constants.PROP_BASE_VO_PACKAGE_STYLE);
				if (null == s) s = Constants.PROP_VALUE_RELATIVE;
				preferenceStore.setValue(Constants.PROP_BASE_VO_PACKAGE_STYLE, s);
				basePackageStyleSelection = s;
				s = Plugin.getProperty(getElement(), Constants.PROP_GENERATION_VALUE_OBJECT_ENABLED);
				if (null == s) s = Boolean.TRUE.toString();
				preferenceStore.setValue(Constants.PROP_GENERATION_VALUE_OBJECT_ENABLED, s);
				s = Plugin.getProperty(getElement(), Constants.PROP_DAO_PACKAGE_STYLE);
				if (null == s) s = Constants.PROP_VALUE_RELATIVE;
				preferenceStore.setValue(Constants.PROP_DAO_PACKAGE_STYLE, s);
				managerPackageStyleSelection = s;
				s = Plugin.getProperty(getElement(), Constants.PROP_GENERATION_DAO_ENABLED);
				if (null == s) s = Boolean.TRUE.toString();
				preferenceStore.setValue(Constants.PROP_GENERATION_DAO_ENABLED, s);
				s = Plugin.getProperty(getElement(), Constants.PROP_BASE_DAO_USE_BASE_PACKAGE);
				if (null == s) s = Boolean.TRUE.toString();
				preferenceStore.setValue(Constants.PROP_BASE_DAO_USE_BASE_PACKAGE, s);
				s = Plugin.getProperty(getElement(), Constants.PROP_BASE_DAO_PACKAGE_STYLE);
				if (null == s) s = Constants.PROP_VALUE_SAME;
				preferenceStore.setValue(Constants.PROP_BASE_DAO_PACKAGE_STYLE, s);
				baseManagerPackageStyleSelection = s;
				s = Plugin.getProperty(getElement(), Constants.PROP_USE_CUSTOM_ROOT_DAO);
				if (null == s) s = Boolean.FALSE.toString();
				preferenceStore.setValue(Constants.PROP_USE_CUSTOM_ROOT_DAO, s);
				s = Plugin.getProperty(getElement(), Constants.PROP_BASE_DAO_EXCEPTION);
			}
			catch (Exception e) {}
		}
		return preferenceStore;
	}
	
	public class AddButtonListener implements SelectionListener {
		private HibernateProperties parent;
		private IProject project;

		public AddButtonListener (HibernateProperties parent, IProject project) {
			this.parent = parent;
			this.project = project;
		}
		public void widgetSelected(SelectionEvent e) {
			try {
				List templates = ResourceManager.getInstance(project).getNonRequiredTemplates();
				int templateCount = 0;
				if (null != templates && templates.size() > 0) {
					for (Iterator i=templates.iterator(); i.hasNext(); ) {
						Template template = (Template) i.next();
						if (null == ResourceManager.getInstance(project).getTemplateLocation(template.getName())) {
							templateCount ++;
						}
					}
					if (templateCount == 0) {
						HSUtil.showError("You must add more templates by clicking Window >> Preferences >> Hibernate Synchronizer | (Templates tab)", getShell());
						return;
					}
				}
			}
			catch (Exception exc) {}
			AddTemplateLocation dialog = new AddTemplateLocation(getShell(), parent, project);
			dialog.open();
		}
		
		public void widgetDefaultSelected(SelectionEvent e) {
		}
	}

	public class AddParameterButtonListener implements SelectionListener {
		private HibernateProperties parent;
		private IProject project;

		public AddParameterButtonListener (HibernateProperties parent, IProject project) {
			this.parent = parent;
			this.project = project;
		}
		public void widgetSelected(SelectionEvent e) {
			AddTemplateParameter dialog = new AddTemplateParameter(getShell(), parent, project);
			dialog.open();
		}
		
		public void widgetDefaultSelected(SelectionEvent e) {
		}
	}
	
	public class DeleteKeyListener implements KeyListener {
		private HibernateProperties parent;
		private IProject project;
		public DeleteKeyListener (HibernateProperties parent, IProject project) {
			this.parent = parent;
			this.project = project;
		}
		public void keyPressed(KeyEvent e) {
			if (e.keyCode == SWT.DEL) {
				int index = templateTable.getSelectionIndex();
				if (index >= 0) {
					try {
						TemplateLocation templateLocation = (TemplateLocation) ResourceManager.getInstance(project).getTemplateLocations().get(index);
						boolean confirm = MessageDialog.openConfirm(parent.getShell(), "Project Template Removal Confirmation", "Are you sure you want to remove this template from your project?");
						if (confirm) {
							ResourceManager.getInstance(project).deleteTemplateLocation(templateLocation);
							parent.reloadTemplates();
						}
					}
					catch (Exception exc) {
						Plugin.log(exc);
					}
				}
			}
		}
		public void keyReleased(KeyEvent e) {}
	}

	public class DeleteButtonListener implements SelectionListener {
		private HibernateProperties parent;
		private IProject project;

		public DeleteButtonListener (HibernateProperties parent, IProject project) {
			this.parent = parent;
			this.project = project;
		}
		public void widgetSelected(SelectionEvent e) {
			int count = 0;
			TableItem[] items = templateTable.getItems();
			for (int i=0; i<items.length; i++) {
				if (items[i].getChecked()) count++;
			}
			if (count > 0) {
				boolean confirm = MessageDialog.openConfirm(parent.getShell(), "Template Removal Confirmation", "Are you sure you want to remove the " + count + " checked templates from your project?");
				if (confirm) {
					ArrayList ttd = new ArrayList();
					try {
						for (int i=0; i<items.length; i++) {
							if (items[i].getChecked()) {
								TemplateLocation templateLocation = (TemplateLocation) ResourceManager.getInstance(project).getTemplateLocations().get(i);
								ttd.add(templateLocation);
							}
						}
						for (Iterator i=ttd.iterator(); i.hasNext(); ) {
							try {
								ResourceManager.getInstance(project).deleteTemplateLocation((TemplateLocation) i.next());
							}
							catch (Exception exc) {
								Plugin.log(exc);
							}
						}
						parent.reloadTemplates();
					}
					catch (Exception exc) {
						MessageDialog.openError(getShell(), "An error has occured", exc.getMessage());
					}
				}
			}
			else {
				int index = templateTable.getSelectionIndex();
				if (index >= 0) {
					try {
						TemplateLocation templateLocation = (TemplateLocation) ResourceManager.getInstance(project).getTemplateLocations().get(index);
						boolean confirm = MessageDialog.openConfirm(parent.getShell(), "Project Template Removal Confirmation", "Are you sure you want to remove this template from your project?");
						if (confirm) {
							ResourceManager.getInstance(project).deleteTemplateLocation(templateLocation);
							parent.reloadTemplates();
						}
					}
					catch (Exception exc) {
						exc.printStackTrace();
					}
				}
			}
		}
		
		public void widgetDefaultSelected(SelectionEvent e) {}
	}

	public class DeleteParameterButtonListener implements SelectionListener {
		private HibernateProperties parent;
		private IProject project;

		public DeleteParameterButtonListener (HibernateProperties parent, IProject project) {
			this.parent = parent;
			this.project = project;
		}
		public void widgetSelected(SelectionEvent e) {
			int index = parametersTable.getSelectionIndex();
			if (index >= 0) {
				try {
					boolean confirm = MessageDialog.openConfirm(parent.getShell(), "Project Template Parameter Removal Confirmation", "Are you sure you want to remove this template parameter from your project?");
					if (confirm) {
						List parameterNames = ResourceManager.getInstance(project).getTemplateParameterNames();
						String key = (String) parameterNames.get(index);
						ResourceManager.getInstance(project).deleteTemplateParameter(key);
						parent.reloadTemplateParameters();
					}
				}
				catch (Exception exc) {
					Plugin.log(exc);
				}
			}
		}
		
		public void widgetDefaultSelected(SelectionEvent e) {}
	}
	
	public class TableDoubleClickListener implements MouseListener {
		private HibernateProperties parent;
		public TableDoubleClickListener (HibernateProperties parent) {
			this.parent = parent;
		}
		public void mouseDoubleClick(MouseEvent e) {
			try {
				IProject project = (IProject) getElement();
				TemplateLocation templateLocation = (TemplateLocation) ResourceManager.getInstance(project).getTemplateLocations().get(templateTable.getSelectionIndex());
				EditTemplateLocation dialog = new EditTemplateLocation(getShell(), parent, ((IProject) getElement()), templateLocation);
				dialog.open();
			}
			catch (Exception exc) {
				Plugin.log(exc);
			}
		}
		public void mouseDown(MouseEvent e) {}
		public void mouseUp(MouseEvent e) {}
	}
	
	public class EditButtonListener implements SelectionListener {
		private HibernateProperties parent;
		public EditButtonListener (HibernateProperties parent) {
			this.parent = parent;
		}
		public void widgetSelected(SelectionEvent e) {
			try {
				IProject project = (IProject) getElement();
				TemplateLocation templateLocation = (TemplateLocation) ResourceManager.getInstance(project).getTemplateLocations().get(templateTable.getSelectionIndex());
				EditTemplateLocation dialog = new EditTemplateLocation(getShell(), parent, ((IProject) getElement()), templateLocation);
				dialog.open();
			}
			catch (Exception exc) {
				Plugin.log(exc);
			}
		}
		
		public void widgetDefaultSelected(SelectionEvent e) {}
	}

	public class EditParameterButtonListener implements SelectionListener {
		private HibernateProperties parent;
		private IProject project;

		public EditParameterButtonListener (HibernateProperties parent, IProject project) {
			this.parent = parent;
			this.project = project;
		}
		public void widgetSelected(SelectionEvent e) {
			int index = parametersTable.getSelectionIndex();
			if (index >= 0) {
				try {
					List parameterNames = ResourceManager.getInstance(project).getTemplateParameterNames();
					String key = (String) parameterNames.get(index);
					EditTemplateParameter dialog = new EditTemplateParameter(getShell(), parent, (IProject) getElement(), key);
				}
				catch (Exception exc) {
					Plugin.log(exc);
				}
			}
		}
		
		public void widgetDefaultSelected(SelectionEvent e) {}
	}
	
	public class ParameterDeleteKeyListener implements KeyListener {
		private HibernateProperties parent;
		private IProject project;
		public ParameterDeleteKeyListener (HibernateProperties parent, IProject project) {
			this.parent = parent;
			this.project = project;
		}
		public void keyPressed(KeyEvent e) {
			if (e.keyCode == SWT.DEL) {
				int index = parametersTable.getSelectionIndex();
				if (index >= 0) {
					try {
						boolean confirm = MessageDialog.openConfirm(parent.getShell(), "Project Template Parameter Removal Confirmation", "Are you sure you want to remove this template parameter from your project?");
						if (confirm) {
							List parameterNames = ResourceManager.getInstance(project).getTemplateParameterNames();
							String key = (String) parameterNames.get(index);
							ResourceManager.getInstance(project).deleteTemplateParameter(key);
							parent.reloadTemplateParameters();
						}
					}
					catch (Exception exc) {
						Plugin.log(exc);
					}
				}
			}
		}
		public void keyReleased(KeyEvent e) {}
	}
	
	public class ParameterDoubleClickListener implements MouseListener {
		private HibernateProperties parent;
		public ParameterDoubleClickListener (HibernateProperties parent) {
			this.parent = parent;
		}
		public void mouseDoubleClick(MouseEvent e) {
			try {
				IProject project = (IProject) getElement();
				int index = parametersTable.getSelectionIndex();
				List parameterNames = ResourceManager.getInstance(project).getTemplateParameterNames();
				String key = (String) parameterNames.get(index);
				EditTemplateParameter dialog = new EditTemplateParameter(getShell(), parent, (IProject) getElement(), key);
				dialog.open();
			}
			catch (Exception exc) {
				Plugin.log(exc);
			}
		}
		public void mouseDown(MouseEvent e) {}
		public void mouseUp(MouseEvent e) {}
	}
}