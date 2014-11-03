def numStr(x):
   if abs(x - int(x)) < 1e-40: # An integer (probably)
      return str(int(x))
   if abs(x) < 1e-3: # Scientific notation (close to 0)
      return '{:.2e}'.format(x)
   return '{:.3f}'.format(x)
