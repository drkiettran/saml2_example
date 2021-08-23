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

I added a Java class `IndexController.java`. This class just serve an `index` URL.

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

## Step `4` Configuring an application with WSO2

- I logged in to wso2 using `uid/psw` as `admin/admin`. 
- I created a new service provider from the left panel as in `Service Provider / Add`.
- I entered `Service Provder Name` as Spring SAML2
- I clicked on `Register` button.
- I then entered the `Application Sertificate` by browsing to my `resources/saml/saml2.pem` as I created in `step 2` above. It actually copied the content of the `pem` file into the text box.
- I then clicked on `Inbound Authentication Configuration` tab.
- I chose SAML2 Web SSO Configuration tab
- I then clicked on `Configure` link.
- I entered `Issuer` as `https://localhost:8443/saml/metadata`.
- I added three more `Assertion Consumer URLs` as: `https://localhost:8443/saml/metadata`, `https://localhost:8443/saml/SSO`, `https://localhost:8443`
- I made sure to check these, `Enable Response Signing`, `Enable Signature Validation in Authentication Requests and Logout Requests`, `Enable Single Logout`, `Enable Attribute Profile/Include Attributes in the Response Always` and `Enable IdP Initiated SSO`.
- I clicked on `Update` button

## Step `5` Create a Login Role

- I clicked on `Add` from `Users and Roles` tab on the left pane. 
- I clicked on `Add New Role`.
- I selected `PRIMARY` and entered new `Role` name as `Login`
- I clicked on `Next`, and select `Login` role.
- I select `All Permissions/Admin Permissions/Login`.
- I clicked on `Finish`.

## Step `6` Adding User and Assign Roles

- I clicked on `Add` from `Users and Roles` tab on the left pane. 
- I clicked on `Add New User`.
- I selected `PRIMARY` `Domain`, entered `Username` and `Password`.
- I clicked on `Next`, and select `Login` role.
- I clicked on `Finish`.

## Step `7` Exporting SAML2 metadata file

- I clicked `Resident` of `Identity Provider` on the left panel.
- I expanded `Inbound Authentication Configuration/SAMLE2 Web SSO Configuration`.
- I clicked on `Download SAML Metadata`.
- I save the file under `src/main/resources/saml/` directory as `metadata.xml`.


