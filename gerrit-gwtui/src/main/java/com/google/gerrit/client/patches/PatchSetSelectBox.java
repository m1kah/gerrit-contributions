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

import com.google.gerrit.client.Dispatcher;
import com.google.gerrit.client.Gerrit;
import com.google.gerrit.common.data.PatchScript;
import com.google.gerrit.common.data.PatchSetDetail;
import com.google.gerrit.reviewdb.client.Patch;
import com.google.gerrit.reviewdb.client.PatchSet;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwtorm.client.KeyUtil;

import java.util.LinkedList;
import java.util.List;

public class PatchSetSelectBox extends Composite {
  interface Binder extends UiBinder<HTMLPanel, PatchSetSelectBox> {
  }

  private static Binder uiBinder = GWT.create(Binder.class);

  interface BoxStyle extends CssResource {
    String selected();

    String hidden();

    String downloadLink();
  }

  public enum Side {
    A, B
  }

  PatchScript script;
  Patch.Key patchKey;
  PatchSet.Id idSideA;
  PatchSet.Id idSideB;
  PatchSet.Id idActive;
  Side side;
  PatchScreen.Type screenType;
  List<Anchor> links;

  @UiField
  HTMLPanel linkPanel;

  @UiField
  BoxStyle style;

  @UiField
  DivElement sideMarker;

  public PatchSetSelectBox(Side side, final PatchScreen.Type type) {
    this.side = side;
    this.screenType = type;

    initWidget(uiBinder.createAndBindUi(this));
  }

  public void display(final PatchSetDetail detail, final PatchScript script, Patch.Key key,
      PatchSet.Id idSideA, PatchSet.Id idSideB) {
    this.script = script;
    this.patchKey = key;
    this.idSideA = idSideA;
    this.idSideB = idSideB;
    this.idActive = (side == Side.A) ? idSideA : idSideB;
    this.links = new LinkedList<Anchor>();

    linkPanel.clear();

    if (screenType == PatchScreen.Type.UNIFIED) {
      sideMarker.setInnerText((side == Side.A) ? "(-)" : "(+)");
    } else {
      sideMarker.getStyle().setDisplay(Display.NONE);
    }

    Anchor baseLink = null;
    if (detail.getInfo().getParents().size() > 1) {
      baseLink = createLink(PatchUtil.C.patchBaseAutoMerge(), null);
    } else {
      baseLink = createLink(PatchUtil.C.patchBase(), null);
    }

    links.add(baseLink);
    if (screenType == PatchScreen.Type.UNIFIED || side == Side.A) {
      linkPanel.add(baseLink);
    }

    if (side == Side.B) {
      links.get(0).setStyleName(style.hidden());
    }

    for (Patch patch : script.getHistory()) {
      PatchSet.Id psId = patch.getKey().getParentKey();
      Anchor anchor = createLink(Integer.toString(psId.get()), psId);
      links.add(anchor);
      linkPanel.add(anchor);
    }

    if (idActive == null && side == Side.A) {
      links.get(0).setStyleName(style.selected());
    } else {
      links.get(idActive.get()).setStyleName(style.selected());
    }

    Anchor downloadLink = createDownloadLink();
    if (downloadLink != null) {
      linkPanel.add(downloadLink);
    }
  }

  private Anchor createLink(String label, final PatchSet.Id id) {
    final Anchor anchor = new Anchor(label);
    anchor.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (side == Side.A) {
          idSideA = id;
        } else {
          idSideB = id;
        }

        Patch.Key keySideB = new Patch.Key(idSideB, patchKey.get());

        switch (screenType) {
          case SIDE_BY_SIDE:
            Gerrit.display(Dispatcher.toPatchSideBySide(idSideA, keySideB));
            break;
          case UNIFIED:
            Gerrit.display(Dispatcher.toPatchUnified(idSideA, keySideB));
            break;
        }
      }

    });

    return anchor;
  }

  private Anchor createDownloadLink() {
    boolean isCommitMessage = Patch.COMMIT_MSG.equals(script.getNewName());

    if (isCommitMessage || (side == Side.A && 0 >= script.getA().size())
        || (side == Side.B && 0 >= script.getB().size())) {
      return null;
    }

    Patch.Key key =
        (idSideA == null) ? patchKey : (new Patch.Key(idSideA, patchKey.get()));

    String sideURL = (side == Side.A) ? "1" : "0";
    final String base = GWT.getHostPageBaseURL() + "cat/";

    Image image = new Image(Gerrit.RESOURCES.downloadIcon());

    final Anchor anchor = new Anchor();
    anchor.setHref(base + KeyUtil.encode(key.toString()) + "^" + sideURL);
    anchor.setTitle(PatchUtil.C.download());
    anchor.setStyleName(style.downloadLink());
    DOM.insertBefore(anchor.getElement(), image.getElement(),
        DOM.getFirstChild(anchor.getElement()));

    return anchor;
  }
}
