# Release 4: pom.xml additions (you apply these)

The supplied `pom.xml` already contains these two dependencies (under the `M4` comment), so there
is nothing to paste: run `mvn -q test -Pm4`. This note records what they are for.

```xml
    <dependency>
      <groupId>io.javalin</groupId>
      <artifactId>javalin-bundle</artifactId>
      <version>7.2.3</version>
    </dependency>
    <dependency>
      <groupId>org.mockito</groupId>
      <artifactId>mockito-core</artifactId>
      <version>5.24.0</version>
      <scope>test</scope>
    </dependency>
```

- The Javalin bundle brings Javalin 7, a JSON mapper and SLF4J logging, used by `PaymentApi`.
- Mockito is optional for `PaymentDeskTest`: a small fake `PaymentRepository` of your own is
  equally accepted.
- If you removed them, the M4 profile stops at compile time with
  `package io.javalin does not exist`; restore them from this note.
- The supplied workspace already provides these dependencies, so there is nothing to download or
  install. If Maven reports that a dependency cannot be resolved, ask your trainer to repair the
  workspace.
