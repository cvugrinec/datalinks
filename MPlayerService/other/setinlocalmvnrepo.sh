# chris v
# Small bash for putting the needed jars in your local mvn repo..

rm message 

for file in $(ls azure*.jar)
do
  artifact=$(echo $file | sed  's/-[0-9].*//' | sed 's/.jar//')
  # version nr
  version=$(echo $file | sed  's/^azure.*[-]//' | sed 's/.jar//')
  echo "setting artifact: $artifact with version $version in mvn repo"
  #mvn install:install-file -Dfile=$file -DgroupId=com.microsoft.windowsazure  -DartifactId=$artifact -Dversion=$version -Dpackaging=jar
  echo "compile group: 'com.microsoft.windowsazure', name: '$artifact', version: '$version'" >>message
done

echo "now put this stuff in your build.gradle file"
cat message
rm message
