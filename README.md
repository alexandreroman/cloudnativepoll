# Cloud-native Poll app

This project is a cloud-native implementation of a voting app, using modern technologies
like [Spring Boot](https://spring.io/projects/spring-boot),
[Spring Cloud](https://spring.io/projects/spring-cloud) and
[Spring Data](https://spring.io/projects/spring-data).

You can freely set up your own questions and thumbnails, by setting configuration
through Spring properties.

<img src="app-screenshot.png"/>

## Compiling this app

This app is made of 2 microservices: a [backend](backend) and a
[Web UI frontend](frontend). These microservices leverage a RabbitMQ instance to share
messages and a Redis instance to store data.

You need a JDK 11+ to build this app:
```bash
$ ./mvwn clean package
```

## Running locally

Start a Redis instance using Docker:
```bash
$ docker run --rm -p 6379:6379/tcp redis:5
```

Start a RabbitMQ instance:
```bash
$ docker run --rm -p 5672:5672/tcp -p 15672:15672/tcp rabbitmq:3-management
```

The RabbitMQ management UI is available at http://localhost:15672:
use `guest` / `guest` to sign in.

Start the backend:
```bash
$ java -jar backend/target/cloudnativepoll-backend.jar
```

Start the frontend:
```bash
$ java -jar frontend/target/cloudnativepoll-webui.jar
```

The app is available at http://localhost:8080.

## Contribute

Contributions are always welcome!

Feel free to open issues & send PR.

## License

Copyright &copy; 2021 [VMware, Inc. or its affiliates](https://vmware.com).

This project is licensed under the [Apache Software License version 2.0](https://www.apache.org/licenses/LICENSE-2.0).
