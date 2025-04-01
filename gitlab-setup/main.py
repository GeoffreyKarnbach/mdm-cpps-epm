from json import load
from gitlab_generator import *
from gitlab_helper import *
from filesystem_generator import generate_from_json, upload_to_gitlab_and_remove_locally


def setup_gitlab_from_json(data):
    
    # Step 1: Create the project
    project_id = create_gitlab_project(data)
    print("--- Project created ---")
    print(f"Project ID: {project_id}")

    # Step 2: Create subgroups for workspaces
    subgroup_ids = create_gitlab_subgroups(data)
    print("--- Subgroups created ---")
    print(subgroup_ids)
    
    # Step 3: Add users to the project
    add_gitlab_users_to_project(data, project_id)
    print("--- Users added to project ---")

    # Step 4: Add users to the subgroups
    add_gitlab_users_to_subgroups(data, subgroup_ids)
    print("--- Users added to subgroups ---")

    # Step 5: Add deploy key to the project
    deploy_key = get_machine_public_key()
    add_deploy_key_to_project(project_id, "MDCPPS-EPM", deploy_key)
    print("--- Deploy key added ---")
 
    # Step 6: Generate file structure
    unique_id = generate_from_json(data)
    print("--- File structure generated ---")

    
    # Step 7: Push the project to GitLab
    upload_to_gitlab_and_remove_locally(data, unique_id)
    print("--- Project pushed to GitLab ---")

    print("--- Setup complete ---")

if __name__ == "__main__":
    with open("sample_project.json") as f:
        data = load(f)
        
    setup_gitlab_from_json(data)
