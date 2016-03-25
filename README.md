# FIWARE NGSI API

[![Build Status](https://travis-ci.org/Orange-OpenSource/fiware-ngsi2-api.svg?branch=master)](https://travis-ci.org/Orange-OpenSource/fiware-ngsi2-api) [![Coverity Scan Status](https://scan.coverity.com/projects/7943/badge.svg)](https://scan.coverity.com/projects/7943) [![Coverage Status](https://coveralls.io/repos/github/Orange-OpenSource/fiware-ngsi2-api/badge.svg?branch=master)](https://coveralls.io/github/Orange-OpenSource/fiware-ngsi2-api?branch=master) 
[![Client Doc](https://img.shields.io/badge/client%20doc-latest-brightgreen.svg)](http://www.javadoc.io/doc/com.orange.fiware/ngsi2-client)
[![Server Doc](https://img.shields.io/badge/server%20doc-latest-brightgreen.svg)](http://www.javadoc.io/doc/com.orange.fiware/ngsi2-server)
[![Apache Version 2 Licence](https://img.shields.io/badge/License-Apache%20Version%202-blue.svg)](LICENSE.txt)

This project is a Java library for the [NGSI v2 API](http://telefonicaid.github.io/fiware-orion/api/v2/)

This library was originally created for the [Fiware-Cepheus](https://github.com/Orange-OpenSource/fiware-cepheus) project. A library implementing NGSI v1 API can be found at Orange-OpenSource/fiware-ngsi-api

What remains to be done:
- [ ] Simplified Entity Representation.
- [ ] Virtual Attribute.
- [ ] NotifyContext.
- [ ] NotifyContextAvailability.

## Usage

### Client

The client is based on Spring `AsyncRestTemplate` HTTP client.

Add the client library to your `pom.xml`:

```xml
<dependency>
    <groupId>com.orange.fiware</groupId>
    <artifactId>ngsi2-client</artifactId>
    <version>X.Y.Z</version>
</dependency>
```

Get an instance of `NgsiClient` and provide the `AsyncRestTemplate` (a default one or customized to your needs) and a base URL for the NGSIv2 server to target:

```java
AsyncRestTemplate asyncRestTemplate = new AsyncRestTemplate();
String baseURL = "http://server.org/";
Ngsi2Client client = new Ngsi2Client(asyncRestTemplate, baseURL);
```

All requests return a `ListenableFuture`. You can therefore block to get the response or provide a callback:

```java
// Synchronous
Entity entity = ngsiClient.getEntity("DC_S1-D41", "Room", Arrays.asList("temperature", "humidity")).get();

// Asynchronous
ngsiClient.getEntity("DC_S1-D41", "Room", Arrays.asList("temperature", "humidity")).addCallback(entity -> {
        /* handle entity */
    }, ex -> {
        /* handle error */
    });
```

Request returning a list of elements (entities, types, etc...) use a `Paginated` class that wraps the list of elements and return additional pagination information like `offet`, `limit` and `total` count of elements:

```java
// List the first twenty types and count total types
Paginated<EntityType> result = ngsiClient.getEntityTypes(0, 20, true).get();

List<EntityType> types = result.getItems();
int total = result.getTotal();
```

### Server

The library proposes an abstract class `Ngsi2BaseController` based on the Spring MVC framework to let you implement a NGSIv2 server easily.

Add the server library to your `pom.xml`:

```xml
<dependency>
    <groupId>com.orange.fiware</groupId>
    <artifactId>ngsi2-server</artifactId>
    <version>X.Y.Z</version>
</dependency>
```

Implement a new class based on the `Ngsi2BaseController` in your project and override methods you want to support :

```java
@RestController
@RequestMapping("/v2/")
public class Ngsi2Controller extends Ngsi2BaseController {
    @Override
    protected void createEntity(Entity entity){
      /* implementation */
    }
}
```

## License

This project is under the Apache License version 2.0

