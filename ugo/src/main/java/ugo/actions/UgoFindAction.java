package ugo.actions;

/* ###
 * IP: GHIDRA
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import docking.action.KeyBindingData;
import docking.action.MenuData;
import docking.widgets.CursorPosition;
import docking.widgets.FindDialog;
import docking.widgets.FindDialogSearcher;
import docking.widgets.SearchLocation;
import docking.widgets.fieldpanel.field.Field;
import docking.widgets.fieldpanel.support.FieldLocation;
import ghidra.app.decompiler.component.ClangTextField;
import ghidra.app.plugin.core.decompile.DecompilerActionContext;
import ghidra.framework.plugintool.PluginTool;
import ghidra.util.HelpLocation;
import ugo.UgoDecompilerController;
import ugo.UgoDecompilerPanel;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.List;

public class UgoFindAction extends UgoAbstractDecompilerAction {
    private FindDialog findDialog;
    private final UgoDecompilerPanel decompilerPanel;
    private final PluginTool tool;

    public UgoFindAction(PluginTool tool, UgoDecompilerController controller) {
        super("Find");
        this.tool = tool;
        this.decompilerPanel = controller.getDecompilerPanel();
        setPopupMenuData(new MenuData(new String[]{"Find..."}, "Decompile"));
        setKeyBindingData(new KeyBindingData(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK));
        setEnabled(true);
    }

    protected FindDialog getFindDialog() {
        if (findDialog == null) {
            findDialog = new FindDialog("Decompiler Find Text", new DecompilerSearcher()) {
                @Override
                protected void dialogClosed() {
                    // clear the search results when the dialog is closed
                    decompilerPanel.setSearchResults(null);
                }
            };
            findDialog.setHelpLocation(new HelpLocation("DecompilePlugin", "Find"));
        }
        return findDialog;
    }

    private class DecompilerSearcher implements FindDialogSearcher {

        @Override
        public CursorPosition getCursorPosition() {
            FieldLocation fieldLocation = decompilerPanel.getCursorPosition();
            return new UgoDecompilerCursorPosition(fieldLocation);
        }

        @Override
        public CursorPosition getStart() {

            int lineNumber = 0;
            int fieldNumber = 0; // always 0, as the field is the entire line and it is the only field
            int column = 0; // or length for the end
            FieldLocation fieldLocation = new FieldLocation(lineNumber, fieldNumber, 0, column);
            return new UgoDecompilerCursorPosition(fieldLocation);
        }

        @Override
        public CursorPosition getEnd() {

            List<Field> lines = decompilerPanel.getFields();
            int lineNumber = lines.size() - 1;
            ClangTextField textLine = (ClangTextField) lines.get(lineNumber);

            int fieldNumber = 0; // always 0, as the field is the entire line and it is the only field
            int rowCount = textLine.getNumRows();
            int row = rowCount - 1; // 0-based
            int column = textLine.getNumCols(row);
            FieldLocation fieldLocation = new FieldLocation(lineNumber, fieldNumber, row, column);
            return new UgoDecompilerCursorPosition(fieldLocation);
        }

        @Override
        public void setCursorPosition(CursorPosition position) {
            decompilerPanel.setCursorPosition(
                    ((UgoDecompilerCursorPosition) position).getFieldLocation());
        }

        @Override
        public void highlightSearchResults(SearchLocation location) {
            decompilerPanel.setSearchResults(location);
        }

        @Override
        public SearchLocation search(String text, CursorPosition position, boolean searchForward,
                                     boolean useRegex) {
            UgoDecompilerCursorPosition UgoDecompilerCursorPosition = (UgoDecompilerCursorPosition) position;
            FieldLocation fieldLocation = UgoDecompilerCursorPosition.getFieldLocation();
            return useRegex ? decompilerPanel.searchTextRegex(text, fieldLocation, searchForward)
                    : decompilerPanel.searchText(text, fieldLocation, searchForward);
        }

    }

    @Override
    protected boolean isEnabledForDecompilerContext(DecompilerActionContext context) {
        return true;
    }

    @Override
    protected void decompilerActionPerformed(DecompilerActionContext context) {
        FindDialog dialog = getFindDialog();
        String text = decompilerPanel.getHighlightedText();
        if (text != null) {
            dialog.setSearchText(text);
        }

        // show over the root frame, so the user can still see the Decompiler window
        tool.showDialog(dialog);
    }
}
