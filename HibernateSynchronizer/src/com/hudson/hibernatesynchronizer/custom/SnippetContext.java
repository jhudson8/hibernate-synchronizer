/**
 * This software is licensed under the general public license.  See http://www.gnu.org/copyleft/gpl.html
 * for more information.
 */
package com.hudson.hibernatesynchronizer.custom;

import java.io.StringWriter;

import org.apache.velocity.app.Velocity;
import org.apache.velocity.context.Context;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

import com.hudson.hibernatesynchronizer.Constants;

/**
 * @author <a href="mailto: joe@binamics.com">Joe Hudson </a>
 */
public class SnippetContext {

    private Context context;

    private IProject project;

    public SnippetContext(Context context, IProject project) {
        this.context = context;
        this.project = project;
    }

    public String get(String snippetName) {
        try {
            Snippet s = TemplateManager.getInstance().findSnippetByName(
                    snippetName, project);
            if (null == s)
                return null;
            else {
                StringWriter sw = new StringWriter();
                Constants.customGenerator.evaluate(context, sw, Velocity.class
                        .getName(), s.getContent());
                return sw.toString();
            }
        } catch (Exception e) {
            MessageDialog.openError(new Shell(), "Error", e.getMessage());
            return null;
        }
    }
}