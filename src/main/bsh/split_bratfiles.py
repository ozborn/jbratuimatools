import glob
import sys
import os
import errno
import shutil

cuiless_dir = "/Users/ozborn/code/repo/cuilessdata"
outdir = cuiless_dir + "/devel_updated/"
if(len(sys.argv) > 2):
        pass
else :
        print "Error: split_brat.py directory filecount username"

thedir = sys.argv[1]
thecount = int(sys.argv[2])
username = sys.argv[3]
userdir = username+"_devel_dataset"

def mkdir_p(path):
	try:
		os.makedirs(path)
	except OSError as exc: # Python >2.5
		if exc.errno == errno.EEXIST and os.path.isdir(path):
			pass
		else: raise


# Main Method
allann = glob.glob(thedir+'/*.ann')

filecount = len(allann)
print "Total Files:"+`filecount`
print "Total Datasets requested:"+`thecount`

for i in range(thecount):
	outdatasetdir = outdir+userdir+`i`
	mkdir_p(outdatasetdir)

current_set=0
for i in range(filecount):
	current_set = current_set % thecount
	outdatasetdir = outdir+userdir+`current_set`+"/"
	basefile = allann[i][allann[i].rindex("/")+1:allann[i].rindex(".")]
	shutil.copyfile(thedir+"/"+basefile+".ann",outdatasetdir+basefile+".ann")
	shutil.copyfile(thedir+"/"+basefile+".txt",outdatasetdir+basefile+".txt")
	shutil.copyfile(cuiless_dir+"/annotation.conf",outdatasetdir+"annotation.conf")
	current_set = current_set +1
