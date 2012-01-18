/**
 * This software is licensed under the general public license.  See http://www.gnu.org/copyleft/gpl.html
 * for more information.
 */
package com.hudson.hibernatesynchronizer.custom;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.hudson.hibernatesynchronizer.Constants;
import com.hudson.hibernatesynchronizer.util.HSUtil;

/**
 * @author <a href="mailto: joe@binamics.com">Joe Hudson </a>
 */
public class Template implements Comparable {

    public static final String TYPE_JAVA = "Java class";

    public static final String TYPE_RESOURCE = "Resource";

    private String name;

    private String description;

    private String content;

    protected File fileLocation;

    private String id;

    private String context;

    public Template() {
    }

    public Template(File file) throws IOException {
        restore(file);
    }

    public Template(InputStream is) throws IOException {
        restore(is);
    }

    /**
     * @return Returns the context.
     */
    public String getContext() {
        return context;
    }

    /**
     * @param context
     *            The context to set.
     */
    public void setContext(String context) {
        this.context = context;
    }

    public String getId() {
        if (null == fileLocation)
            return null;
        else {
            if (null == id) {
                String name = fileLocation.getName();
                int index = name.lastIndexOf('.');
                if (index >= 0) {
                    name = name.substring(0, index);
                }
                id = name;
            }
            return id;
        }
    }

    public String getFileName() {
        return fileLocation.getName();
    }

    /**
     * @return Returns the content.
     */
    public String getContent() {
        return content;
    }

    /**
     * @param content
     *            The content to set.
     */
    public void setContent(String content) {
        if (null == content)
            content = "";
        this.content = content;
    }

    /**
     * @return Returns the description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description
     *            The description to set.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return Returns the name.
     */
    public String getName() {
        return name;
    }

    /**
     * @param name
     *            The name to set.
     */
    public void setName(String name) {
        if (null != name)
            name = name.trim();
        this.name = name;
    }

    public File getFile() {
        return fileLocation;
    }

    public void save() throws IOException {
        save((String) null);
    }

    public void save(String name) throws IOException {
        FileOutputStream fos = null;
        try {
            if (null != fileLocation) {
                if (!fileLocation.exists()) {
                    try {
                        fileLocation.createNewFile();
                    } catch (Exception e) {
                    }
                }
                fos = new FileOutputStream(fileLocation);
            } else {
                if (null == name)
                    name = System.currentTimeMillis() + getExtension();
                else
                    name = name + getExtension();
                File directory = Constants.getCustomTemplateDirectory();
                String dirString = directory.getAbsolutePath().replace('\\',
                        '/');
                if (!dirString.endsWith("/"))
                    dirString = dirString + '/';
                String fileName = dirString + File.separator + name;
                fileLocation = new File(fileName);
                try {
                    fileLocation.createNewFile();
                } catch (Exception e) {
                }
                fos = new FileOutputStream(fileLocation);
            }
            fos.write(this.serialize());
            TemplateManager.getInstance().reloadTemplates();
        } finally {
            if (null != fos)
                fos.close();
        }
    }

    public boolean delete() throws IOException {
        if (null != fileLocation && fileLocation.exists()) {
            boolean rtn = fileLocation.delete();
            TemplateManager.getInstance().reloadTemplates();
            return rtn;
        } else {
            return false;
        }
    }

    public void restore(File file) throws IOException {
        restore(new FileInputStream(file));
        fileLocation = file;
    }

    protected String getSaveParameter(String s) {
        if (null == s)
            return "\n";
        else {
            s = s.replace('\n', ' ');
            return s + "\n";
        }
    }

    public int compareTo(Object o) {
        if (null == o || !(o instanceof Template))
            return -1;
        if (null == getName() || null == ((Template) o).getName())
            return 0;
        return getName().compareTo(((Template) o).getName());
    }

    public List validate() {
        List errors = new ArrayList();
        if (null == name || name.length() == 0) {
            errors.add("You must enter the name");
        } else if (name.indexOf(':') >= 0) {
            errors.add("The name must not contain a ':'");
        }
        return errors;
    }

    public boolean isJavaClass() {
        return (null != getContext() && getContext().equals(TYPE_JAVA));
    }

    public void restore(InputStream is) throws IOException {
        try {
            String s = HSUtil.getStringFromStream(is);
            int start = 0;
            int index = s.indexOf("\n");
            setName(s.substring(start, index));
            start = index + 1;
            index = s.indexOf("\n", start);
            setDescription(s.substring(start, index));
            start = index + 1;
            index = s.indexOf("\n", start);
            setContext(s.substring(start, index));
            String content = s.substring(index + 1, s.length());
            setContent(content);
        } finally {
            if (null != is)
                is.close();
        }
    }

    public String toString() {
        return new String(serialize());
    }

    public byte[] serialize() {
        StringBuffer sb = new StringBuffer();
        sb.append(getSaveParameter(getName()));
        sb.append(getSaveParameter(getDescription()));
        sb.append(getSaveParameter(getContext()));
        if (null != getContent())
            sb.append(getContent());
        return sb.toString().getBytes();
    }

    protected String getExtension() {
        return ".tmpl";
    }
}