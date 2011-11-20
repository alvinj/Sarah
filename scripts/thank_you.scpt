# --------------------------------------------------------
# crazy work to build the path to our Common.scpt file
# change the first line below as needed
# --------------------------------------------------------
set nameOfThisFile to "thank_you.scpt"
set nameOfDataFile to "thank_you.data"

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

set CommonLib to run script ("script s" & return & (read alias commonLibFile as Çclass utf8È) & return & "end script " & return & "return s")

set thank_yous to CommonLib's returnFileContentsAsList(dataFile)
set numThanks to count of thank_yous
set rn to (random number from 1 to numThanks)
set reply to item rn of thank_yous
say reply
