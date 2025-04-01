from gitlab_helper import *
import requests

"""
Add a deploy key to a project

:param project_id: The id of the project
:param title: The title of the deploy key
:param key: The deploy key content (public key - of the form ssh-rsa ...)

The deploy key has write access to the repository
"""
def add_deploy_key_to_project(project_id, title, key):
    url, headers = return_url_and_headers(f"projects/{project_id}/deploy_keys")

    data = {
        "title": title,
        "key": key,
        "can_push": True
    }

    response = requests.post(url, headers=headers, data=data)
    response.raise_for_status()
    return response.json()

"""
Create a subgroup in a group

:param name: The name of the subgroup
:param parent_id: The id of the parent group
:param visibility: The visibility of the subgroup (default is private)

:return: The id of the created subgroup

The path of the subgroup is created by converting the name to lowercase and replacing spaces with hyphens (slug)
"""
def create_subgroup(name, parent_id, visibility = "private"):
    url, headers = return_url_and_headers("groups")

    data = {
        "name": name,
        "path": create_slugs(name),
        "parent_id": parent_id,
        "visibility": visibility
    }

    response = requests.post(url, headers=headers, data=data)
    response.raise_for_status()
    return response.json()["id"]

"""
Create multiple subgroups in a group

:param names: A list of names of the subgroups
:param parent_id: The id of the parent group
:param visibility: The visibility of the subgroups (default is private)

:return: A dictionary with the names as keys and the ids as values of the generated subgroups
"""
def create_subgroups(names, parent_id, visibility = "private"):
    response_map = {}

    for name in names:
        response_map[name] = create_subgroup(name, parent_id, visibility)

    return response_map

"""
Add a member to a group or project

:param is_group: A boolean value to determine if the entity is a group or project
:param entity_id: The id of the group or project
:param user_id: The id of the user to add
:param access_level: The access level of the user (default is 30)

TODO: Replace access_level by predetermined values depending on regular, reviewer or admin status
"""
def add_member_to(is_group, entity_id, user_id, access_level = 30):
    target = "groups" if is_group else "projects"

    url, headers = return_url_and_headers(f"{target}/{entity_id}/members")

    data = {
        "user_id": user_id,
        "access_level": access_level
    }

    response = requests.post(url, headers=headers, data=data)
    response.raise_for_status()
    return response.json()

"""
Add multiple members to a group or project

:param is_group: A boolean value to determine if the entity is a group or project
:param entity_id: The id of the group or project
:param user_ids: A list of user ids to add
:param access_level: The access level of the user (default is 30)
"""
def add_members_to_(is_group, entity_id, user_ids, access_level = 30):
    for user_id in user_ids:
        add_member_to(is_group, entity_id, user_id, access_level)


"""
Create a project in a group

:param name: The name of the project
:param namespace_id: The id of the group
:param visibility: The visibility of the project (default is private)

:return: The id of the created project
"""
def create_project(name, namespace_id, visibility = "private"):
    url, headers = return_url_and_headers("projects")

    data = {
        "name": name,
        "namespace_id": namespace_id,
        "visibility": visibility
    }

    response = requests.post(url, headers=headers, data=data)
    response.raise_for_status()
    return response.json()["id"]

"""
TODO: Add functions to edit and remove members from groups and projects
"""
