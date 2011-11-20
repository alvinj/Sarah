
--
-- An AppleScript function that reads a file and returns the lines
-- from that file as a list.
--
on returnFileContentsAsList(theFile)
	set fileHandle to open for access theFile
	set theLines to paragraphs of (read fileHandle)
	close access fileHandle
	return theLines
end


