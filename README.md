# synthetic-cities (syncity)

The Geo SimLab repository. For more info contact: 
 - Itzhak Benenson <bennya@tauex.tau.ac.il>
 - Golan Ben-Dor <golanben@mail.tau.ac.il>
 
## Installation
1. Clone the repository
2. Import to eclipse as a maven project
3. Install GLPK from [here][3]
3. Install `amod` 1.9.1 (check the FAQ)
4. Import your amod as well
5. Set the parameters in the [SimulationRunner](src/main/java/syncity/SimulationRunner.java) class and run it :)
 
#### FAQ
- `amod` doen't have a maven repo. Download from this [commit link][1]
- Eclipse might recognize the `src` folder as your root package: 
  - <kbd>Right-click</kbd> the project 
  - Choose <kbd>Build Path</kbd>-><kbd>Configure Build Path</kbd>
  - In the <kbd>Source</kbd> tab remove the current `src` folder
  - Instead add `src/main/java` and `src/main/resources` 
- For GLPK UnsatisfiedLinkError see [this fix][2]
 
 
[1]: https://github.com/amodeus-science/amod/tree/f80ba30884ac3c50af4cca9eef155a963f273ada
[2]: https://stackoverflow.com/questions/29923476/glpk-java-java-lang-unsatisfiedlinkerror-cant-find-dependent-libraries/30999011#30999011
[3]: http://glpk-java.sourceforge.net/gettingStarted.html
