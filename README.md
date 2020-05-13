# synthetic-cities (syncity)

The Geo SimLab repository. For more info contact: 
 - Itzhak Benenson <bennya@tauex.tau.ac.il>
 - Golan Ben-Dor <golanben@mail.tau.ac.il>
 
## Installation
1. Clone the repository
2. Import to eclipse as a maven project
3. Download `amod` 1.9.1*
4. Import your amod as well
5. 
 
### FAQ
- Eclipse might recognize the `src` folder as your root package: 
  - `Right-click` the project 
  - Choose `Build Path`->`Configure Build Path`
  - In the `Source` tab remove the current `src` folder
  - Instead add `src/main/java` and `src/main/resources` 
- `amod` doen't have a maven repo. download from this [commit link][1]
 
 
[1]: https://github.com/amodeus-science/amod/blob/f80ba30884ac3c50af4cca9eef155a963f273ada