# Annohub

0. Prerequisites

   * Linux/Unix distribution</br>
   * Java runtime >= 1.8</br>
   * 7z (7za) file archiver utitily</br>
   * rapper rdf utility (http://librdf.org/raptor/)</br>
   * TomEE >= 7.1.0  (https://tomee.apache.org/)</br>

1. Download Tinkerpop Gremlin Server version 3.3.10

   https://tinkerpop.apache.org/downloads.html


2. Unpack the file and install the neo4j-gremlin driver

   cd apache-tinkerpop-gremlin-server-3.3.10 

   bin/gremlin-server.sh install org.apache.tinkerpop neo4j-gremlin 3.3.10


  The process of plugin installation is handled by Grape, which helps resolve dependencies into the classpath. If you run   
  into errors you can obtain further information on the installation of Grape at https://tinkerpop.apache.org/docs/current/reference/#neo4j-gremlin


3. Edit the Gremlin Server configuration file conf/neo4j-empty.properties to set the server's database directory

   gremlin.neo4j.directory=/your/server/directory


4. Start the server

   bin/gremlin-server.sh


5. Edit the Annohub configuration file (you can use /src/main/resources/FIDConfig.xml as a template)

   Database setup</br>
   a. Gremlin.Server.conf - /your/path/to/apache-tinkerpop-gremlin-server-3.3.10/conf/gremlin-server-neo4j.yaml

   b. Gremlin.Server.home - /your/path/to/apache-tinkerpop-gremlin-server-3.3.10

   c. Gremlin.Server.data - /another database directory (this is different from the directory entered in step 3 !)

   Application setup</br>
   a. RunParameter.downloadFolder - crawler-download-directory (e.g. /tmp/annohub/downloads)

   b. RunParameter.ServiceUploadDirectory - web-application-upload-directory (e.g. /tmp/annohub/uploads)

   c. RunParameter.decompressionUtility - enter one of which 7z or (7za) 


6. For easy maintance of your configuration you can set the environment variable FID_CONFIG_FILE to the location of you configuration file 


7. Build the Annohub application with maven

   mvn install clean


8. Initialize the Annohub model database

   run.sh -init

9. After initalization has finished you can parse data 

   run.sh -execute -seed seed_file 

   where seed_file contains a list of language resource URLs (one URL per line)


10. For the deployment of the Annohub web-application an instance of TomEE (https://tomee.apache.org/) is required. 

 
