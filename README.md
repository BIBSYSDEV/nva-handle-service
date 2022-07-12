# nva-handle-service

API for creating handle for URI. The lambda needs to have a static IP to gain access to external handle database.

Valid request body JSON:

```json
{
    "uri" : "<A VALID URI>"
}
```

Success response body:

```json
{
    "handle" : "<A CREATED HANDLE URI>"
}
```
