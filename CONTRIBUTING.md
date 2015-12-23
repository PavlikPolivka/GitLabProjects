#How to setup you environment for developing this plugin
- Open IntelliJ
- Create IntelliJ Plugin SDK [Guide](https://www.jetbrains.com/idea/help/configuring-intellij-platform-plugin-sdk.html)
- Checkout this plugin
- Add new Run Configuration
    - Click plus button and select Plugin
    - Select your created SDK
- Click on debug button

#Links
[Official JetBrains documentation](https://www.jetbrains.com/idea/help/plugin-development-guidelines.html)

#Notes
Idea specific files are part of the repository because there is TeamCity server that is building this plugin.
For this to work these files have to be there. Feel free to do any changes on them locally, just do not commit it as part of your pull request.