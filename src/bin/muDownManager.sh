PRG=$0
PROGDIR=$(dirname $PRG)

java -cp $PROGDIR/lib/*: mudownmanager.MuDownManagerApp
