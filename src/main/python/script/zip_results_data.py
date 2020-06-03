import os
import sys
import zipfile

SKIP_DIRS = ['tmp', 'virtualNetwork', 'simobj']


def zipdir(path, ziph):
    # ziph is zipfile handle
    for root, dirs, files in os.walk(path):
        if any(skip in root for skip in SKIP_DIRS):
            continue
        for file in files:
            ziph.write(os.path.join(root, file))


if __name__ == '__main__':
    if len(sys.argv) < 2:
        print(f"Usage: {sys.argv[0]} <input dir name> <output ziped file>")
        exit(1)
    indir = sys.argv[1]
    if len(sys.argv) > 2:
        outfile = sys.argv[2]
    else:
        outfile = indir + ".zip"

    zipf = zipfile.ZipFile(outfile, 'w', zipfile.ZIP_DEFLATED)
    zipdir(indir, zipf)
    zipf.close()
    print("Wrote zipped data into - {outfile}")
