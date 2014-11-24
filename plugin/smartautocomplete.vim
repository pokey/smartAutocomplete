if !has('python')
   echo "Error: This version of vim not compiled with python"
   finish
endif

let s:defaultUri = 'http://localhost:8083'

" Event types
" FixMe: [maintainability] Find a way to sync this with the list kept in
" ../server/Tracker.py
let s:BUF_LEAVE = 3
let s:BUF_READ = 4
let s:FILE_READ = 5
let s:BUF_WRITE = 6
let s:BUF_UNLOAD = 7
let s:FILE_CHANGED = 8
let s:INSERT_ENTER = 9
let s:INSERT_LEAVE = 10

" URI to use for autocomplete is user configurable using
" g:SMARTAUTOCOMPLETE_URI
function! s:DetermineUri()
   if exists('g:SMARTAUTOCOMPLETE_URI')
      let s:uriBase = g:SMARTAUTOCOMPLETE_URI
   else
      let s:uriBase = s:defaultUri
   endif
endfunction

" When this is non-zero, result menu is reversed
let s:upsearch = 0
" The current text to be completed
let s:fragment = ""
" The cursor position of s:fragment within file
let s:start = 0
" The cursor position of s:fragment within line
let s:lineStart = 0

""
" This is a completion function called by vim's autocomplete handler.
" 
" It is called twice, the first time with findstart evaluating to true
" and the second time to false. 
" 
" During the first call, the cursor position of the beginning of the text to
" be completed is returned.  The text between the current cursor position and
" the position of the beginning of the completion is referred to as the base
" string.
" 
" For the second call, vim splices the base string out of the current buffer
" and passes it as an argument to the Complete function.  We send a query to
" the server, asking it for a list of completions.  Vim will then display the
" list of completions in a popup menu.
""
function! Complete(findstart, base)

   " First call to function
   " Returns the start position of the text to be completed
   if a:findstart
      call s:DetermineUri()
python << EOF
import vim, re

col = int(vim.eval("col('.')"))-1 #current cursor position
line = vim.eval("getline('.')")[0:col] #current line up to cursor position

# Regular expressions describing valid identifiers.  We don't actually use
# symbols, because we assume a user would never want to autocomplete a symbol,
# since there are no possible completions other than the symbol itself
# FixMe: [maintainability] Merge this with tokenizer.py in server
numbers = r'[0-9]+(?:\.[0-9]+)?'
words = r'\w+'
symbols = r'[^\s\w.]'
tokenizerRegex = re.compile('(' + '|'.join([numbers, words]) + ')$')

m = tokenizerRegex.search(line)
if m:
   start = m.start()
else:
   start = col
vim.command("let s:lineStart=%s" %start)
EOF
      return s:lineStart

   " Second call to function
   " Returns a list of possible completions
   else
      let s:fragment = a:base
python << EOF
import vim, urllib, urllib2

URI = str(vim.eval("s:uriBase")) + '/complete'

# Request list of results from server
try:
   path = str(vim.eval("expand('%:p')"))
   base = str(vim.eval("s:fragment"))
   buf = vim.current.buffer
   up = int(vim.eval("s:upsearch"))

   # Calculate location of cursor position as index into buffer
   (row, col) = vim.current.window.cursor
   loc = 0;
   for i in range(0,row-1):
      loc += len(buf[i]) + 1
   loc += col

   URI += "?path=" + urllib.quote_plus(path) + \
          "&base=" + urllib.quote_plus(base) + \
          "&loc=" + str(loc) + \
          "&up=" + str(up)

   vim.command("let s:start=%s" %loc)

   r = urllib2.Request(URI, "\n".join(buf) + "\n")
   response = urllib2.urlopen(r).read().split(chr(30))

   # If upsearch is not equal to 0, it means
   # Ctrl-P was pressed and we want to reverse the list
   if up != 0:
      response.reverse()
   vim.command("let g:results=%s" %response)

except Exception, e:
   vim.command("let g:results=[]")

EOF
      return g:results
   endif
endfunction

" This function can be called to tell the server to train on the
" directory containing the current file.
function! g:AddParentDir()
   call s:DetermineUri()

python << EOF
import vim, urllib, urllib2, os

URI = str(vim.eval("s:uriBase")) + '/addDir'

try:
   # Current path of buffer
   path = str(vim.eval("expand('%:p')"))
   parent = os.path.dirname(path)

   URI += "?dir=" + urllib.quote_plus(parent)
   r = urllib2.Request(URI)
   urllib2.urlopen(r)   

except Exception, e:
   print e

EOF
   return 0
endfunction

""
" This function is called every time a buffer is switched or is closed. When
" this happens, we submit its contents as training data, so that they can be
" used for smart autocomplete in other buffers.  
""
function! s:TrainBuffer(event, useCurrBuff)
   " Don't send if we are in a buffer not associated with a file
   " FixMe: [correctness] Is this the correct way to determine if <afile> is
   " not associated with a file?  We would like something equivelent to
   " buftype for <afile>
   if (a:useCurrBuff && (empty(expand("%")) || !empty(&buftype))) || (!a:useCurrBuff && (empty(expand("<afile>")) || !empty(getbufvar(bufnr(expand("<afile>")), "&buftype"))))
      return
   endif
   call s:DetermineUri()

