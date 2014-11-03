import math

class Result:
  def __init__(self, ent, acc, mrr):
    self.ent = ent
    self.acc = acc
    self.mrr = mrr

results = {}
for line in open('foo'):
  valArr = line.split('\t')
  results[(valArr[9], (valArr[13], valArr[14]))] = \
    Result(valArr[-8], valArr[-7], valArr[-6])

datasets = ['d3', 'node', 'django', 'boto', 'log4j', 'tomcat']
features = [('KneserNey$BaseLine', '0.0,0.0'),
            ('KneserNey$BaseLine', '-20,-10'),
            ('KneserNey$Mix Recency', '-20,-10'),
            ('NewKN', '-20,-10'),
            ('NewKN Recency', '-20,-10'),
            ('KneserNey$Mix NewKN', '-20,-10'),
            ('KneserNey$Mix NewKN Recency', '-20,-10')]
out = ''
sep = ''
for feature in features:
  innerSep = ''
  out += sep
  sep = '\n'
  for dataset in datasets:
    result = results[(dataset, feature)]
    out += innerSep + '{:.2f}\t{}\t{}'.format(math.exp(float(result.ent)), result.acc, result.mrr)
    innerSep = '\t'

print out
