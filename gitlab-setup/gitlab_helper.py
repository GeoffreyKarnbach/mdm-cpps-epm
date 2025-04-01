from dotenv import load_dotenv
import os
import configparser

load_dotenv()

config = configparser.ConfigParser()
config.read("gitlab.ini")
gitlab_base_url = config["GITLAB"]["BaseUrlApi"]

def return_url_and_headers(endpoint):
    url = f"{gitlab_base_url}/{endpoint}"
    headers = {
        "PRIVATE-TOKEN": os.getenv("GITLAB_ACCESS_TOKEN")
    }
    return url, headers

def create_slugs(name):
    return name.lower().replace(" ", "-")

def get_machine_public_key():
    return os.getenv("SSH_PUBLIC_KEY")