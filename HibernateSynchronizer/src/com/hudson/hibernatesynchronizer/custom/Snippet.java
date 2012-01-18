/**
 * This software is licensed under the general public license.  See http://www.gnu.org/copyleft/gpl.html
 * for more information.
 */
package com.hudson.hibernatesynchronizer.custom;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.resources.IProject;

import com.hudson.hibernatesynchronizer.Constants;
import com.hudson.hibernatesynchronizer.util.HSUtil;

/**
 * @author <a href="mailto: joe@binamics.com">Joe Hudson </a>
 */
public class Snippet extends Template {
    private boolean untouched;

    /**
     *  
     */
    public Snippet() {
        super();
    }

    /**
     * @param file
     * @throws IOException
     */
    public Snippet(File file) throws IOException {
        super(file);
    }

    /**
     * @param is
     * @throws IOException
     */
    public Snippet(InputStream is) throws IOException {
        super(is);
    }

    public void save(IProject project) throws IOException {
        if (null == fileLocation) {
            File sDir = Constants.getProjectSnippetDirectory(project);
            if (!sDir.exists())
                sDir.mkdir();
            fileLocation = new File(sDir + "/" + System.currentTimeMillis()
                    + getExtension());
        }
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
                File directory = Constants.getSnippetDirectory();
                String dirString = directory.getAbsolutePath().replace('\\',
                        '/');
                if (!dirString.endsWith("/"))
                    dirString = dirString + '/';
                String fileName = dirString + File.separator + name;
                fileLocation = new File(fileName);
                try {
                    fileLocation.createNewFile();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                fos = new FileOutputStream(fileLocation);
            }
            fos.write(this.serialize());
            TemplateManager.getInstance().reloadSnippets();
        } finally {
            if (null != fos)
                fos.close();
        }
    }

    public boolean delete() throws IOException {
        if (null != fileLocation && fileLocation.exists()) {
            boolean rtn = fileLocation.delete();
            TemplateManager.getInstance().reloadSnippets();
            return rtn;
        } else {
            return false;
        }
    }

    public void restore(InputStream is) throws IOException {
        try {
            String s = HSUtil.getStringFromStream(is);
            int start = 0;
            int index = s.indexOf("\n");
            setName(s.substring(start, index).trim());
            start = index + 1;
            index = s.indexOf("\n", start);
            setDescription(s.substring(start, index).trim());
            String content = s.substring(index + 1, s.length());
            setContent(content);
        } finally {
            if (null != is)
                is.close();
        }
    }

    public int compareTo(Object o) {
        if (null == o || !(o instanceof Snippet))
            return -1;
        if (null != getFileName() && null != ((Snippet) o).getFileName()) {
            if (getFileName().startsWith("_")
                    && !((Snippet) o).getFileName().startsWith("_")) {
                return 1;
            } else if (!getFileName().startsWith("_")
                    && ((Snippet) o).getFileName().startsWith("_")) {
                return -1;
            }
        }
        if (null == getName() || null == ((Template) o).getName())
            return 0;
        return getName().compareTo(((Template) o).getName());
    }

    public String toString() {
        return new String(serialize());
    }

    public byte[] serialize() {
        StringBuffer sb = new StringBuffer();
        sb.append(getSaveParameter(getName()));
        sb.append(getSaveParameter(getDescription()));
        if (null != getContent())
            sb.append(getContent());
        return sb.toString().getBytes();
    }

    public boolean isCustom() {
        return !getFileName().startsWith("_");
    }

    public boolean isModified() {
        return !untouched;
    }

    public void setName(String name) {
        if (name.startsWith(":")) {
            name = name.substring(1, name.length());
            untouched = true;
        } else {
            untouched = false;
        }
        super.setName(name);
    }
}