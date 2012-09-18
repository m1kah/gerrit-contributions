// Copyright (C) 2012 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.gerrit.client.patches;

import com.google.gerrit.client.Gerrit;
import com.google.gerrit.client.changes.PatchTable;
import com.google.gerrit.client.changes.Util;
import com.google.gerrit.client.patches.AbstractPatchContentTable.InsertCommentCommand;
import com.google.gerrit.client.patches.AbstractPatchContentTable.NextChunkKeyCmd;
import com.google.gerrit.client.patches.AbstractPatchContentTable.NextCommentCmd;
import com.google.gerrit.client.patches.AbstractPatchContentTable.NoOpKeyCommand;
import com.google.gerrit.client.patches.AbstractPatchContentTable.PrevChunkKeyCmd;
import com.google.gerrit.client.patches.AbstractPatchContentTable.PrevCommentCmd;
import com.google.gerrit.client.patches.AbstractPatchContentTable.PublishCommentsKeyCommand;
import com.google.gerrit.client.patches.PatchScreen.TopView;
import com.google.gerrit.client.rpc.GerritCallback;
import com.google.gerrit.client.ui.ListenableAccountDiffPreference;
import com.google.gerrit.client.ui.Screen;
import com.google.gerrit.client.ui.NavigationTable.NextKeyCommand;
import com.google.gerrit.client.ui.NavigationTable.OpenKeyCommand;
import com.google.gerrit.client.ui.NavigationTable.PrevKeyCommand;
import com.google.gerrit.common.data.ChangeDetail;
import com.google.gerrit.common.data.PatchScript;
import com.google.gerrit.common.data.PatchSetDetail;
import com.google.gerrit.reviewdb.client.AccountDiffPreference;
import com.google.gerrit.reviewdb.client.Patch;
import com.google.gerrit.reviewdb.client.PatchSet;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwtexpui.globalkey.client.GlobalKey;
import com.google.gwtexpui.globalkey.client.KeyCommand;
import com.google.gwtexpui.globalkey.client.KeyCommandSet;
import com.google.gwtjsonrpc.common.AsyncCallback;

import java.util.ArrayList;
import java.util.List;

public class AllPatchesScreen extends Screen implements CommentEditorContainer {
  private static class NoOPKeyCommand extends KeyCommand {
    interface Action {
      void onKeyPress(KeyPressEvent event);
    }

    private Action action;

    public NoOPKeyCommand(int mask, char key, String help) {
      super(mask, key, help);
    }

    public NoOPKeyCommand(int mask, int key, String help) {
      super(mask, key, help);
    }

    public static NoOPKeyCommand newCommand(int mask, char key, String help, Action action) {
      NoOPKeyCommand command = new NoOPKeyCommand(mask, key, help);
      command.action = action;
      return command;
    }

    @Override
    public void onKeyPress(KeyPressEvent event) {
      action.onKeyPress(event);
    }
  }

  private final PatchSet.Id patchSetId;
  private final PatchSet.Id baseId;
  private ListenableAccountDiffPreference prefs;
  private FlowPanel contentPanel;
  private PatchTable patchTable;
  private KeyCommandSet keysNavigation;
  private KeyCommandSet keysAction;
  private HandlerRegistration regNavigation;
  private HandlerRegistration regAction;
  private KeyCommandSet keysComment;
  private HandlerRegistration regComment;
  private KeyCommandSet keysOpenByEnter;
  private HandlerRegistration regOpenByEnter;
  private AbstractPatchContentTable focusedContentTable;
  private List<AbstractPatchContentTable> contentTables = new ArrayList<AbstractPatchContentTable>();

  public AllPatchesScreen(final PatchSet.Id patchSetId, final PatchSet.Id baseId, final PatchSetDetail detail,
      final PatchTable patchTable) {
    this.patchSetId = patchSetId;
    this.baseId = baseId;
    prefs = new ListenableAccountDiffPreference();
    prefs.reset();
  }

  @Override
  protected void onInitUI() {
    super.onInitUI();

    contentPanel = new FlowPanel();
    contentPanel.setStyleName(Gerrit.RESOURCES.css()
        .sideBySideScreenSideBySideTable());
    add(contentPanel);
  }

  @Override
  protected void onLoad() {
    super.onLoad();

    setupKeys();
    loadPatchSet();
  }

