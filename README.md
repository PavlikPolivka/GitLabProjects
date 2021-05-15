[![No Maintenance Intended](http://unmaintained.tech/badge.svg)](http://unmaintained.tech/)
# DEPRECATED

I no longer have time to maintain this. JetBrains keep changing how their vcs integration works and I no longer use Gitlab. If someone wants to continue with this email me at pavel.polivka@hey.com we can figure out some kind of hand off.

## GitLab Projects
Simple plugin that is adding support for GitLab specific actions to JetBrain IDEs

### Features:
* **GitLab Checkout support** - add GitLab autocompleter to IDE Git checkout dialog
* **GitLab Share dialog** - allows quick import of new projects to GitLab, user can specify namespace and project visibility
* **GitLab Merge Request dialog** - user can quickly create new merge requests from current branch
* **GitLab Merge Request List dialog** - user can list and accept all open code reviews

### Development
This project is using the intellij gradle build plugin. 
https://github.com/JetBrains/gradle-intellij-plugin

To build do

`gradle build`

See the built plugin archive in: `build/distributions`

To run it in IDE and debug

`gradle runIdea`


### Contributing
Everybody is welcome to contribute to this project.

Submit your pull requests to **master** branch.

Master is always exact code that is used in latest release.
