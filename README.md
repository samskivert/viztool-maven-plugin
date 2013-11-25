## Viztool Maven Plugin

This Maven plugin allows you to easily generate [Viztool] visualizations of the classes in a
Maven-based project. Just add it to your POM like so:

```xml
  <profiles>
    <profile>
      <id>viztool</id>
      <build>
        <plugins>
          <plugin>
            <groupId>com.samskivert</groupId>
            <artifactId>viztool-maven-plugin</artifactId>
            <version>1.0</version>
            <configuration>
              <pkgroot>PKGROOT</pkgroot>
            </configuration>
            <executions>
              <execution>
                <id>genviz</id>
                <phase>process-classes</phase>
                <goals>
                  <goal>genviz</goal>
                </goals>
              </execution>
            </executions>
          </plugin>
        </plugins>
      </build>
    </profile>
  </profiles>
```

Then generate a visualization thusly:

    mvn -Pviztool package

This will pop up a print output dialog at some point during the build. You can then print to a PDF
file and view and print your visualization from there.

[Viztool]: https://github.com/samskivert/viztool
