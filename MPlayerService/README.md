# Azure Media Services 


This demo demonstrates the following:
 * How to upload a piece of media to the Azure Media Services Asset repository
 * How to encode a piece of media
 * How to make this newly encoded media available via a public url
 * Usage of the Azure Media Service API (AmsMediaHandler), demonstration of the following methods:
   * checkJobStatus
   * encode, in this code we create a readonly AccessPolicy for only 90 days
   * getStreamingOriginLocator (show the public endpoint on which the media will be available)
   * uploadFileAndCreateAsset 
   * uploadMediaToAMS
 * Playing the Media with the Azure Media Player

How to set this up
  * If you are not interested in the code...skip the setting up required libs part...just do a java -jar build/libs/bplayer-ams-0.1.0.jar
  * setting up required libs
    * cd other
    * execute the following script: setinlocalmvnrepo.sh ....this will put all your required dependencies in your local maven repo
    * in your build.gradle only put change the localrepostory URL to point to your local MVN repo, the depencies are already set for you
  * build the project with gradle: gradle clean build
  * java -jar build/libs/bplayer-ams-0.1.0.jar
  * this will spin up a springboot app, which you can access by entering: http://localhost:8080
  * You can upload the media to Azure the repository if you have a key....I will share this key with my current project members 

* See the demo:
  * http://datalinks.nl/azure-demo.html?url=http://tstmediaservice1.streaming.mediaservices.windows.net/b50ec8b9-4c30-4434-ae22-83bf60249baf/ams-demo.ism/manifest

