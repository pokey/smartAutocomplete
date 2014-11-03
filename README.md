# Smart Autocomplete

This project uses machine learning to predict what a user will type in a text
editor.

## Getting started

To setup the system for use, follow the following two steps:

1. Start the server:

        server/server.py <directory to your project>

    The server will scan all the files in the project directory.

2. Add the following line to your `.vimrc` file:

        source <absolute path of this directory>/plugin/smartautocomplete.vim

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

### Java verson

To compile:

    ./pull-dependencies   # Downloads all libraries and datasets to lib (don't need to do frequently)
    make                  # Compiles everything

To run:

    ./run @data=d3
