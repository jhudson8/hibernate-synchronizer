/**
 * This software is licensed under the general public license.  See http://www.gnu.org/copyleft/gpl.html
 * for more information.
 */
package com.hudson.hibernatesynchronizer.popup.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

/**
 * @author <a href="mailto: joe@binamics.com">Joe Hudson </a>
 */
public class SynchronizeOverwriteFiles extends SynchronizeFiles {
    public boolean force() {
        return true;
    }

    public void run(IAction action) {
        Shell shell = new Shell();
        boolean rtn = MessageDialog
                .openConfirm(shell, "File Overwrite Confirmation",
                        "Are you sure you want to overwrite all files with the template generation?");
        if (rtn) {
            super.run(action);
        }
    }
}