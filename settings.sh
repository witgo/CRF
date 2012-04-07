# settings for CRF..

# Check if CRF_HOME is set 
if [ ! ${CRF_HOME} ]; then
  echo "CRF_HOME is not set. Using default as current working directory."
  export CRF_HOME=.
fi

echo "Using CRF_HOME=${CRF_HOME}"

# Setting the classpath variable

for file in `ls $CRF_HOME/lib/*.jar`
do
  export CLASSPATH=$file:$CLASSPATH
done
