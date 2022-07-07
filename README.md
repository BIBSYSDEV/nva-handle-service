# nva-handle-service

API for creating handle for URI. The lambda needs to have a static IP to gain access to external handle database.

Request body:

{"uri":"[A VALID URI]"}

Response body:

{"handle":"[A CREATED HANDLE URI]"}
