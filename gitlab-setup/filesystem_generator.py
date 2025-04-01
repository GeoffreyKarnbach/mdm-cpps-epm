import os
import uuid
import json
import time
from gitlab_helper import create_slugs
import subprocess
import shutil

def generate_from_json(data):
    """
    Steps:
    - Generate a unique directory name
    - Create the directory
    - Inside of the directory (for all the remaining steps):
        - Create a directory for the project name (convert to slug)
    
    """
    unique_id = uuid.uuid4().hex
    os.mkdir(unique_id)

    # Create a README file
    with open(f"{unique_id}/README.md", "w") as f:
        f.write(f"# {data['name']} - {data['version']} \n\n{data['description']}")

    # Create a gitignore file
    with open(f"{unique_id}/.gitignore", "w") as f:
        f.write("")

    # Create a config file
    os.mkdir(f"{unique_id}/.mdcppsepm")

    with open(f"{unique_id}/.mdcppsepm/config.json", "w") as f:
        f.write(json.dumps(data, indent=4))

    # Create empty gitlab-ci file
    with open(f"{unique_id}/.gitlab-ci.yml", "w") as f:
        f.write("")

    # Create a directory for the gitlab merge request template
    os.mkdir(f"{unique_id}/.gitlab")
    os.mkdir(f"{unique_id}/.gitlab/merge_requests_templates")

    with open(f"{unique_id}/.gitlab/merge_requests_templates/default.md", "w") as f:
        f.write("MERGE REQUEST TEMPLATE")

    os.mkdir(f"{unique_id}/.gitlab/issue_templates")
    with open(f"{unique_id}/.gitlab/issue_templates/default.md", "w") as f:
        f.write("ISSUE TEMPLATE")

    # Create a directory for the common workspace and the ccg file
    common_workspace = create_slugs(data["commonWorkspace"]["name"] + " Workspace")
    os.mkdir(f"{unique_id}/{common_workspace}")

    common_workspace_ccg = create_slugs(data["commonWorkspace"]["name"] + " Concepts") + ".ccg"
    with open(f"{unique_id}/{common_workspace}/{common_workspace_ccg}", "w") as f:
        f.write("")

    # Create a directory for each domain workspace and the cg file
    for workspace in data["domainWorkspaces"]:
        workspace_name = create_slugs(workspace["name"] + " Workspace")
        os.mkdir(f"{unique_id}/{workspace_name}")

        workspace_cg = create_slugs(workspace["name"] + " Concepts") + ".cg"
        with open(f"{unique_id}/{workspace_name}/{workspace_cg}", "w") as f:
            f.write("")

    return unique_id

def upload_to_gitlab_and_remove_locally(data, unique_id):
    os.chdir(unique_id)
    subprocess.run(["git", "init", "--initial-branch=main"], check=True)
    rootGroupName = data["gitlab"]["rootGroupName"]
    projectName = create_slugs(data["name"])
    subprocess.run(["git", "remote", "add", "origin", f"git@gitlab.com:{rootGroupName}/{projectName}.git"], check=True)
    subprocess.run(["git", "add", "."], check=True)
    subprocess.run(["git", "commit", "-m", "Initial commit"], check=True)
    subprocess.run(["git", "push", "--set-upstream", "origin", "main"], check=True)

    time.sleep(15)

    os.chdir("..")
    try:
        shutil.rmtree(unique_id)
    except Exception as e:
        print(f"Error removing directory: {e}")


if __name__ == "__main__":
    with open("sample_project.json") as f:
        data = json.load(f)

    generate_from_json(data)