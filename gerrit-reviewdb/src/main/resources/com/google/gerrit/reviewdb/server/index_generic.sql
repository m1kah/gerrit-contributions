-- Gerrit 2 : Generic
--

-- Indexes to support @Query
--

-- *********************************************************************
-- AccountAccess
--    covers:             byPreferredEmail, suggestByPreferredEmail
CREATE INDEX accounts_byPreferredEmail
ON accounts (preferred_email);

--    covers:             suggestByFullName
CREATE INDEX accounts_byFullName
ON accounts (full_name);


-- *********************************************************************
-- AccountExternalIdAccess
--    covers:             byAccount
CREATE INDEX account_external_ids_byAccount
ON account_external_ids (account_id);

--    covers:             byEmailAddress, suggestByEmailAddress
CREATE INDEX account_external_ids_byEmail
ON account_external_ids (email_address);


-- *********************************************************************
-- AccountGroupMemberAccess
--    @PrimaryKey covers: byAccount
CREATE INDEX account_group_members_byGroup
ON account_group_members (group_id);


-- *********************************************************************
-- AccountGroupIncludeAccess
--    @PrimaryKey covers: byGroup
CREATE INDEX account_group_includes_byInclude
ON account_group_includes (include_id);


-- *********************************************************************
-- AccountProjectWatchAccess
--    @PrimaryKey covers: byAccount
--    covers:             byProject
CREATE INDEX account_project_watches_byProject
ON account_project_watches (project_name);


-- *********************************************************************
-- AccountSshKeyAccess
--    @PrimaryKey covers: byAccount, valid


-- *********************************************************************
-- ApprovalCategoryAccess
--    too small to bother indexing


-- *********************************************************************
-- ApprovalCategoryValueAccess
--     @PrimaryKey covers: byCategory


-- *********************************************************************
-- BranchAccess
--    @PrimaryKey covers: byProject


-- *********************************************************************
-- ChangeAccess
--    covers:             byOwnerOpen
CREATE INDEX changes_byOwnerOpen
ON changes (open, owner_account_id, created_on, change_id);

--    covers:             byOwnerClosed
CREATE INDEX changes_byOwnerClosed
ON changes (open, owner_account_id, last_updated_on);

--    covers:             submitted, allSubmitted
CREATE INDEX changes_submitted
ON changes (status, dest_project_name, dest_branch_name, last_updated_on);

--    covers:             allOpenPrev, allOpenNext
CREATE INDEX changes_allOpen
ON changes (open, sort_key);

--    covers:             byProjectOpenPrev, byProjectOpenNext
CREATE INDEX changes_byProjectOpen
ON changes (open, dest_project_name, sort_key);

--    covers:             byProject
CREATE INDEX changes_byProject
ON changes (dest_project_name);

--    covers:             allClosedPrev, allClosedNext
CREATE INDEX changes_allClosed
ON changes (open, status, sort_key);

--    covers:             byBranchClosedPrev, byBranchClosedNext
CREATE INDEX changes_byBranchClosed
ON changes (status, dest_project_name, dest_branch_name, sort_key);

CREATE INDEX changes_key
ON changes (change_key);


-- *********************************************************************
-- PatchSetApprovalAccess
--    @PrimaryKey covers: byPatchSet, byPatchSetUser
--    covers:             openByUser
CREATE INDEX patch_set_approvals_openByUser
ON patch_set_approvals (change_open, account_id);

--    covers:             closedByUser
CREATE INDEX patch_set_approvals_closedByUser
ON patch_set_approvals (change_open, account_id, change_sort_key);


-- *********************************************************************
-- ChangeMessageAccess
--    @PrimaryKey covers: byChange


-- *********************************************************************
-- PatchLineCommentAccess
--    @PrimaryKey covers: published, draft
CREATE INDEX patch_comment_drafts
ON patch_comments (status, author_id);


-- *********************************************************************
-- PatchSetAncestorAccess
--    @PrimaryKey covers: ancestorsOf
--    covers:             descendantsOf
CREATE INDEX patch_set_ancestors_desc
ON patch_set_ancestors (ancestor_revision);


-- *********************************************************************
-- ProjectAccess
--    @PrimaryKey covers: all, suggestByName


-- *********************************************************************
-- TrackingIdAccess
--
CREATE INDEX tracking_ids_byTrkKey
ON tracking_ids (tracking_key);


-- *********************************************************************
-- StarredChangeAccess
--    @PrimaryKey covers: byAccount

CREATE INDEX starred_changes_byChange
ON starred_changes (change_id);

-- *********************************************************************
-- SubmoduleSubscriptionAccess

CREATE INDEX submodule_subscription_access_bySubscription
ON submodule_subscriptions (submodule_project_name, submodule_branch_name);
