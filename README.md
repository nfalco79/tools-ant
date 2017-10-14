tools
=====

[![Build Status](https://travis-ci.org/nfalco79/tools-ant.svg?branch=master)](https://travis-ci.org/nfalco79/tools-ant) [![Coverage Status](https://coveralls.io/repos/github/nfalco79/tools-ant/badge.svg?branch=master)](https://coveralls.io/github/nfalco79/tools-ant?branch=master)

## Maven

Released versions are available in The Central Repository.
Just add this artifact to your project:

```xml
<dependency>
    <groupId>com.github.nfalco79</groupId>
    <artifactId>tools-ant</artifactId>
    <version>{version}</version>
</dependency>
```

However if you want to use the last snapshot version, you have to add the Nexus OSS repository:

```xml
<repository>
    <id>osshr</id>
    <name>Nexus OSS repository for snapshots</name>
    <url>https://oss.sonatype.org/content/repositories/snapshots</url>
    <snapshots>
        <enabled>true</enabled>
    </snapshots>
</repository>
```


## License

This project is licensed under [APLv2 license](http://www.spdx.org/licenses/Apache-2.0).