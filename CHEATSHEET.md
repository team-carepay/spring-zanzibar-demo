# Zed CLI :

```
zed import spicedb-backup.yaml
zed import spicedb-caveatsbackup.yaml

zed permission check book:pride_and_prejudice view user:john --explain
zed permission check book:pride_and_prejudice view user:john --caveat-context {"ip":"10.2.3.4"} --explain

```


# Docker compose

```
docker compose up -d
docker compose down
```



# Links

* [Keycloak](https://localhost:9080)
* [GraphiQL](https://localhost:5000/graphiql)
* [Red Panda](https://localhost:9133)
* [Playground](https://play.authzed.com/)
