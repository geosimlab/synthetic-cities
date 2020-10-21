# synthetic-cities (syncity)

The Geo SimLab repository. For more info contact: 
 - Itzhak Benenson <bennya@tauex.tau.ac.il>
 - Golan Ben-Dor <golanben@mail.tau.ac.il>
 
## Installation
1. Clone the repository
2. Import to eclipse as a maven project
3. Install GLPK from [here][1]
3. Install `amodeus` [from our fork][2] and checkout the branch 'simlab/main-1.9.1'
5. You can now use one of the runners in `java/syncity`
 
#### FAQ
- Eclipse might recognize the `src` folder as your root package: 
  - <kbd>Right-click</kbd> the project 
  - Choose <kbd>Build Path</kbd>-><kbd>Configure Build Path</kbd>
  - In the <kbd>Source</kbd> tab remove the current `src` folder
  - Instead add `src/main/java` and `src/main/resources` 
- For GLPK UnsatisfiedLinkError see [this fix][3]
 
 
[1]: http://glpk-java.sourceforge.net/gettingStarted.html
[2]: https://github.com/geosimlab/amodeus
[3]: https://stackoverflow.com/questions/29923476/glpk-java-java-lang-unsatisfiedlinkerror-cant-find-dependent-libraries/30999011#30999011
