# get files and install into local repositories

# spi-full-0.2.4
wget http://sourceforge.net/p/jsbml/code/HEAD/tree/trunk/core/lib/spi-full-0.2.4.jar?format=raw -O spi-full-0.2.4.jar
mvn install:install-file -DgroupId=cysbml-temp -DartifactId=spi-full -Dversion=0.2.4 -Dfile=spi-full-0.2.4.jar -Dpackaging=jar -DgeneratePom=true

# jigsaw-dateParser-0.1
wget http://sourceforge.net/p/jsbml/code/HEAD/tree/trunk/core/lib/jigsaw-dateParser.jar?format=raw -O jigsaw-dateParser-0.1.jar
mvn install:install-file -DgroupId=cysbml-temp -DartifactId=jigsaw-dateParser -Dversion=0.1 -Dfile=jigsaw-dateParser-0.1.jar -Dpackaging=jar -DgeneratePom=true

# checkout jsbml svn
# cd core
# ant jar # build the core jar
# <dependency>
# 	<groupId>org.sbml.jsbml</groupId>
#  	<artifactId>core</artifactId>
#  	<version>1.0</version>
#  </dependency>

cp 
mvn install:install-file -DgroupId=cysbml-temp -DartifactId=core -Dversion=1.0 -Dfile=core.jar -Dpackaging=jar -DgeneratePom=true
