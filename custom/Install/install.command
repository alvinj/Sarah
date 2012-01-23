
SARAH_ROOT_DIR=~/.sarah
DATA_DIR=~/${SARAH_ROOT_DIR}/data
LOGS_DIR=~/${SARAH_ROOT_DIR}/logs
PLUGINS_DIR=~/${SARAH_ROOT_DIR}/plugins

echo "Making DATA directory ($DATA_DIR) ..."
mkdir -p $DATA_DIR

echo "Making LOGS directory ($LOGS_DIR)..."
mkdir -p $LOGS_DIR

echo "Making PLUGINS directory ($PLUGINS_DIR)..."
mkdir -p $PLUGINS_DIR

# the directory where the script is run
INSTALL_DIR=`dirname "$0"`

#
# TODO - copy the initial files here
#
echo "INSTALL DIR = $INSTALL_DIR"

#cp -r ${INSTALL_DIR}/install-data/* ~/${SARAH_ROOT_DIR}/data


