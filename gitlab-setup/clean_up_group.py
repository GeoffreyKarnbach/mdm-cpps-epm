from gitlab_helper import *
import requests

groupId = 93643489
creatorUserId = 22663746

def clean_up_subprojects(groupId):
    url, headers = return_url_and_headers(f"groups/{groupId}/projects")
    response = requests.get(url, headers=headers)
    response.raise_for_status()
    projects = response.json()
    for project in projects:
        delete_project(project["id"])
    print(f"Group {groupId} cleaned up") 

# Remove all subgroups from a group
def clean_up_group(groupId):
    url, headers = return_url_and_headers(f"groups/{groupId}/subgroups")
    response = requests.get(url, headers=headers)
    response.raise_for_status()
    subgroups = response.json()
    for subgroup in subgroups:
        delete_subgroup(subgroup["id"])
    print(f"Group {groupId} cleaned up")

def delete_subgroup(subgroupId):
    url, headers = return_url_and_headers(f"groups/{subgroupId}")
    response = requests.delete(url, headers=headers)
    response.raise_for_status()
    print(f"Subgroup {subgroupId} deleted")

def delete_project(projectId):
    url, headers = return_url_and_headers(f"projects/{projectId}")
    response = requests.delete(url, headers=headers)
    response.raise_for_status()
    print(f"Project {projectId} deleted")

def remove_all_billable_users_from_group(groupId):
    url, headers = return_url_and_headers(f"groups/{groupId}/billable_members")
    response = requests.get(url, headers=headers)
    response.raise_for_status()
    members = response.json()
    for member in members:
        if member["id"] != creatorUserId:
            remove_member_from_group(groupId, member["id"])
    print(f"Group {groupId} cleaned up")

def remove_member_from_group(groupId, userId):
    url, headers = return_url_and_headers(f"groups/{groupId}/billable_members/{userId}")
    response = requests.delete(url, headers=headers)
    response.raise_for_status()
    print(f"Member {userId} removed from group {groupId}")

if __name__ == "__main__":
    clean_up_subprojects(groupId)
    clean_up_group(groupId)
    remove_all_billable_users_from_group(groupId)