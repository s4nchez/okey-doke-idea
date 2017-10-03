[![Build Status](https://travis-ci.org/s4nchez/okey-doke-idea.svg?branch=master)](https://travis-ci.org/s4nchez/okey-doke-idea)


# okey-doke-idea
Plugin with support of the [Okey-doke Approval Test library](https://github.com/dmcg/okey-doke) for IntelliJ based IDEs.

Details available in the [JetBrains IntelliJ Plugins page](https://plugins.jetbrains.com/idea/plugin/9424-okey-doke-support).

## Installation

Search for "Okey-doke Support" in `IDE Settings -> Plugins -> Browse repositories...`.

## Developing

* Download the [IntelliJ Kotlin Plugin](https://plugins.jetbrains.com/idea/plugin/6954-kotlin)
  * _Make sure the version matches the one your IDE is currently using, or be prepared to get unexpected behaviours in runtime._
* Clone this repo
* Import project into IntelliJ from gradle config.
* Use `buildPlugin` gradle task to build plugin `.zip` file.
* Use `runIde` gradle task to run/debug the plugin.
