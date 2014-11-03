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

### Python verson

To run:

    server/server.py <directory to your project>
