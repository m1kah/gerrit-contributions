// Copyright (C) 2009 The Android Open Source Project
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

package com.google.gerrit.common.data;

import com.google.gerrit.common.audit.Audit;
import com.google.gerrit.common.auth.SignInRequired;
import com.google.gerrit.reviewdb.client.PatchSet;
import com.google.gwtjsonrpc.common.AsyncCallback;
import com.google.gwtjsonrpc.common.RemoteJsonService;
import com.google.gwtjsonrpc.common.RpcImpl;
import com.google.gwtjsonrpc.common.VoidResult;
import com.google.gwtjsonrpc.common.RpcImpl.Version;

@RpcImpl(version = Version.V2_0)
public interface ChangeManageService extends RemoteJsonService {
  @Audit
  @SignInRequired
  void submit(PatchSet.Id patchSetId, AsyncCallback<ChangeDetail> callback);

  @Audit
  @SignInRequired
  void abandonChange(PatchSet.Id patchSetId, String message,
      AsyncCallback<ChangeDetail> callback);

  @Audit
  @SignInRequired
  void revertChange(PatchSet.Id patchSetId, String message,
      AsyncCallback<ChangeDetail> callback);

  @Audit
  @SignInRequired
  void restoreChange(PatchSet.Id patchSetId, String message,
      AsyncCallback<ChangeDetail> callback);

  @Audit
  @SignInRequired
  void publish(PatchSet.Id patchSetId, AsyncCallback<ChangeDetail> callback);

  @Audit
  @SignInRequired
  void deleteDraftChange(PatchSet.Id patchSetId, AsyncCallback<VoidResult> callback);

  @Audit
  @SignInRequired
  void rebaseChange(PatchSet.Id patchSetId, AsyncCallback<ChangeDetail> callback);
}
