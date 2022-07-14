# nva-handle-service

Prerequisite: Cloudformation stack `template_vpc_eid.yml` should have been created. VPC-configuration of this service depends on this.

API for creating handle for URI. The lambda needs to have a static IP to gain access to external handle database.

Platform team have permission to open database firewall for this IP. The IP can be found in the AWS console at EC2 > Elastic IPs (find the one belonging to the stack created from `template_vpc_eid.yml`) 

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
