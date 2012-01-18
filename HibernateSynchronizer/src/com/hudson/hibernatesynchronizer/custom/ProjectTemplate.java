/**
 * This software is licensed under the general public license.  See http://www.gnu.org/copyleft/gpl.html
 * for more information.
 */
package com.hudson.hibernatesynchronizer.custom;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.hudson.hibernatesynchronizer.util.HSUtil;

/**
 * @author <a href="mailto: joe@binamics.com">Joe Hudson </a>
 */
public class ProjectTemplate {
    private Template template;

    private String location;

    private String name;

    private boolean override;

    private boolean enabled;

    public ProjectTemplate() {
    }

    public ProjectTemplate(InputStream templateInfo) throws IOException {
        restore(HSUtil.getStringFromStream(templateInfo), null, null);
    }

    public ProjectTemplate(String templateInfo) throws IOException {
        restore(templateInfo, null, null);
    }

    public ProjectTemplate(InputStream templateInfo, String previousTemplte,
            String newTemplate) throws IOException {
        restore(HSUtil.getStringFromStream(templateInfo), previousTemplte,
                newTemplate);
    }

    public ProjectTemplate(String templateInfo, String previousTemplte,
            String newTemplate) throws IOException {
        restore(templateInfo, previousTemplte, newTemplate);
    }

    public void restore(String s, String previousTemplte, String newTemplate)
            throws IOException {
        int start = 0;
        int index = s.indexOf(":");
        String templateId = s.substring(start, index);
        if (null != previousTemplte && templateId.equals(previousTemplte))
            templateId = newTemplate;
        template = TemplateManager.getInstance().findTemplateByName(templateId);
        start = index + 1;
        index = s.indexOf(":", start);
        name = s.substring(start, index);
        start = index + 1;
        index = s.indexOf(":", start);
        location = s.substring(start, index);
        start = index + 1;
        index = s.indexOf(":", start);
        override = new Boolean(s.substring(start, index)).booleanValue();
        start = index + 1;
        enabled = new Boolean(s.substring(start, s.length())).booleanValue();
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(template.getName() + ":");
        sb.append(name + ":");
        sb.append(location + ":");
        sb.append(new Boolean(override).toString() + ":");
        sb.append(new Boolean(enabled).toString());
        return sb.toString();
    }

    /**
     * @return
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * @param enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * @return
     */
    public String getLocation() {
        return location;
    }

    /**
     * @param location
     */
    public void setLocation(String location) {
        if (null == location)
            this.location = "";
        else {
            this.location = location.trim();
        }
    }

    /**
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * @param name
     */
    public void setName(String name) {
        if (null == name)
            this.name = null;
        else
            this.name = name.trim();
    }

    /**
     * @return
     */
    public boolean shouldOverride() {
        return override;
    }

    /**
     * @param override
     */
    public void setOverride(boolean override) {
        this.override = override;
    }

    /**
     * @return
     */
    public Template getTemplate() {
        return template;
    }

    /**
     * @param template
     */
    public void setTemplate(Template template) {
        this.template = template;
    }

    public List validate() {
        List errors = new ArrayList();
        if (null == name || name.length() == 0) {
            errors.add("You must enter the name");
        } else if (name.indexOf(':') >= 0) {
            errors.add("The name must not contain a ':'");
        }
        if (null == location || location.trim().length() == 0) {
            location = "";
        } else if (location.indexOf(':') >= 0) {
            errors.add("The location must not contain a ':'");
        } else {
            if (getTemplate().isJavaClass()
                    && (location.indexOf("/") > 0 || location.indexOf("\\") > 0)) {
                errors.add("The package can not have file separators");
            }
        }
        return errors;
    }
}