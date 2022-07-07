# nva-handle-service

API for creating handle for URI. The lambda needs to have a static IP to gain access to external handle database.

Valid request body JSON:

```
{
    "uri" : "<A VALID URI>"
}
```

Success response body:

```
{
    "handle" : "<A CREATED HANDLE URI>"
}
```
