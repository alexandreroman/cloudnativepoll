# Cloud-native Poll app

This project is a cloud-native implementation of a poll app, using modern technologies
like [Spring Boot](https://spring.io/projects/spring-boot),
[Spring Cloud](https://spring.io/projects/spring-cloud) and
[Spring Data](https://spring.io/projects/spring-data).

## Compiling this app

```bash
$ ./mvwn clean package
```

## Running locally

```bash
$ docker run --rm -p 6379:6379/tcp redis:5
```

```bash
$ docker run --rm -p 5672:5672/tcp -p 15672:15672/tcp rabbitmq:3-management
```

```bash
$ java -jar backend/target/cloudnativepoll-eureka-server.jar
```

```bash
$ java -jar backend/target/cloudnativepoll-backend.jar
```

```bash
$ java -jar backend/target/cloudnativepoll-webui.jar
```

## Deploying to Pivotal Platform

```bash
$ cf create-service p-redis shared-vm redis
$ cf create-service p-rabbitmq standard rabbitmq
$ cf create-service p-service-registry standard service-registry
$ cf create-service -c '{"git": { "uri": "https://github.com/alexandreroman/cloudnativepoll-config", "cloneOnStart": "true" }}' p-config-server standard config-server
$ cf push
$ cf add-network-policy cloudnativepoll-webui --destination-app cloudnativepoll-backend
```

## Contribute

Contributions are always welcome!

Feel free to open issues & send PR.

## License

Copyright &copy; 2019 [Pivotal Software, Inc](https://pivotal.io).

This project is licensed under the [Apache Software License version 2.0](https://www.apache.org/licenses/LICENSE-2.0).
