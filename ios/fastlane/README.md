fastlane documentation
----

# Installation

Make sure you have the latest version of the Xcode command line tools installed:

```sh
xcode-select --install
```

For _fastlane_ installation instructions, see [Installing _fastlane_](https://docs.fastlane.tools/#installing-fastlane)

# Available Actions

### build_production

```sh
[bundle exec] fastlane build_production
```

Build Production IPA

### deploy_production

```sh
[bundle exec] fastlane deploy_production
```

Deploy Production to App Store

----


## iOS

### ios deploy_dev

```sh
[bundle exec] fastlane ios deploy_dev
```

Deploy Development to Firebase App Distribution

### ios verify_setup

```sh
[bundle exec] fastlane ios verify_setup
```



### ios build_staging

```sh
[bundle exec] fastlane ios build_staging
```

Build production TestFlight IPA con debug

### ios deploy_staging

```sh
[bundle exec] fastlane ios deploy_staging
```

Deploy Staging a TestFlight

----

This README.md is auto-generated and will be re-generated every time [_fastlane_](https://fastlane.tools) is run.

More information about _fastlane_ can be found on [fastlane.tools](https://fastlane.tools).

The documentation of _fastlane_ can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
