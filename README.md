# Smart Autocomplete

This project uses machine learning to predict what a user will type in a text
editor.

## Getting started

1. Pull dependencies and compile:

         ./pull-dependencies   # Downloads all libraries and datasets to lib (don't need to do frequently)
         make                  # Compiles everything

2. Add the following line to your `.vimrc` file:

        source <absolute path of this directory>/plugin/smartautocomplete.vim

3. Start the server:

        ./run -inPaths <paths>

    The server will scan all the files in the given paths, and
    enable autocompletion for files in these paths.

Now when you start vim on any file, and press Ctrl+P in insert mode, vim will
call the server for the autocomplete suggestions rather than vim's own.  What
you end up selecting will be also sent to the server as additional training
data so that autocomplete will get better over time.

## Background

Formally, the problem is to perform online learning to obtain a distribution over
possible output completion _y_ given the input context _x_.

- Input context _x_ contains
    - Names and contents of all files in the project.
    - The current file name.
    - The current position in the current file.

- Output completion _y_: a sequence of characters to be inserted at the current
  position in the current file.

## Old Python verson

To run:

    server/server.py <directory to your project>

FIXME: Talk about how to use Python version to keep track of usage
data

## Running experiments

To run a benchmark on the d3 dataset, execute the following
command:

    ./run @mode=tune @data=d3

Results of the experiment will be put into a directory called
_state_ in the current directory.  The location of the _state_
directory can be modified by adding a _@state=_ option to _./run_.

### Visualizing experiments

Once you have run an experiment, you can visualize the results by
starting a web server:

    java -cp classes:lib/* smartAutocomplete.httpServer.StandaloneHttpServer state

Then go to localhost:8040/exec/<id> in a browser to visualize the
experiment in directory <id>.exec.  This doesn't work on every
file, so if you don't see any colors when you click on a file, try
another file.  You can click on tokens to see information on how
the classifier performed, and click on feature names to see details
of how the feature made its prediction.
