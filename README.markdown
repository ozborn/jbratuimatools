Collection of java UIMA tools to handle analysis of BRAT annotations. This code is very early in development and works for only 1 particular annotation format at this point in time. It should probably be folded into the OpenNLP Apache Project (also on github) as this now has a BRAT annotation parser. However it can not handle the CUI annotation task I am using it for here.
 
It includes functionality to:
* Check for missing CUIs
* Check for disrepancies
* Output brat annotations overlayed on SemEval data
* Generate SemEval based annotations for BRAT annotation


INSTALL
Unfortunately this currently requires oracle ojdbc7.jar to function to get the ideal concept name from UMLS, so ojdbc7.jar must be downloaded and can then be installed as folllows using maven:

```
mvn install:install-file -Dfile=/home/ozborn/Downloads/ojdbc7.jar -DgroupId=com.oracle -DartifactId=ojdbc7 -Dversion=12.1.0.1 -Dpackaging=jar
```

Then install:
```
mvn install -DskipTests
```

The failsafe integrations must be skipped if there is no umls jdbc connection string provided in src/main/umlsDB.properties in this format:
```
umlsJdbcString=jdbc:oracle:thin:umls_user/umls_password@host:1521:sid
```


ECLIPSE INSTALL AND BUILD
-Add target/generated-sources/jcasgen to the source directory if doing a java build
-did not need to do last build

TODO
-Standardize on logging, maybe include Logger.properties for Uima Logging
-Cleanup/Take action on log warnings for things like: "Failed to find needed related ontology concept in object :extra-axial at 1867"
