import json, urllib

gitCmds = open("gitCmds", 'w')
cmds = open("cmds", 'w')
pullDepsCmds = open("pullDepsCmds", 'w')
runCmds = open("runCmds", 'w')

def getTopLang(lang, extension):
  print lang + ":"
  url = "https://api.github.com/search/repositories?q=language:{}&sort=stars&order=desc".format(lang)
  jsonurl = urllib.urlopen(url)
  text = json.loads(jsonurl.read())
  for item in text["items"][:10]:
    name = item["name"]
    gitCmds.write("git clone {}\n".format(item["clone_url"]))
    cmds.write(("for x in `find {} -name \"*.{}\"`; "
                "do echo $x; d=core-`dirname $x`; "
                "mkdir -p $d; cp $x $d; done\n").format(name, extension))
    pullDepsCmds.write("pull('/scr/pokey/smartAutocomplete/datasets/core-{}', 'datasets')\n".format(name))
    runCmds.write("'lib/datasets/core-{}', ".format(name))

getTopLang("java", "java")
getTopLang("python", "py")
getTopLang("javascript", "js")

gitCmds.close()
cmds.close()
pullDepsCmds.close()
runCmds.close()
