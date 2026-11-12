# Release 5: pom.xml addition (you apply this)

The supplied `pom.xml` already contains this dependency (under the `M5` comment); nothing to paste.

```xml
    <dependency>
      <groupId>org.postgresql</groupId>
      <artifactId>postgresql</artifactId>
      <version>42.7.14</version>
    </dependency>
```

Optional, to start the service with Maven (`mvn -q compile exec:java`), add this plugin inside
`<build><plugins>`:

```xml
      <plugin>
        <groupId>org.codehaus.mojo</groupId>
        <artifactId>exec-maven-plugin</artifactId>
        <version>3.6.4</version>
        <configuration>
          <mainClass>payments.App</mainClass>
        </configuration>
      </plugin>
```

Then apply `release/m5/saveSettlement-interface-addition.md` and run `mvn -q test -Pm5` with a
local PostgreSQL reachable through PG* variables. If the driver line is ever removed, the database
tests fail with "the PostgreSQL JDBC driver is missing: apply release/m5/pom-additions.md".
