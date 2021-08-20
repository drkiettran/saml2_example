# SAML2 Coding Example.
This projects show how you may be able to create a Spring Boot application that uses SAML V2 for SSO with an Identity Provider.
This work is based on this repository: `https://github.com/oktadev/okta-spring-boot-saml-example` and this web page: `https://www.chakray.com/wso2-identity-server-integration-spring-boot-security-saml/`

## Step `1` Creating a basic Spring Boot application

I created a Spring Boot application that has these capabilities from `https://start.spring.io/`:

	- DEV tools
	- Spring Web
	- Thymeleaf
	- Spring Security

I chose `Maven`, `JDK 8`, packaging as `JAR`. At the time this code was developed, I used Spring Boot version `2.5.3`.

## Step `2` Developing Code

I imported the project into my `Eclipse IDE`. 

I updated the `pom.xml` file to include the following libraries:

```xml
<!-- SAML2 Dependencies -->
		<dependency>
			<groupId>org.springframework.security.extensions</groupId>
			<artifactId>spring-security-saml2-core</artifactId>
			<version>1.0.3.RELEASE</version>
		</dependency>
		<dependency>
			<groupId>org.springframework.security.extensions</groupId>
			<artifactId>spring-security-saml-dsl-core</artifactId>
			<version>1.0.5.RELEASE</version>
		</dependency>
		<!-- SAML2 Dependencies -->
```

I added a Java class `IndexController.java`. This class just serve an `index` url.

```java
package com.drkiettran.saml2_example;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class IndexController {

    @RequestMapping("/")
    public String index() {
        return "index";
    }
}
```

I then added this Java class, `SecurityConfiguration.java`:

```java
package com.drkiettran.saml2_example;

import static org.springframework.security.extensions.saml2.config.SAMLConfigurer.saml;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@EnableWebSecurity
@Configuration
@EnableGlobalMethodSecurity(securedEnabled = true)
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {
	@Value("${security.saml2.metadata-url}")
	String metadataUrl;

	@Value("${server.ssl.key-alias}")
	String keyAlias;

	@Value("${server.ssl.key-store-password}")
	String password;

	@Value("${server.port}")
	String port;

	@Value("${server.ssl.key-store}")
	String keyStoreFilePath;

	@Override
	protected void configure(final HttpSecurity http) throws Exception {
		http
        .authorizeRequests()
            .antMatchers("/saml/**").permitAll()
            .anyRequest().authenticated()
            .and()
        .apply(saml())
            .serviceProvider()
                .keyStore()
                    .storeFilePath(this.keyStoreFilePath)
                    .password(this.password)
                    .keyname(this.keyAlias)
                    .keyPassword(this.password)
                    .and()
                .protocol("https")
                .hostname(String.format("%s:%s", "localhost", this.port))
                .basePath("/")
                .and()
            .identityProvider()
            .metadataFilePath(this.metadataUrl);
	}
}
```

I updated the application properties, `application.properties` as follows:

```properties
server.port = 8443
server.ssl.enabled = true
server.ssl.key-alias = saml2cert
server.ssl.key-store = classpath:saml/keystore.jks
server.ssl.key-store-password = changeit

security.saml2.metadata-url = file:///home/student/Downloads/metadata.xml
```

I created an index file under `resources/template/index.html`:

```html
<!DOCTYPE html>
<html>
  <head>
  <title>Spring Security SAML Example</title>
  </head>
  <body>Hello SAML From Kiet Tran!
  </body>
</html>
```

I then have the following files under `resources/saml` directory:

```
keystore.jks
saml2.cer
saml.pem
```

I created `keystore.jks` as follows:

```shell
keytool -genkey -keyalg RSA -alias saml2cert -keystore keystore.jks
```

I created `saml2.cer` as follows:

```shell
keytool -export -keystore keystore.jks -alias saml2cert -file saml2.cer
```

I then created `saml2.pem` as follows:

```shell
openssl x509 -inform der -in saml2.cer -out saml2.pem
```

I would later use the `saml2.pem` file to create an application with `WSO2`.

## Step `3` Deploy WSO2

I deploy WSO2 using Docker Image at Docker Hub website `https://hub.docker.com/r/wso2/wso2is`. This is the script
I used to run the container:

```shell
#!/bin/bash

export EXT_PORT_NO=9443
export INT_PORT_NO=9443
export IMAGE_NAME=wso2/wso2is:5.7.0
export CONTAINER_NAME=wso2-is

# Pull latest container
docker pull $IMAGE_NAME
 
# Setup local configuration folder
# docker volume create --name mysql_data
# uid/psw = admin/admin

# Start container
docker run --restart=always -d -p $EXT_PORT_NO:$INT_PORT_NO \
-e MYSQL_ROOT_PASSWORD=password \
-v /tmp:/tmp \
--name $CONTAINER_NAME -t $IMAGE_NAME
 
docker logs --follow $CONTAINER_NAME

```
