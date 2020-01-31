* Release Highlights for release ${project.version} ([Full Log](https://github.com/jflamy/owlcms4/issues?utf8=%E2%9C%93&q=is%3Aclosed+is%3Aissue+project%3Ajflamy%2Fowlcms4%2F1+))
  - [x] Internal change: the way the releases are built has been redone to use the [Jenkins](https://jenkins.io/) tool.
    - [ ] From now on, the stable official owlcms4 releases will be at https://github.com/owlcms/owlcms4 and the public results relay will be available from https://github.com/owlcms/owlcms-publicresults
    - [ ] The pre-releases (for early adopters) will be at https://github.com/jflamy-dev/owlcms4-prerelease and the matching public results relay at https://github.com/jflamy-dev/owlcms-publicresults-prerelease
  - [x] Enhancement: Remote public results scoreboard.  The competition site sends updates to a separate scoreboard application that provides the scoreboard/leaderboard to the public. (#139, #292)
  - [x] Fix: Allowed time not updated correctly after decision when using the phone refereeing interface (#291)
  - [x] Fix: after several group changes, there could be a spinning wait indicator on group selection menu , and a need to click twice to start the intro countdown or go to lifting (#294)
  - [x] Enhancement: Portuguese translations

Installation Instructions :
  - For **Windows**, download `owlcms_setup.exe` from the Assets section below and follow [Windows installation instructions](https://jflamy.github.io/owlcms4/#/LocalWindowsSetup.md) 
    
    > If you get a blue window with `Windows protected your PC`, or if your laptop works very hard performing an anti-virus scan that takes very long, see this page : [Make Windows Defender Allow Installation](https://jflamy.github.io/owlcms4/#/DefenderOff)
  - For **Linux** and **Mac OS**, download the `owlcms.zip` file from the Assets section below and follow [Local Linux and Mac OS instructions](https://jflamy.github.io/owlcms4/#/LocalLinuxMacSetup.md) 
  - For **Heroku** cloud, download the `owlcms.zip` file from the Assets section below and follow [Cloud installation instructions](https://jflamy.github.io/owlcms4/#/Heroku.md)