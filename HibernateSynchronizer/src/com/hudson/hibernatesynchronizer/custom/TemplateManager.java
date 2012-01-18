/**
 * This software is licensed under the general public license.  See http://www.gnu.org/copyleft/gpl.html
 * for more information.
 */
package com.hudson.hibernatesynchronizer.custom;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

import com.hudson.hibernatesynchronizer.Constants;
import com.hudson.hibernatesynchronizer.Plugin;

/**
 * @author <a href="mailto: joe@binamics.com">Joe Hudson </a>
 */
public class TemplateManager {

    private static final String PROJECT_TEMPLATES = "ProjectTemplates";

    private static final String TEMPLATE_PARAMETERS = "TemplateParameters";

    private static TemplateManager instance = new TemplateManager();

    private List templates;

    private List snippets;

    private Map projectSnippets = new HashMap();

    private Map projectSnippetsOnly = new HashMap();

    private Map customSnippetsOnly = new HashMap();

    private Map projectTemplates = new HashMap();

    private Map templateParameters;

    public static TemplateManager getInstance() {
        return instance;
    }

    public void reloadTemplates() {
        this.templates = null;
        this.projectTemplates.clear();
    }

    public void reloadSnippets() {
        this.snippets = null;
        projectSnippets.clear();
        projectSnippetsOnly.clear();
        customSnippetsOnly.clear();
    }

    public void reloadTemplateParameters() {
        this.templateParameters = null;
    }

    public List getTemplateParameterNames(IProject project)
            throws CoreException {
        Map templateParameters = getTemplateParameters(project);
        ArrayList parameterNames = new ArrayList(templateParameters.size());
        for (Iterator i = templateParameters.keySet().iterator(); i.hasNext();) {
            parameterNames.add(i.next());
        }
        Collections.sort(parameterNames);
        return parameterNames;
    }

    public Map getTemplateParameters(IProject project) throws CoreException {
        if (null == templateParameters) {
            templateParameters = new HashMap();
            String ptString = Plugin.getProperty(project, TEMPLATE_PARAMETERS);
            if (null != ptString && ptString.trim().length() > 0) {
                StringTokenizer st = new StringTokenizer(ptString, "&");
                while (st.hasMoreTokens()) {
                    String s = st.nextToken();
                    int index = s.indexOf("=");
                    String key = URLDecoder.decode(s.substring(0, index));
                    String value = URLDecoder.decode(s.substring(index + 1, s
                            .length()));
                    templateParameters.put(key, value);
                }
            }
        }
        return templateParameters;
    }

