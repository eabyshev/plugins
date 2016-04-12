print "If this script is not inside parent folder of your plugin, CTRL+C and move it there"
plugin = raw_input ("Enter plugin name: ")
if not plugin:
	while True:
		plugin = raw_input ("Plugin name cannot be empty, please enter again: ")
		if plugin:
			break
n = raw_input ("Enter management no: ")
if not n:
	while True:
		n = raw_input ("Management no cannot be empty, please enter again: ")
		if n:
			break
try:
	val = int (n)
except ValueError:
	while True:
		n = raw_input ("Invalid management no, please enter again: ")
		if not n:
			while True:
				n = raw_input ("Management no cannot be empty, please enter again: ")
				if n:
					break
		try:
			val = int (n)
			break
		except ValueError:
			pass
path = raw_input ("Enter path inside management: ")
skip_test = raw_input ("Skip tests? (y/n): ")
if skip_test != "y" and skip_test != "n":
	while True:
		skip_test = raw_input ("Format must be \"y\" for yes and \"n\" for no, please enter again: ")
		if skip_test == "y" or skip_test == "n":
			break
command = "mvn clean install "
if skip_test == "y":
	command += "-Dmaven.test.skip=true "
command += "&& scp " + plugin + "-api/target/" + plugin + "-plugin-api-4.0.0-RC8-SNAPSHOT.jar " + plugin + "-cli/target/" + \
		   plugin + "-plugin-cli-4.0.0-RC8-SNAPSHOT.jar " + plugin + "-impl/target/" + plugin + "-plugin-impl-4.0.0-RC8-SNAPSHOT.jar " \
		   + plugin + "-rest/target/" + plugin + "-plugin-rest-4.0.0-RC8-SNAPSHOT.jar "
command += "root@management" + n + ".critical-factor.com:/root/" + path + "\n"
script = open ("build.sh", "w+")
script.write (command)
script.close()
#args = shlex.split (command)
#print ("Please wait...")
#process = subprocess.Popen (args, stdout = subprocess.PIPE)
#output = process.communicate()[0]

print "Script successfully created. You can use it by executing \"bash build.sh\""
