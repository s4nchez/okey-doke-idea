# okey-doke-idea
IntelliJ IDEA Plugin for okey-doke

Details available in the [JetBrains IntelliJ Plugins page](https://plugins.jetbrains.com/idea/plugin/9424-okey-doke-support).

## Installation

Search for "Okey-doke Support" in IntelliJ IDEA's Plugins settings (under "_Browse repositories..._").

## Developing

* Download the [IntelliJ Kotlin Plugin](https://plugins.jetbrains.com/idea/plugin/6954-kotlin)
  * _Make sure the version matches the one your IDE is currently using, or be prepared to get unexpected behaviours in runtime._
* Clone this repo
* Open it in IntelliJ IDEA
* Configure the Module SDK to use the version of the *IntelliJ IDEA* SDK available on your IDE
* Add the *Kotlin/lib* directory from the downloaded Kotlin Plugin as a dependency, with *Provided* scope
* Add a new Run/Debug Configuration of type *Plugin*

At this point you should be able to make changes and run/debug it.
