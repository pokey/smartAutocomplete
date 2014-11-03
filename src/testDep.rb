
# BerkeleyLM
system 'cd lib && ' +
       'curl -O https://berkeleylm.googlecode.com/files/berkeleylm-1.1.5.tar.gz && ' +
       'tar -xzf berkeleylm-1.1.5.tar.gz && ' +
       'cd berkeleylm-1.1.5 && ant && cd .. && ' +
       'ln -sf berkeleylm-1.1.5/jar/berkeleylm.jar berkeleylm.jar' or exit 1

