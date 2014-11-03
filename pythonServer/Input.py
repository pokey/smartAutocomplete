from collections import namedtuple

Input = namedtuple('Input', ['path', 'content', 'location', 'base', 'up'])
AnnotatedInput = namedtuple('AnnotatedInput', ['input', 'words', 'index',
                                               'lines', 'lineIndex'])
