# Support for Cordova Hybrid Apps in Codename One

This project enables developers to create [Cordova](https://cordova.apache.org/) (HTML5/Javascript) hybrid native apps with [Codename One](http://www.codenameone.com).

##License

[Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)

##Prerequisites

* JDK 8 or Higher
* ANT
* Netbeans with Codename One Plugin Installed

## Installing the Tools

1. Download latest [cn1-cordova-tools.zip](https://github.com/codenameone/CN1Cordova/raw/master/cn1-cordova-tools.zip), and extract locally.


## Using the Tools

### Creating a New Codename One Cordova Project

From the terminal or command prompt:

~~~
$ cd cn1-cordova-tools
$ ant create -Did=com.example.hello -Dname=HelloWorld
~~~

This will create a new Codename One netbeans project at cn1-cordova-tools/HelloWorld with the "packageName" set to `com.example.hello` and the app's name "HelloWorld".

Open this project up in Netbeans to start working on it.  You'll find the app's www files (e.g. index.html etc...) inside the src/html directory of the project.

This project will include two subdirectories worth noting, that aren't part of a normal Codename One project:

1. "plugins" - Contains Codename One library projects for plugins associated with this app.
2. "cordova-tools" - Contains an ANT build script with targets to help manage the cordova-related aspects of the project.  E.g. install plugins, and refresh the project with new plugin cn1libs are added. [Read the CLI Usage Instructions](https://github.com/codenameone/CN1Cordova/wiki/Project-cordova-tools-CLI-Usage)

#### Specifying Output Directory

By default the project is generated inside the cn1-cordova-tools directory.  You can change this to a different directory using the `-Ddest=</path/to/dest` command-line flag.  E.g.

~~~~
$ ant create -Did=com.example.hello -Dname=HelloWorld -Ddest=/Users/shannah/NetbeansProjects
~~~~



### Migrating an Existing Cordova Project to a Codename One Cordova Project

From the terminal or command prompt:

~~~
$ cd cn1-cordova-tools
$ ant create -Dsource=</path/to/cordova/app>
~~~

This will create Netbeans Project inside the cn1-cordova-tools directory with settings (package id and name) matching the app specified in the `-Dsource` argument. The contents of the app's `www` directory will be copied to the project's `src/html` directory.

This project will include two subdirectories worth noting, that aren't part of a normal Codename One project:

1. "plugins" - Contains Codename One library projects for plugins associated with this app.
2. "cordova-tools" - Contains an ANT build script with targets to help manage the cordova-related aspects of the project.  E.g. install plugins, and refresh the project with new plugin cn1libs are added. [Read the CLI Usage Instructions](https://github.com/codenameone/CN1Cordova/wiki/Project-cordova-tools-CLI-Usage)

NOTE:  You can also specify the `-Ddest` parameter to specify an alternate output directory for your project.

### CLI Usage Instructions

[Read the full cn1-cordova-tools ANT task CLI Usage instructions](https://github.com/codenameone/CN1Cordova/wiki/cn1-cordova-tools-CLI-usage)

## Developing Plugins

See the [Plugin Development Wiki Page](https://github.com/codenameone/CN1Cordova/wiki/Plugin-Development)

## JavaDoc

* [Cordova CN1Lib](https://codenameone.github.io/CN1Cordova/javadoc/cordova)
* [Ant Tasks](https://codenameone.github.io/CN1Cordova/javadoc/CordovaAppBuilder)
* [Camera Plugin](https://codenameone.github.io/CN1Cordova/javadoc/cordova-plugin-camera)