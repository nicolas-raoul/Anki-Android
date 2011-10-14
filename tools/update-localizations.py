#!/usr/bin/python
# -*- coding: utf-8 -*-

# Copyright (c) 2010 norbert.nagold@gmail.com
#
# This program is free software; you can redistribute it and/or modify it under
# the terms of the GNU General Public License as published by the Free Software
# Foundation; either version 3 of the License, or (at your option) any later
# version.
#
# This program is distributed in the hope that it will be useful, but WITHOUT ANY
# WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
# PARTICULAR PURPOSE. See the GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License along with
# this program.  If not, see <http://www.gnu.org/licenses/>.
#
# This script extract localization from ankidroid.zip into the right folders.
# http://crowdin.net/download/project/ankidroid.zip

# Below is the list of official AnkiDroid localizations.
# Add a language if 01-core.xml is translated
# Do not remove languages.
# When you add a language, please also add it to mAppLanguages in Preferences.java

languages = ['ar', 'ca', 'cs', 'de', 'el', 'es-ES', 'fi', 'fr', 'hu', 'id', 'it', 'ja', 'ko', 'nl', 'pl', 'pt-PT', 'ro', 'ru', 'sr', 'sv-SE', 'tr', 'vi', 'zh-CN', 'zh-TW'];
#languages = ['ar', 'ca', 'cs', 'de', 'el', 'es-ES', 'fi', 'fr', 'hu', 'it', 'ja', 'ko', 'nl', 'pl', 'pt-PT', 'ro', 'ru', 'sr', 'sv-SE', 'vi', 'zh-CN', 'zh-TW', 'th', 'sk', 'da', 'ko', 'he', 'uk'];

fileNames = ['01-core', '02-strings', '03-dialogs', '04-network', '05-feedback', '06-statistics', '07-cardbrowser', '08-widget', '09-backup', '10-preferences', '11-arrays', 'tutorial']


import os
import zipfile
import urllib
import string
import re

def replacechars(filename, fileExt, isCrowdin):
	s = open(filename,"r+")
	newfilename = filename + ".tmp"
	fin = open(newfilename,"w")
	errorOccured = False
	if fileExt != '.csv':
		for line in s.readlines():
			if line.startswith("<?xml"):
				line = "<?xml version=\"1.0\" encoding=\"utf-8\"?> \n <!-- \n ~ Copyright (c) 2009 Andrew <andrewdubya@gmail> \n ~ Copyright (c) 2009 Edu Zamora <edu.zasu@gmail.com> \n ~ Copyright (c) 2009 Daniel Svaerd <daniel.svard@gmail.com> \n ~ Copyright (c) 2009 Nicolas Raoul <nicolas.raoul@gmail.com> \n ~ Copyright (c) 2010 Norbert Nagold <norbert.nagold@gmail.com> \n ~ This program is free software; you can redistribute it and/or modify it under \n ~ the terms of the GNU General Public License as published by the Free Software \n ~ Foundation; either version 3 of the License, or (at your option) any later \n ~ version. \n ~ \n ~ This program is distributed in the hope that it will be useful, but WITHOUT ANY \n ~ WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A \n ~ PARTICULAR PURPOSE. See the GNU General Public License for more details. \n ~ \n ~ You should have received a copy of the GNU General Public License along with \n ~ this program.  If not, see <http://www.gnu.org/licenses/>. \n --> \n \n"
			else:
				# some people outwitted crowdin's "0"-bug by filling in "0 ", this changes it back:
				if line.startswith("	<item>0 </item>"): 
					line = "    <item>0</item>\n"
				line = string.replace(line, '\'', '\\\'')
				line = string.replace(line, '\\\\\'', '\\\'')
				line = string.replace(line, 'amp;', '')
				if re.search('%[0-9]\\s\\$|%[0-9]\\$\\s', line) != None:
					errorOccured = True