    public void saveTemplateParameters(IProject project) throws CoreException {
        StringBuffer sb = new StringBuffer();
        Map templateParameters = getTemplateParameters(project);
        for (Iterator i = templateParameters.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry) i.next();
            if (sb.length() > 0)
                sb.append("&");
            sb.append(URLEncoder.encode((String) entry.getKey()));
            sb.append("=");
            sb.append(URLEncoder.encode((String) entry.getValue()));
        }
        Plugin.saveProperty(project, TEMPLATE_PARAMETERS, sb.toString());
    }

    public String getTemplateParameter(String key, IProject project)
            throws CoreException {
        return (String) getTemplateParameters(project).get(key);
    }

    public void addTemplateParameter(String key, String value, IProject project)
            throws CoreException {
        getTemplateParameters(project).put(key, value);
        saveTemplateParameters(project);
    }

    public void updateTemplateParameter(String key, String value,
            IProject project) throws CoreException {
        getTemplateParameters(project).remove(key);
        addTemplateParameter(key, value, project);
    }

    public void deleteTemplateParameter(String key, IProject project)
            throws CoreException {
        getTemplateParameters(project).remove(key);
        saveTemplateParameters(project);
    }

    public ProjectTemplate findProjectTemplateByName(IProject project,
            String templateName) throws CoreException {
        return findProjectTemplateByName(project, templateName, null);
    }

    public ProjectTemplate findProjectTemplateByName(IProject project,
            String templateName, String newTemplateName) throws CoreException {
        Iterator i = null;
        if (null != newTemplateName) {
            i = TemplateManager.getInstance().getProjectTemplates(project,
                    templateName, newTemplateName).iterator();
        } else {
            i = TemplateManager.getInstance().getProjectTemplates(project)
                    .iterator();
        }
        while (i.hasNext()) {
            ProjectTemplate pt = (ProjectTemplate) i.next();
            if (null != pt.getTemplate()) {
                if (pt.getTemplate().getName().equals(templateName)
                        || (null != newTemplateName && pt.getTemplate()
                                .getName().equals(newTemplateName))) {
                    return pt;
                }
            }
        }
        return null;
    }

    public ProjectTemplate getProjectTemplate(String projectTemplateName,
            IProject project) throws CoreException {
        ProjectTemplate projectTemplate = null;
        if (null != projectTemplateName) {
            for (Iterator i = TemplateManager.getInstance()
                    .getProjectTemplates(project).iterator(); i.hasNext();) {
                ProjectTemplate pt = (ProjectTemplate) i.next();
                if (pt.getName().equals(projectTemplateName)) {
                    projectTemplate = pt;
                }
            }
        }
        return projectTemplate;
    }

    public List getProjectTemplates(IProject project) throws CoreException {
        return getProjectTemplates(project, null, null);
    }

    public List getProjectTemplates(IProject project, String previousTemplate,
            String newTemplate) throws CoreException {
        List ptList = (List) projectTemplates.get(project);
        if (null == ptList) {
            ptList = new ArrayList();
            String ptString = Plugin.getProperty(project, PROJECT_TEMPLATES);
            if (null != ptString) {
                StringTokenizer st = new StringTokenizer(ptString, ";");
                while (st.hasMoreTokens()) {
                    try {
                        ProjectTemplate template = new ProjectTemplate(st
                                .nextToken(), previousTemplate, newTemplate);
                        if (null != template.getTemplate()) {
                            ptList.add(template);
                        }
                    } catch (IOException ioe) {
                        Plugin.logError(ioe);
                    }
                }
            }
        }
        return ptList;
    }

    public void saveProjectTemplates(IProject project, List projectTemplates)
            throws CoreException {
        if (null == projectTemplates) {
            Plugin.clearProperty(project, PROJECT_TEMPLATES);
            return;
        }
        StringBuffer ptString = new StringBuffer();
        boolean started = false;
        for (Iterator i = projectTemplates.iterator(); i.hasNext();) {
            ProjectTemplate pt = (ProjectTemplate) i.next();
            if (started)
                ptString.append(";");
            else
                started = true;
            ptString.append(pt.toString());
        }
        Plugin.saveProperty(project, PROJECT_TEMPLATES, ptString.toString());
    }

    public void deleteProjectTemplate(IProject project, String templateId)
            throws CoreException {
        List projectTemplates = getProjectTemplates(project);
        for (int i = 0; i < projectTemplates.size(); i++) {
            ProjectTemplate template = (ProjectTemplate) projectTemplates
                    .get(i);
            if (template.getTemplate().getId().equals(templateId)) {
                projectTemplates.remove(i);
            }
        }
        saveProjectTemplates(project, projectTemplates);
        reloadTemplates();
    }

    public void updateProjectTemplate(IProject project,
            ProjectTemplate projectTemplate) throws CoreException {
        List projectTemplates = getProjectTemplates(project);
        for (int i = 0; i < projectTemplates.size(); i++) {
            ProjectTemplate template = (ProjectTemplate) projectTemplates
                    .get(i);
            if (template.getTemplate().getId().equals(
                    projectTemplate.getTemplate().getId())) {
                projectTemplates.set(i, projectTemplate);
            }
        }
        saveProjectTemplates(project, projectTemplates);
    }

    public ProjectTemplate findProjectTemplate(IProject project,
            String templateId) throws CoreException {
        List ptList = getProjectTemplates(project);
        for (Iterator i = ptList.iterator(); i.hasNext();) {
            ProjectTemplate pt = (ProjectTemplate) i.next();
            if (null != pt.getTemplate()
                    && pt.getTemplate().getId().equals(templateId)) {
                return pt;
            }
        }
        return null;
    }

    public Template findTemplate(String id) throws IOException {
        if (null == templates)
            getTemplates();
        for (Iterator i = templates.iterator(); i.hasNext();) {
            Template t = (Template) i.next();
            if (t.getId().equals(id)) {
                return t;
            }
        }
        return null;
    }

    public Template findTemplateByName(String templateName) throws IOException {
        if (null == templates)
            getTemplates();
        for (Iterator i = templates.iterator(); i.hasNext();) {
            Template t = (Template) i.next();
            if (t.getName().equals(templateName)) {
                return t;
            }
        }
        return null;
    }

    public List getTemplates() throws IOException {
        if (null == templates) {
            File[] files = Constants.getCustomTemplateDirectory().listFiles();
            templates = new ArrayList(files.length);
            for (int i = 0; i < files.length; i++) {
                if (files[i].getName().endsWith(Constants.EXT_TEMPLATE)) {
                    templates.add(new Template(files[i]));
                }
            }
            Collections.sort(templates);
        }
        return templates;
    }

    public Snippet findSnippet(String id, IProject project) throws IOException {
        for (Iterator i = getSnippets(project).iterator(); i.hasNext();) {
            Snippet s = (Snippet) i.next();
            if (s.getId().equals(id)) {
                return s;
            }
        }
        return null;
    }

    public Snippet findSnippetByName(String snippetName, IProject project)
            throws IOException {
        for (Iterator i = getSnippets(project).iterator(); i.hasNext();) {
            Snippet s = (Snippet) i.next();
            if (s.getName().equals(snippetName)) {
                return s;
            }
        }
        return null;
    }

    public List getProjectSnippetsOnly(IProject project) throws IOException {
        List projectSnippets = (List) projectSnippetsOnly
                .get(project.getName());
        if (null == projectSnippets) {
            projectSnippets = new ArrayList();
            File dir = Constants.getProjectSnippetDirectory(project);
            if (dir.exists()) {
                File[] files = dir.listFiles();
                for (int i = 0; i < files.length; i++) {
                    if (files[i].getName().endsWith(Constants.EXT_TEMPLATE)) {
                        Snippet s = new Snippet(files[i]);
                        projectSnippets.add(s);
                    }
                }
            }
            Collections.sort(projectSnippets);
            projectSnippetsOnly.put(project.getName(), projectSnippets);
        }
        return projectSnippets;
    }

    public List getCustomSnippetsOnly(IProject project) throws IOException {
        List customSnippets = (List) customSnippetsOnly.get(project.getName());
        if (null == customSnippets) {
            customSnippets = new ArrayList();
            List allSnippets = getSnippets(project);
            for (Iterator i = allSnippets.iterator(); i.hasNext();) {
                Snippet s = (Snippet) i.next();
                if (s.isCustom())
                    customSnippets.add(s);
            }
            Collections.sort(customSnippets);
            customSnippetsOnly.put(project.getName(), customSnippets);
        }
        return customSnippets;
    }

    public Snippet getSnippetByName(String name, IProject project)
            throws IOException {
        for (Iterator i = getSnippets(project).iterator(); i.hasNext();) {
            Snippet s = (Snippet) i.next();
            if (s.getName().equals(name))
                return s;
        }
        return null;
    }

    public List getSnippets(IProject project) throws IOException {
        if (null == snippets) {
            snippets = new ArrayList();
            File[] files = Constants.getSnippetDirectory().listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].getName().endsWith(Constants.EXT_TEMPLATE)) {
                    Snippet s = new Snippet(files[i]);
                    snippets.add(s);
                }
            }
            Collections.sort(snippets);
        }
        if (null == project) {
            return snippets;
        }
        List projectSnippets = (List) this.projectSnippets.get(project
                .getName());
        if (null == projectSnippets) {
            projectSnippets = new ArrayList();
            Map psMap = new HashMap();
            for (Iterator i = getProjectSnippetsOnly(project).iterator(); i
                    .hasNext();) {
                Snippet s = (Snippet) i.next();
                psMap.put(s.getName(), s);
            }
            for (Iterator i = psMap.values().iterator(); i.hasNext();) {
                projectSnippets.add(i.next());
            }
            for (Iterator i = snippets.iterator(); i.hasNext();) {
                Snippet s = (Snippet) i.next();
                if (null == psMap.get(s.getName())) {
                    projectSnippets.add(s);
                }
            }
            Collections.sort(projectSnippets);
            this.projectSnippets.put(project.getName(), projectSnippets);
        }
        return projectSnippets;
    }
}