  private void setupKeys() {
    keysNavigation = new KeyCommandSet(Gerrit.C.sectionNavigation());
    keysNavigation.add(new UpToChangeCommand(patchSetId, 0, 'u'));

    keysNavigation.add(new NoOPKeyCommand(0, 'k', Util.C.patchTablePrev()));

    keysNavigation.add(new NoOPKeyCommand(0, 'j', Util.C.patchTableNext()));
    keysNavigation.add(new NoOPKeyCommand(0, 'o', Util.C.patchTableOpenDiff()));
    keysNavigation.add(new NoOPKeyCommand(0, KeyCodes.KEY_ENTER, Util.C
        .patchTableOpenDiff()));
    keysNavigation.add(new NoOPKeyCommand(0, 'O', Util.C
        .patchTableOpenUnifiedDiff()));

    keysNavigation.add(new NoOPKeyCommand(0, '[', PatchUtil.C.previousFileHelp()));
    keysNavigation.add(new NoOPKeyCommand(0, ']', PatchUtil.C.nextFileHelp()));
    keysNavigation.add(new NoOPKeyCommand(0, 'k', PatchUtil.C.linePrev()));
    keysNavigation.add(new NoOPKeyCommand(0, 'j', PatchUtil.C.lineNext()));
    keysNavigation.add(new NoOPKeyCommand(0, 'p', PatchUtil.C.chunkPrev()));
    keysNavigation.add(new NoOPKeyCommand(0, 'n', PatchUtil.C.chunkNext()));
    keysNavigation.add(new NoOPKeyCommand(0, 'P', PatchUtil.C.commentPrev()));
    keysNavigation.add(new NoOPKeyCommand(0, 'N', PatchUtil.C.commentNext()));

    keysAction = new KeyCommandSet(Gerrit.C.sectionActions());
    keysAction.add(new NoOPKeyCommand(0, 'o', PatchUtil.C.expandComment()));
    keysOpenByEnter = new KeyCommandSet(Gerrit.C.sectionNavigation());
    keysOpenByEnter.add(new NoOPKeyCommand(0, KeyCodes.KEY_ENTER, PatchUtil.C.expandComment()));

    if (Gerrit.isSignedIn()) {
      keysAction.add(new NoOPKeyCommand(0, 'c', PatchUtil.C
          .commentInsert()));
      keysAction.add(new NoOPKeyCommand(0, 'r', Util.C
          .keyPublishComments()));

      // See CommentEditorPanel
      //
      keysComment = new KeyCommandSet(PatchUtil.C.commentEditorSet());
      keysComment.add(new NoOpKeyCommand(KeyCommand.M_CTRL, 's', PatchUtil.C
          .commentSaveDraft()));
      keysComment.add(new NoOpKeyCommand(0, KeyCodes.KEY_ESCAPE, PatchUtil.C
          .commentCancelEdit()));
    }
  }

  @Override
  public void onUnload() {
    unregisterKeys();
  }

  @Override
  public void registerKeys() {
    unregisterKeys();
    if (keysNavigation != null) {
      regNavigation = GlobalKey.add(this, keysNavigation);
    }
    if (keysAction != null) {
      regAction = GlobalKey.add(this, keysAction);
    }
    if (keysComment != null) {
      regComment = GlobalKey.add(this, keysComment);
    }
    if (keysOpenByEnter != null) {
      regOpenByEnter = GlobalKey.add(this, keysOpenByEnter);
    }
  }

  private void unregisterKeys() {
    if (regAction != null) {
      regAction.removeHandler();
      regAction = null;
    }
    if (regComment != null) {
      regComment.removeHandler();
      regComment = null;
    }
    if (regNavigation != null) {
      regNavigation.removeHandler();
      regNavigation = null;
    }
    if (regOpenByEnter != null) {
      regOpenByEnter.removeHandler();
      regOpenByEnter = null;
    }
  }

  private void loadPatchSet() {
    Util.DETAIL_SVC.patchSetDetail(patchSetId, new GerritCallback<PatchSetDetail>() {
      @Override
      public void onSuccess(PatchSetDetail result) {
        displayPatchSetDetail(result);
        display();
      }
    });
  }

  private void displayPatchSetDetail(final PatchSetDetail patchSetDetail) {
    for (final Patch patch : patchSetDetail.getPatches()) {
      final PatchSet.Id idSideA = baseId;
      final PatchSet.Id idSideB = patchSetDetail.getPatchSet().getId();
      PatchUtil.DETAIL_SVC.patchScript(patch.getKey(),
          idSideA, idSideB, prefs.get(), new GerritCallback<PatchScript>() {
            @Override
            public void onSuccess(PatchScript result) {
              SideBySideTable table = new SideBySideTable();
              PatchTable patchTable = new PatchTable();
              patchTable.display(idSideA, patchSetDetail);
              table.fileList = patchTable;

              table.display(patch.getKey(), idSideA, idSideB, result);

              table.finishDisplay();

              contentPanel.add(new Label(patch.getFileName()));
              PatchTableHeader header = new PatchTableHeader(PatchScreen.Type.SIDE_BY_SIDE);
              header.display(patchSetDetail, result, patch.getKey(), idSideA, idSideB);
              contentPanel.add(header);
              contentPanel.add(table);

              patchTable.setActive(false);
              contentTables.add(table);
            }
          });
    }
  }

  @Override
  public void notifyDraftDelta(int delta) {
    // TODO Auto-generated method stub

  }

  @Override
  public void remove(CommentEditorPanel panel) {
    // TODO Auto-generated method stub

  }

}