python << EOF
import vim, urllib, urllib2

URI = str(vim.eval("s:uriBase")) + '/train'

try:
   # Current path of buffer
   event = str(vim.eval("a:event"))
   useCurrBuff = int(vim.eval("a:useCurrBuff"))
   if useCurrBuff:
      path = str(vim.eval("expand('%:p')"))
      buf = vim.current.buffer
   else:
      path = str(vim.eval("expand('<afile>:p')"))
      buf = vim.buffers[int(vim.eval('bufnr(expand("<afile>"))'))-1]

   URI += "?path=" + urllib.quote_plus(path) + "&event=" + event
   r = urllib2.Request(URI, "\n".join(buf) + "\n")
   urllib2.urlopen(r)   

except Exception, e:
   print e

EOF
   return 0
endfunction



""
" This function is called when a selection is made from the autocomplete menu.
" The selected result is sent to the server to be used as training data.  The
" parameter is really a hack to allow SendResult to be called from a key remap
""
function! SendResult(c)
   call s:DetermineUri()

python << EOF
import vim

line = vim.eval("getline('.')")
start = int(vim.eval("s:start"))
lineStart = int(vim.eval("s:lineStart"))
curr = int(vim.eval("col('.')"))
base = vim.eval("s:fragment")

# Selection chosen from autocomplete menu
selection = line[lineStart:curr-1]

URI = str(vim.eval("s:uriBase")) + '/accepted'

try:
   # Path of current buffer
   path = str(vim.eval("expand('%:p')"))
   buf = vim.current.buffer
   up = int(vim.eval("s:upsearch"))

   URI += "?selection=" + urllib.quote_plus(selection) + \
          "&path=" + urllib.quote_plus(path) + \
          "&base=" +urllib.quote_plus(base) + \
          "&loc=" + str(start) + \
          "&up=" + str(up)

   r = urllib2.Request(URI, "\n".join(buf) + "\n")
   urllib2.urlopen(r)   

except Exception, e:
   print e

EOF
   return a:c
endfunction



""
" This function is called when Ctrl+P or Ctrl+N are pressed in Insert mode.
" This makes it so that the autocomplete results are in reverse order for a
" Ctrl+P press.
" First, we ask server whether this buffer should use smart autocomplete or
" just normal completion
""
function! SetUpsearch(val)
  call s:DetermineUri()
  let s:upsearch = a:val
python << EOF
import vim, urllib, urllib2

URI = str(vim.eval("s:uriBase")) + '/eligible'

# Request list of results from server
try:
   path = str(vim.eval("expand('%:p')"))
   buf = vim.current.buffer
   up = int(vim.eval("s:upsearch"))

   # Calculate location of cursor position as index into buffer
   (row, col) = vim.current.window.cursor
   loc = 0;
   for i in range(0,row-1):
      loc += len(buf[i]) + 1
   loc += col

   URI += "?path=" + urllib.quote_plus(path) + \
          "&loc=" + str(loc) + \
          "&up=" + str(up)

   r = urllib2.Request(URI, "\n".join(buf) + "\n")
   response = urllib2.urlopen(r).read()

   vim.command("let g:eligible=%s" %response)

except Exception, e:
   vim.command("let g:eligible=0")

EOF
  return g:eligible ? (a:val ? "\<C-X>\<C-U>\<C-P>\<C-P>" : "\<C-X>\<C-U>") : (a:val ? "\<C-P>" : "\<C-N>")
endfunction



" Tell vim to use our autocomplete function
set completefunc=Complete

" When punctuation is pressed while an autocomplete menu is visible, treat
" that as a confirmation of the currently selected item and append that
" punctuation
let punc = ["@","%","^","&","*","(",")","-","+","=",
           \"{","[","}","]","<bar>",":",";","'","<",",",
           \">",".","/","?","<CR>","<C-y>","<Space>"]
for c in punc
   execute "inoremap <expr> ".c." pumvisible() ? SendResult(\"".c."\") : \"".c."\""   
endfor

" Remap Ctrl+P and Ctrl+N to trigger the autocomplete function
inoremap <expr> <C-N> pumvisible() ? "\<C-N>" : SetUpsearch(0)
inoremap <expr> <C-P> pumvisible() ? "\<C-P>" : SetUpsearch(1)

" Send buffer to server whenever we have a chance
" FixMe: [performance] Handle BufUnload so that we can tell server to remove
" file from memory
autocmd BufLeave * call s:TrainBuffer(s:BUF_LEAVE, 1)
autocmd VimLeave * call s:TrainBuffer(s:BUF_LEAVE, 1)
autocmd BufRead * call s:TrainBuffer(s:BUF_READ, 1)
autocmd FileReadPost * call s:TrainBuffer(s:FILE_READ, 1)
autocmd BufWritePost * call s:TrainBuffer(s:BUF_WRITE, 1)
autocmd FileChangedShellPost * call s:TrainBuffer(s:FILE_CHANGED, 0)
autocmd InsertEnter * call s:TrainBuffer(s:INSERT_ENTER, 1)
autocmd InsertLeave * call s:TrainBuffer(s:INSERT_LEAVE, 1)
