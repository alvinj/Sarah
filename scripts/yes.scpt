# --------------------------------------------------------
# crazy work to build the path to our Common.scpt file
# (boilerplate code here, but change data file names)
# change the first line below as needed
# --------------------------------------------------------
set nameOfThisFile to "yes.scpt"
set nameOfDataFile to "yes.data"

set fullPathOfApp to path to me as Unicode text
set AppleScript's text item delimiters to nameOfThisFile
set currDir to text item 1 of fullPathOfApp
set commonLibFile to currDir & "Common.scpt"

# need the posix path to our data file
set fullPosixPathOfApp to POSIX path of (path to me)
set AppleScript's text item delimiters to nameOfThisFile
set currPosixDir to text item 1 of fullPosixPathOfApp
set dataFile to currPosixDir & nameOfDataFile
# --------------------------------------------------------

set commonLibFile to "New SSD:Users:al:Projects:Scala:Sarah:scripts:Common.scpt"
set CommonLib to run script ("script s" & return & (read alias commonLibFile as Çclass utf8È) & return & "end script " & return & "return s")

set yesses to CommonLib's returnFileContentsAsList("/Users/al/Projects/Scala/Sarah/scripts/yes.data")
set numYesses to count of yesses
set rn to (random number from 1 to numYesses)
set reply to item rn of yesses
say reply
