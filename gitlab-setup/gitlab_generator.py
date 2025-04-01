from gitlab_endpoints import *

def create_gitlab_project(data):
    name = data["name"]
    namespace_id = data["gitlab"]["rootGroupId"]
    return create_project(name, namespace_id)

def create_gitlab_subgroups(data):
    groups = []
    groups.append(data["commonWorkspace"]["name"])
    for workspace in data["domainWorkspaces"]:
        groups.append(workspace["name"])
    
    # For each group, add addtionally a group with " Reviewers" suffix (so 2 groups per workspace)
    reviewer_groups = [f"{group} Reviewers" for group in groups]
    groups.extend(reviewer_groups)

    parent_id = data["gitlab"]["rootGroupId"]

    return create_subgroups(groups, parent_id)

def add_gitlab_users_to_project(data, project_id):

    creator_user_id = data["gitlab"]["creatorUserId"]

    for user in data["users"]:
        user_id = user["gitlabId"]

        if str(user_id) != str(creator_user_id):
            print(f"Adding user {user_id} to project {project_id}")
            add_member_to(False, project_id, user_id)


def add_gitlab_users_to_subgroups(data, subgroup_ids):

    creator_user_id = data["gitlab"]["creatorUserId"]

    reviewer_users = []

    for user in data["users"]:
        user_id = user["gitlabId"]
        if user["isReviewer"] == True:
            reviewer_users.append(user_id)

    # Common workspace
    commonWorkspaceUsers = data["commonWorkspace"]["users"]
    commonWorkspaceUsers = [user for user in commonWorkspaceUsers if str(user) != str(creator_user_id)]

    commonWorkspaceReviewers = [user for user in data["commonWorkspace"]["users"] if user in reviewer_users]
    commonWorkspaceReviewers = [user for user in commonWorkspaceReviewers if str(user) != str(creator_user_id)]

    commonWorkspaceSubgroupId = subgroup_ids[data["commonWorkspace"]["name"]]
    commonWorkspaceReviewersSubgroupId = subgroup_ids[f"{data['commonWorkspace']['name']} Reviewers"]

    print(f"Adding users to common workspace: {commonWorkspaceUsers}")
    print(f"Adding reviewers to common workspace: {commonWorkspaceReviewers}")

    add_members_to_(True, commonWorkspaceSubgroupId, commonWorkspaceUsers)
    add_members_to_(True, commonWorkspaceReviewersSubgroupId, commonWorkspaceReviewers)

    # Domain workspaces
    for workspace in data["domainWorkspaces"]:
        workspaceUsers = workspace["users"]
        workspaceUsers = [user for user in workspaceUsers if str(user) != str(creator_user_id)]

        workspaceReviewers = [user for user in workspace["users"] if user in reviewer_users]
        workspaceReviewers = [user for user in workspaceReviewers if str(user) != str(creator_user_id)]

        workspaceSubgroupId = subgroup_ids[workspace["name"]]
        workspaceReviewersSubgroupId = subgroup_ids[f"{workspace['name']} Reviewers"]

        print(f"Adding users to workspace {workspace['name']}: {workspaceUsers}")
        print(f"Adding reviewers to workspace {workspace['name']}: {workspaceReviewers}")

        add_members_to_(True, workspaceSubgroupId, workspaceUsers)
        add_members_to_(True, workspaceReviewersSubgroupId, workspaceReviewers)


    