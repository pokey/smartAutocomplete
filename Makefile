default:
	mkdir -p classes
	javac -cp lib/\* -d classes `find src -name "*.java"`

check:
	src/test/wsj.sh
	src/test/triple.sh
	src/test/testTokenizerPython.sh
	src/test/testTokenizerJava.sh

clean:
	rm -r classes