#			print line		
			fin.write(line)
	else:
		fin.write("<?xml version=\"1.0\" encoding=\"utf-8\"?> \n <!-- \n ~ Copyright (c) 2011 Norbert Nagold <norbert.nagold@gmail.com> \n ~ This program is free software; you can redistribute it and/or modify it under \n ~ the terms of the GNU General Public License as published by the Free Software \n ~ Foundation; either version 3 of the License, or (at your option) any later \n ~ version. \n ~ \n ~ This program is distributed in the hope that it will be useful, but WITHOUT ANY \n ~ WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A \n ~ PARTICULAR PURPOSE. See the GNU General Public License for more details. \n ~ \n ~ You should have received a copy of the GNU General Public License along with \n ~ this program.  If not, see <http://www.gnu.org/licenses/>. \n --> \n \n \n<resources> \n <string-array name=\"tutorial_questions\"> \n")
		content = s.read().split("\n")
		length = len(content)
		line = []
		for i in range(length - 1):
			if isCrowdin:
				start = content[i].rfind('\",\"') + 3
			else:
				start=content[i].find('\"') + 1

			contentLine = content[i][start:len(content[i])-1]
			sepPos = contentLine.find('<separator>')
			if sepPos == -1 and len(contentLine) > 2:
				errorOccured = True
			line.append(["<![CDATA[" + contentLine[:sepPos] + "]]>", "<![CDATA[" + contentLine[sepPos+11:] + "]]>"])
		for fi in line:
			fi[0] = re.sub('\"+', '\\\"', fi[0])
			fi[0] = re.sub('\'+', '\\\'', fi[0])
			fi[0] = re.sub('\\\\{2,}', '\\\\', fi[0])
			fin.write("    <item>" + fi[0] + "</item> \n");
		fin.write(" </string-array>\n <string-array name=\"tutorial_answers\">\n");
		for fi in line:
			fi[1] = re.sub('\"+', '\\\"', fi[1])
			fi[1] = re.sub('\'+', '\\\'', fi[1])
			fi[1] = re.sub('\\\\{2,}', '\\\\', fi[1])
			fin.write("    <item>" + fi[1] + "</item> \n");
		fin.write(" </string-array>\n</resources>");
	s.close()
	fin.close()
	os.rename(newfilename, filename)
	if errorOccured:
		os.remove(filename)
		print 'error in file ' + filename
	else:
		print 'file ' + filename + ' successfully copied'

def fileExtFor(f):
	if f == 'tutorial':
		return '.csv'
	else:
		return '.xml'
def createIfNotExisting(directory):
	if not os.path.isdir(directory):
		os.mkdir(directory)
def update(valuesDirectory, f, source, fileExt, isCrowdin):
	newfile = valuesDirectory + f + '.xml'
	file(newfile, 'w').write(source)
	replacechars(newfile, fileExt, isCrowdin)

zipname = 'ankidroid.zip'

print "downloading crowdin-file"
req = urllib.urlopen('http://crowdin.net/download/project/ankidroid.zip')
file(zipname, 'w').write(req.read())
req.close()

zip = zipfile.ZipFile(zipname, "r")

for language in languages:
	if language == 'zh-TW':
		androidLanguage = 'zh-rTW'
	elif language == 'zh-CN':
		androidLanguage = 'zh-rCN'
	else:
		androidLanguage = language[:2] # Example: pt-PT becomes pt

	print "\ncopying language files for: " + androidLanguage
	valuesDirectory = "../res/values-" + androidLanguage + "/"
	createIfNotExisting(valuesDirectory)

	# Copy localization files, mask chars and append gnu/gpl licence
	for f in fileNames:
		fileExt = fileExtFor(f)
		update(valuesDirectory, f, zip.read(language + "/" + f + fileExt), fileExt, True)

# Special case: English tutorial.
valuesDirectory = "../res/values/"
createIfNotExisting(valuesDirectory)
f = 'tutorial'
fileExt = fileExtFor(f)
source = open("../assets/" + f + fileExt)
#Note: the original tutorial.csv has less columns, therefore we have special
#support for its syntax.
update(valuesDirectory, f, source.read(), fileExt, False)

print "removing crowdin-file"
os.remove(zipname)


