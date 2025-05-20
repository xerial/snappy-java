
centos5-image:
	docker build -f Dockerfile.centos5-linux-x86_64-pic -t xerial/centos5-linux-x86_64:latest
	# dockerhub login
	# docker login --username=xerial
	# docker push xerial/centos5-linux-x86_64:latest

multiarch-crossbuild-image:
	docker build https://github.com/kmturley/crossbuild.git -t xerial/crossbuild:latest
