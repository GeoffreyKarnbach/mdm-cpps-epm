# Relevant Gitlab API routes

|            |                    |
| ---------- | ------------------ |
| Gitlab URL | https://gitlab.com |
| API Prefix | /api/v4            |

## Structure of requests

```
curl --request GET/POST/PUT/DELTE url
--header "PRIVATE-TOKEN: <access_token>"
--data "param_name=param_value"
```

## Create a Group

Path: `POST /groups`<br><br>
Parameters:

- name: string
- path: string

## Environment variables

You need to create a ".env" file with following content to be able to work with the gitlab setup tool in standalone mode:

```
GITLAB_ACCESS_TOKEN=XXXXXX
SSH_PUBLIC_KEY=XXXXXXX
```
